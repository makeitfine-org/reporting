# Plan: communication and ci/cd.md

## Context

The user has an interview-prep document describing the extraction of a Reporting microservice from a Java banking
monolith. They want a new companion document that imagines the *full* decoupling — all 5 modules as independent
microservices — and explains in detail:

1. How all microservices communicate with each other and with external systems
2. The complete CI/CD process for the platform

The document must follow the same style conventions as `architecture and decoupling.md` (Mermaid diagrams, table
formats, code blocks, callout notes, color palette, STAR narrative tone).

---

## Five Microservices (fully decoupled)

| Service              | Responsibility                           | Key Data Store(s)                  |
|----------------------|------------------------------------------|------------------------------------|
| Customer Service     | KYC, profiles, authentication            | PostgreSQL (own schema)            |
| Transaction Service  | Payments, transfers, loans               | PostgreSQL (own schema)            |
| Product Service      | Catalog, interest rates, terms           | PostgreSQL (own schema)            |
| Notification Service | Email / SMS dispatch                     | PostgreSQL (job log)               |
| Reporting Service    | Financial reports, dashboards, analytics | Elasticsearch + Redis + PostgreSQL |

---

## Document Structure: `communication and ci/cd.md`

### Part 1 — Inter-Service Communication

**1. Platform Overview**

- Opening paragraph contextualizing the fully-decoupled state
- Full tech stack summary table
- Services summary table (responsibility, stores, consumers)
- **Diagram 1**: C4Context — full platform (5 services + Kong + Kafka + all external systems)

**2.1. API Gateway (Kong/Nginx)**

- Routing table (`/api/customers/*` → Customer Service, etc.)
- Kong responsibilities: JWT validation, rate limiting, SSL termination, `X-Correlation-Id` injection
- Kong ↔ Keycloak JWKS integration (cached public key, no per-request roundtrip)
- Kong declarative config YAML snippet
- **Diagram 2**: `graph LR` — API Gateway topology (external clients → Kong → services)

**2.2. Synchronous REST (Service-to-Service)**

- Internal calls: Transaction→Customer (KYC check), Transaction→Product (rate lookup), Notification→Customer (contact
  resolution)
- Internal DNS: `http://customer-service.banking.svc.cluster.local:8080`
- Spring `@FeignClient` code example with `X-Correlation-Id` header propagation
- `application.yaml` snippet showing service URL config
- **Diagram 3**: `sequenceDiagram` — Transaction booking flow (Kong→Transaction→Customer→Product→PostgreSQL→Kafka)

**2.3. gRPC Consideration**

- Why evaluated (Transaction→Product is high-frequency)
- Why REST retained: operational simplicity, <15ms internal latency within tolerance
- Future consideration note: revisit at 500+ TPS

**2.4. Asynchronous Kafka Event Bus**

- Why Kafka: temporal decoupling, event replay, fan-out, audit log
- Kafka cluster specs: 3 brokers, 3 partitions/topic, `clientId` partition key, 7-day retention, RF=3
- Full producer/consumer table (all topics across all services)
- `notification.*` topics introduced alongside existing `reporting.*` topics
- Idempotency: all consumers use upsert semantics keyed by `transactionId`
- **Diagram 4**: `graph TB` — full Kafka event flow (5 producer services → all topics → Reporting + Notification
  consumers + DLQ)

**2.5. Kafka Topics by Domain**

- Per-topic subsection: name, partition key, producers, consumers, JSON payload schema
- Topics: `reporting.transaction-created`, `reporting.loan-disbursed`, `reporting.transaction-reversed`,
  `reporting.product-rate-updated`, `reporting.chargeback-processed`, `reporting.dlq`,
  `notification.transaction-created`, `notification.customer-updated`
- DLQ envelope format: original event + `failureReason`, `failedAt`, `attemptCount`, `originalTopic`

**2.6. External Service Integrations**

- Keycloak: token flow, JWKS caching, service accounts, `spring-boot-starter-oauth2-resource-server`
- SMTP/SMS Gateway: Notification Service as sole producer; JavaMailSender + Twilio-compatible REST; Resilience4j Rate
  Limiter (100 SMS/min) + Retry
