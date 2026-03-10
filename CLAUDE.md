# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

Interview prep docs + Spring Boot 3.5 / Java 21 multi-module banking platform. The primary module is `reporting-service`
(CQRS read model, extracted from a Java 11 monolith via Strangler Fig).

## Key Documents

- **`architecture and decoupling.md`** — STAR narrative of monolith → microservice extraction. 11 Mermaid diagrams.
- **`communication and ci-cd.md`** — Kafka topology, resilience config, CI/CD pipeline.
- **`aux/`** — Checkstyle rules (`aux/checkstyle/checkstyle.xml`) and supplementary docs.
- **`GEMINI.md`** — Equivalent guidance file for Gemini CLI.

## Multi-Module Structure

```
reporting-parent (root pom.xml)
├── banking-commons       # shared library: GlobalExceptionHandler (RFC 7807), CorrelationIdFilter,
│                         #   ResourceNotFoundException / ServiceUnavailableException / ValidationException,
│                         #   DeadLetterEnvelope, InternalFeignConfig (auto-propagates X-Correlation-Id via Feign)
├── reporting-service     # primary service — see architecture below
├── customer-service      # :8081 — KYC management
├── transaction-service   # :8082 — payment processing
├── product-service       # :8083 — product catalogue
└── notification-service  # :8084 — email/SMS dispatch
```

## Reporting Service Architecture

```
transaction-service → reporting.transaction-created / reporting.transaction-reversed
                    → notification.transaction-created
customer-service    → notification.customer-updated
product-service     → reporting.product-rate-updated
                      reporting.chargeback-processed / reporting.loan-disbursed
                                   ↓ Apache Kafka
           reporting-service (TransactionEventConsumer / LoanEventConsumer / ProductEventConsumer)
                                   ↓
           Elasticsearch 8  (denormalized TransactionProjection)
                                   ↓  ~150ms aggregation queries
           Redis 7           (5-min TTL cache)
                                   ↓
           REST API          (Spring MVC + OAuth2/Keycloak)  :8080
```

**Package root:** `com.banking.reporting`

**Layers:** `api/` (controllers, DTOs, GlobalExceptionHandler RFC 7807, CorrelationIdFilter) → `application/`
(ReportQueryService, DashboardService) → `domain/` (models, exceptions) → `infrastructure/` (kafka/,
elasticsearch/, postgres/, redis/)

> `reporting-service` carries its own copies of `GlobalExceptionHandler` and domain exceptions. Other services
> use the versions from `banking-commons`.

## Inter-Service Communication (Feign)

`InternalFeignConfig` from `banking-commons` auto-propagates `X-Correlation-Id` on all Feign calls.

| Caller               | Endpoint                                          |
|----------------------|---------------------------------------------------|
| transaction-service  | `GET /internal/customers/{id}/kyc-status`         |
| transaction-service  | `GET /internal/products/{id}/rate`                |
| notification-service | `GET /internal/customers/{id}/contact`            |

## Commands

```bash
mvn clean verify                                                 # full build + unit + integration + coverage
cd reporting-service && mvn test                                 # unit tests only (*Test.java)
cd reporting-service && mvn verify                               # + integration tests (*IT.java, Docker required)
cd reporting-service && mvn test -Dtest=ReportQueryServiceTest   # single test class
cd reporting-service && mvn spring-boot:run -Dspring-boot.run.profiles=local  # run (start deps first)
docker-compose up -d                                             # full local stack
```

**Quality gates** (enforced at `validate`/`verify` phases):
- Checkstyle + PMD — fail on any violation (skip: `-Dcheckstyle.skip -Dpmd.skip`)
- JaCoCo — minimum 90% instruction coverage
- OWASP dependency-check — fails on CVSS ≥ 7 (slow; skip with `-DskipDependencyCheck`)

## Local Stack Ports

| Service              | Port |
|----------------------|------|
| reporting-service    | 8080 |
| customer-service     | 8081 |
| transaction-service  | 8082 |
| product-service      | 8083 |
| notification-service | 8084 |
| Prometheus           | 9090 |
| Grafana              | 3000 |
| Mailhog UI           | 8025 |

Dockerfiles are single-stage (Eclipse Temurin 21, copies pre-built JAR from `target/`).

## Claude Code Workflow

- Use **Context7 MCP** proactively for library/API docs — don't wait to be asked
- Commits: semantic message (max 80 chars), no `Co-Authored-By` trailer
- JSON URLs opened in browser: pretty-print with dark-themed syntax highlighting via `page.evaluate()`
