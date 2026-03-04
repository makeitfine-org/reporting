# Plan: Reporting Microservice Full Implementation

## Context

Based on two documentation files:

- `architecture and decoupling.md` — STAR story of extracting reporting from a Java monolith
- `communication and ci-cd.md` — Inter-service comms, Kafka topology, CI/CD pipeline

The goal is to implement the complete **Reporting Microservice** described in those docs as a real, buildable Maven
project with all required code, tests, infrastructure, and deployment artifacts.

---

## Project Structure to Create

```
reporting/                          ← git root (already exists)
├── pom.xml                         ← Root Maven multi-module POM (NEW)
├── docker-compose.yml              ← Full local stack (NEW)
├── README.md                       ← Detailed usage/build/deploy guide (NEW)
├── .github/
│   └── workflows/
│       └── ci.yaml                 ← 7-stage GitHub Actions pipeline (NEW)
└── reporting-service/              ← Maven module (NEW directory)
    ├── pom.xml
    ├── Dockerfile
    ├── checkstyle.xml
    ├── src/
    │   ├── main/
    │   │   ├── java/com/banking/reporting/
    │   │   │   ├── ReportingServiceApplication.java
    │   │   │   ├── api/
    │   │   │   │   ├── ReportController.java
    │   │   │   │   ├── DashboardController.java
    │   │   │   │   └── dto/  (request/response DTOs)
    │   │   │   ├── application/
    │   │   │   │   ├── ReportQueryService.java
    │   │   │   │   └── DashboardService.java
    │   │   │   ├── domain/
    │   │   │   │   ├── model/  (Report, ReportSnapshot, etc.)
    │   │   │   │   └── exception/
    │   │   │   └── infrastructure/
    │   │   │       ├── config/   (Security, Kafka, ES, Redis, Resilience4j)
    │   │   │       ├── kafka/
    │   │   │       │   ├── consumer/  (3 consumers)
    │   │   │       │   └── event/     (5 event POJOs)
    │   │   │       ├── elasticsearch/
    │   │   │       │   ├── document/  (TransactionProjection)
    │   │   │       │   └── repository/
    │   │   │       ├── redis/
    │   │   │       └── postgres/
    │   │   │           ├── entity/    (ReportConfig, ScheduledReport, AlertRule)
    │   │   │           └── repository/
    │   │   └── resources/
    │   │       ├── application.yaml
    │   │       ├── application-local.yaml
    │   │       └── db/migration/  (Flyway V1__init.sql)
    │   └── test/
    │       ├── java/com/banking/reporting/
    │       │   ├── unit/          (service + component unit tests)
    │       │   ├── integration/   (Testcontainers full-stack)
    │       │   └── performance/   (JMH benchmarks)
    │       └── resources/
    │           └── application-test.yaml
    └── helm/
        └── reporting-service/
            ├── Chart.yaml
            ├── templates/
            │   ├── deployment.yaml
            │   ├── service.yaml
            │   ├── configmap.yaml
            │   ├── secret.yaml
            │   ├── hpa.yaml
            │   ├── pdb.yaml
            │   └── ingress.yaml
            ├── values.yaml         ← defaults
            ├── values-dev.yaml
            ├── values-staging.yaml
            └── values-prod.yaml
```

AWS deployment: `reporting-service/aws/` — CloudFormation templates for EKS, RDS, MSK, ElastiCache, OpenSearch.

---

## Implementation Steps

### Step 1 — Root `pom.xml`

Multi-module Maven parent. Manages versions via `<dependencyManagement>`. Modules: `[reporting-service]`. Includes
plugin management for: spring-boot-maven-plugin, maven-checkstyle-plugin, maven-pmd-plugin, jacoco-maven-plugin,
dependency-check-maven.

### Step 2 — `reporting-service/pom.xml`

Spring Boot 3.x parent. Dependencies:

- `spring-boot-starter-web`
- `spring-boot-starter-data-elasticsearch`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-oauth2-resource-server`
- `spring-boot-starter-actuator`
- `spring-kafka`
- `resilience4j-spring-boot3`
- `micrometer-registry-prometheus`
- `springdoc-openapi-starter-webmvc-ui`
- `postgresql`
- `flyway-core`
- `lombok`
- `jackson-databind` (null-safe config)
- Test: `spring-boot-starter-test`, `spring-kafka-test`, `testcontainers` (kafka, postgresql, elasticsearch, redis)

### Step 3 — Domain Model (null-safe with Lombok)

- `TransactionProjection` — Elasticsearch document (ES `@Document`)
- `ProductDetails` — Nested ES object
- `ReportConfig` — JPA entity (PostgreSQL)
- `ScheduledReport` — JPA entity
- `AlertRule` — JPA entity

### Step 4 — Kafka Events (5 event POJOs)

Based on §2.5 of comms doc:

- `TransactionCreatedEvent` (fields: eventType, schemaVersion, transactionId, clientId, correlationId, amount, currency,
  productId, productType, occurredAt)
- `LoanDisbursedEvent`
- `TransactionReversedEvent`
- `ProductRateUpdatedEvent`
- `ChargebackProcessedEvent`
- `DeadLetterEnvelope` (DLQ wrapper)

### Step 5 — Kafka Consumers (with DLQ + exponential backoff)

- `TransactionEventConsumer` — handles `reporting.transaction-created` + `reporting.transaction-reversed` +
  `reporting.chargeback-processed`
- `LoanEventConsumer` — handles `reporting.loan-disbursed`
- `ProductEventConsumer` — handles `reporting.product-rate-updated`
- `KafkaConsumerConfig` — `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` (1s→2s→4s backoff, 3 attempts → DLQ)

### Step 6 — Elasticsearch Layer

- `TransactionProjectionRepository` extends `ElasticsearchRepository`
- `TransactionProjectionRepositoryCustom` for aggregation queries
- ES index mapping matches the JSON mapping in §5 of architecture doc
- `ElasticsearchConfig` with `@CircuitBreaker` + `@Bulkhead` (20 concurrent, 50 queue depth)

### Step 7 — Redis Cache Layer

- `ReportCacheService` — TTL 5 min, keyed by `clientId + period`
- `RedisConfig` with `ObjectMapper` (null-safe, JavaTime module)

### Step 8 — PostgreSQL Layer (Flyway migrations)

- `V1__create_report_config.sql` — report_config, scheduled_reports, alert_rules tables
- JPA repositories for each entity

### Step 9 — Application Services

- `ReportQueryService`:
    1. Check Redis cache → hit: return cached
    2. Load report config from PostgreSQL
    3. Execute ES aggregation (wrapped in Circuit Breaker + Bulkhead)
    4. Enrich (trends, comparisons)
    5. Store in Redis (5 min TTL)
    6. Return DTO
- `DashboardService` — real-time dashboard widgets via ES

### Step 10 — REST API Layer

- `ReportController` — `GET /api/reports/financial`, `GET /api/reports/revenue`, `GET /api/reports/transactions`,
  `POST /api/reports/config`
- `DashboardController` — `GET /api/reports/dashboard`
- OpenAPI annotations via springdoc
- `CorrelationIdFilter` — extracts/generates `X-Correlation-Id`, puts in MDC, sets response header

### Step 11 — Security Config

- `SecurityConfig` — OAuth2 resource server (JWT), Keycloak JWKS URI from env var
- Role-based access: `ROLE_ANALYST`, `ROLE_BI_SERVICE`
- Internal endpoints (actuator) secured separately

### Step 12 — Error Handling (RFC 7807)

- `GlobalExceptionHandler` — `@RestControllerAdvice`
- Handles: `ServiceUnavailableException`, `ResourceNotFoundException`, `ValidationException`, `ElasticsearchException`,
  generic `Exception`
- Returns `ProblemDetail` with correlationId, timestamp, instance

### Step 13 — Resilience4j Config

As per §2.8 of comms doc:

- Circuit Breaker: elasticsearch (50% threshold, 10-call window, 30s wait)
- Retry: elasticsearch (3 attempts, 2s base, 2× multiplier)
- Bulkhead: elasticsearch (20 concurrent, 50 queue)
- TimeLimiter: ES queries 3s

### Step 14 — application.yaml

Full config with env-variable overrides for: Keycloak JWKS URI, Kafka bootstrap, ES URIs, Redis host, PostgreSQL URL.
Include Resilience4j, Micrometer/Prometheus, Flyway, logging (Loki pattern with `%X{correlationId}`).

### Step 15 — Dockerfile

Multi-stage (builder → extractor → runtime) as specified in §3.5 of comms doc. Eclipse Temurin 21 JRE, non-root `spring`
user, layered JAR.

### Step 16 — docker-compose.yml

Services:

- `reporting-service` (builds from ./reporting-service/Dockerfile)
- `kafka` (confluentinc/cp-kafka:7.5.0, 1 broker for local)
- `zookeeper` (confluentinc/cp-zookeeper:7.5.0)
- `elasticsearch` (elasticsearch:8.11.0, single node)
- `redis` (redis:7-alpine)
- `postgres` (postgres:15-alpine)
- `keycloak` (quay.io/keycloak/keycloak:22, with banking realm import)
- `prometheus` (prom/prometheus)
- `grafana` (grafana/grafana)
- Health checks on all services. Reporting service depends_on with condition: service_healthy.

### Step 17 — Tests

**Unit Tests** (no Spring context, Mockito):

- `ReportQueryServiceTest` — cache hit, cache miss, ES failure fallback
- `DashboardServiceTest`
- `TransactionEventConsumerTest`
- `LoanEventConsumerTest`
- `ProductEventConsumerTest`
- `CorrelationIdFilterTest`
- `GlobalExceptionHandlerTest`

**Integration Tests** (Testcontainers: Kafka + PostgreSQL + Elasticsearch + Redis):

- `ReportControllerIntegrationTest` — full HTTP request → Redis/ES → response, JWT auth
- `KafkaConsumerIntegrationTest` — publish event → verify ES projection created
- `DlqIntegrationTest` — ES failure → verify DLQ message after 3 retries
- `CacheIntegrationTest` — verify Redis TTL and invalidation
- `SecurityIntegrationTest` — missing JWT → 401, wrong role → 403

**Performance Tests** (JMH):

- `ElasticsearchAggregationBenchmark` — baseline aggregation query latency
- `RedisCacheBenchmark` — cache hit throughput

### Step 18 — Helm Chart

Templates with Go template syntax:

- `deployment.yaml` — image from values, envFrom ConfigMap + Secret, liveness/readiness probes (actuator), resource
  limits
- `service.yaml` — ClusterIP port 8080
- `configmap.yaml` — KAFKA_BOOTSTRAP_SERVERS, ES_URIS, KEYCLOAK_JWKS_URI, REDIS_HOST, POSTGRES env vars
- `secret.yaml` — DB_PASSWORD, KAFKA_PASSWORD (base64, from values)
- `hpa.yaml` — min 3, max 10, target CPU 70%
- `pdb.yaml` — minAvailable: 2
- `ingress.yaml` — `/api/reports/*`
- `values-dev.yaml` — replicas: 1, DEBUG logging, dev.* Kafka topics
- `values-staging.yaml` — replicas: 3, staging.* topics
- `values-prod.yaml` — replicas: 3, HPA enabled, production topics

### Step 19 — AWS Deployment

CloudFormation templates in `reporting-service/aws/`:

- `eks-cluster.yaml` — EKS cluster + managed node group
- `rds-postgresql.yaml` — RDS PostgreSQL 15 Multi-AZ
- `msk-kafka.yaml` — Amazon MSK 3-broker cluster
- `elasticache-redis.yaml` — Redis 7 cluster mode
- `opensearch.yaml` — Amazon OpenSearch (ES 8.x compatible)
- `ecr.yaml` — ECR repository for reporting-service image
- `secrets-manager.yaml` — SecretManager entries (DB creds, Kafka creds)
- `iam.yaml` — IAM roles for EKS service account (IRSA)

### Step 20 — GitHub Actions CI/CD

`.github/workflows/ci.yaml` — 7-stage pipeline:

1. Code Quality (Checkstyle, PMD, OWASP)
2. Unit & Integration Tests (JUnit 5, Testcontainers, JaCoCo 80%)
3. Build (mvn package + Docker multi-stage)
4. Security Scan (Trivy + Snyk)
5. Push to GHCR (main only)
6. Deploy (Helm: dev → staging → prod with manual approval gate)
7. Smoke Tests & Canary (k6, Prometheus metrics gate, auto-rollback)

### Step 21 — README.md

Sections: Project Overview, Architecture, Prerequisites, Local Development (docker-compose), Running Tests, Building
Docker Image, Kubernetes Deployment, AWS Deployment, API Reference, Configuration Reference, Observability,
Troubleshooting.

---

## Critical Files (Primary References)

- Architecture decisions: `architecture and decoupling.md`
- Kafka topology & resilience config: `communication and ci-cd.md` §2.4–2.9
- CI/CD pipeline YAML: `communication and ci-cd.md` §3.2–3.12
- ES index mapping: `architecture and decoupling.md` §Step 5
- Event payload schemas: `communication and ci-cd.md` §2.5

---

## Verification

1. `mvn clean verify` from root — all tests pass, JaCoCo ≥ 80%
2. `docker-compose up --build` — all services healthy
3. `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/reports/financial?clientId=cli-001&period=2022-01`
   returns JSON
4. Publish test Kafka event → verify ES projection via `GET /api/reports/transactions?clientId=cli-001`
5. Kill Elasticsearch → verify circuit breaker opens and DLQ receives events
6. `helm lint reporting-service/helm/reporting-service` — no errors