- SWIFT/SEPA: Transaction Service exclusively; mTLS mutual auth; ISO 20022 XML; Circuit Breaker + retry queue
- BI Tools: service-account JWT (`ROLE_BI_SERVICE`), Kong rate-limited (100 req/min), no direct ES access
- Mobile App: same Kong JWT flow; push notifications via FCM/APNs from Notification Service
- **Diagram 5**: `graph TB` — External integration map (K8s cluster boundary + all outside systems with protocol labels)

**2.7. Service Discovery (Kubernetes-Native)**

- No Eureka/Consul, no Istio — K8s DNS handles it
- Internal DNS reference table for all 5 services
- ConfigMap-injected service URLs in `application.yaml`
- Istio: documented future path if mTLS compliance becomes required

**2.8. Resilience Patterns**

- Pattern summary table: Circuit Breaker, Retry, Rate Limiter, Bulkhead, Timeout, DLQ — per scenario with library and
  config
- Java code example: `@CircuitBreaker` + `@Retry` on Feign client call with fallback to cache
- `application.yaml` Resilience4j config block
- **Diagram 6**: `stateDiagram-v2` — Circuit Breaker state machine (CLOSED→OPEN→HALF_OPEN + Prometheus/Grafana alert)

**2.9. Error Handling & DLQ Strategy**

- REST: RFC 7807 `application/problem+json` via `@ControllerAdvice`
- Kafka: 3-attempt exponential backoff → `DeadLetterPublishingRecoverer` → `*.dlq` topic
- DLQ monitoring: Prometheus `kafka_consumer_dlq_total`, Grafana alert, PagerDuty
- Correlation ID propagation across HTTP (`X-Correlation-Id` header) and Kafka (`correlationId` field in event envelope)

---

### Part 2 — CI/CD Pipeline

**3.1. Repository & Branch Strategy**

- Polyrepo: 7 repositories (`banking/customer-service`, `transaction-service`, `product-service`,
  `notification-service`, `reporting-service`, `banking-commons`, `infra-helm-charts`)
- Trunk-Based Development with short-lived feature branches (max 2 days)
- Branch naming: `feature/<JIRA-ID>-description`, `hotfix/<JIRA-ID>`
- `main` protection rules: CI gates + 1 reviewer approval; no direct pushes
- `detect-secrets` pre-commit hook across all repos

**3.2. Pipeline Architecture Overview**

- Fail-fast philosophy; dev deploy < 10 min, prod < 20 min
- 7-stage summary table (tool, duration, gate type)
- **Diagram 7**: `graph LR` — CI/CD pipeline flow (7 stages with failure paths and auto-rollback branch on stage 7)

**3.3. Stage 1 — Code Quality**

- Checkstyle (Google Java Style), PMD (`pmd-ruleset.xml`), OWASP Dependency Check (CVSS ≥ 7.0 = fail)
- Full GitHub Actions YAML
- `pom.xml` plugin stanzas

**3.4. Stage 2 — Unit & Integration Tests**

- JUnit 5 + Testcontainers per service (per-service container table: Kafka, PostgreSQL, ES, Redis, WireMock, Keycloak)
- `@DynamicPropertySource` base class code example
- JaCoCo 80% line coverage gate
- Full GitHub Actions YAML with test report artifact upload

**3.5. Stage 3 — Build (Maven + Docker Multi-Stage)**

- `mvn package -DskipTests` → Docker multi-stage `Dockerfile` (full content)
- Eclipse Temurin 21 JRE, non-root `spring` user
- Image tags: `ghcr.io/banking/<service>:<git-sha>` + `:latest`
- GitHub Actions YAML with dynamic SHA tag

**3.6. Stage 4 — Security Scanning**

- Trivy (container image, OS + app deps) + Snyk (Maven source scan)
- Policy: CRITICAL/HIGH with fix = fail; HIGH no fix = warning + backlog
- SARIF output → GitHub Advanced Security tab
- Full GitHub Actions YAML

**3.7. Stage 5 — Push to GHCR**

- Only on `main` merges (`if: github.ref == 'refs/heads/main'`)
- `GITHUB_TOKEN` auth, `packages: write` permission
- Both SHA and `latest` tags pushed
- Full GitHub Actions YAML

**3.8. Stage 6 — Deploy (Helm Upgrade)**

- Centralized Helm charts in `infra-helm-charts` repo
- Per-environment values files (`values-dev.yaml`, `values-staging.yaml`, `values-prod.yaml`)
- `helm upgrade --install --atomic --timeout 5m` with `--set image.tag=$SHA`
- Per-environment differences table (replicas, log level, resource requests)
- Staging: automatic after dev; prod: `environment: production` protection rule with required reviewer approval
- **Diagram 8**: `graph TB` — deployment topology (GitHub Actions → GHCR → 3 K8s namespaces via Helm)

**3.9. Stage 7 — Smoke Tests & Canary Analysis**

- Per-service smoke test table (health check + functional call + success criteria)
- Canary: Prometheus error rate (<1%) + p99 latency (<20% regression) over 3-min window post-deploy
- k6 load test snippet (10 VU, 60s) as staging canary
- Auto-rollback step via `helm rollback` if threshold breached

**3.10. Environment Strategy**

- dev (`banking-dev`), staging (`banking-staging`), prod (`banking-prod`)
- Staging = production-equivalent (same replicas, separate `staging.*` Kafka topics)
- Prod window: 00:00–06:00 UTC preferred
- Same Docker image SHA promoted through all environments — no rebuild

**3.11. Rollback Strategy**

- Primary: `helm rollback <release> <revision>` (10-revision history retained)
- Automatic triggers: `--atomic` failure, canary threshold breach, PagerDuty alert within 10 min
- Feature flags: Redis-backed, admin API managed, runtime disable without redeployment
- Rollback runbook table (4 scenarios: CrashLoop, error spike, logic bug, feature flag)

**3.12. Secrets Management**

- Secret types table: DB creds, Kafka SASL, Keycloak client secrets, SMTP, SMS, SWIFT mTLS cert, GHCR pull secret, CI
  tokens — all in Kubernetes Secrets (encrypted at rest via KMS)
- Helm values secret reference YAML (never plain values, always `secretKeyRef`)
- Vault: documented future path (Agent Injector pattern, no app code changes)
- `detect-secrets` CI step in code quality stage

**4. Appendix — Key Design Decisions**
Five prose decisions (matching existing doc style):

1. Polyrepo vs Monorepo
2. Trunk-Based Development vs GitFlow
3. No Istio/Service Mesh
4. `--atomic` Helm vs Blue/Green
5. Kubernetes Secrets vs Vault (for now)

---

## Diagrams Summary (8 Mermaid diagrams)

| # | Type                                    | Section              |
|---|-----------------------------------------|----------------------|
| 1 | `C4Context` — Full platform             | 1. Platform Overview |
| 2 | `graph LR` — API Gateway topology       | 2.1 API Gateway      |
| 3 | `sequenceDiagram` — Transaction booking | 2.2 REST             |
| 4 | `graph TB` — Kafka event flow           | 2.4 Kafka            |
| 5 | `graph TB` — External integrations      | 2.6 External         |
| 6 | `stateDiagram-v2` — Circuit Breaker     | 2.8 Resilience       |
| 7 | `graph LR` — CI/CD pipeline (7 stages)  | 3.2 Pipeline         |
| 8 | `graph TB` — Deployment topology        | 3.8 Deploy           |

---

## Critical Files

- `/home/eug/dev/projects/my/reporting/architecture and decoupling.md` — Style reference (Mermaid colors, table formats,
  code blocks, narrative tone)
- `/home/eug/dev/projects/my/reporting/CLAUDE.md` — Project context

## Writing Conventions (from existing doc)

- Inline backticks for class names, topic names, config keys, file paths, HTTP paths
- `> **Note:**` blockquotes after diagrams
- All code blocks have language tags
- Diagram fill colors: green=new `#51cf66`, yellow=legacy `#ffd43b`, red=error `#ff6b6b`, teal=data stores `#4ecdc4`,
  orange=Kafka `#ff922b`, purple=observability `#845ef7`
- Tables always have `|---|` separator rows

## Verification

After writing the document:

1. Open in VS Code with "Markdown Preview Mermaid Support" extension — verify all 8 diagrams render without errors
2. Check all internal section anchor links in the Table of Contents resolve correctly
3. Confirm naming consistency with `architecture and decoupling.md` (same service names, Kafka topic names, tech
   versions)
4. Review the Appendix decisions are consistent with the architecture choices made in the existing document
