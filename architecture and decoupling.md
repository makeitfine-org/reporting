# Reporting Microservice: Architecture & Decoupling

> **Interview Preparation — STAR Method Story**
> Situation → Task → Action → Result

---

## Table of Contents

1. [Situation: The Monolith](#1-situation-the-monolith)
2. [Task: The Breaking Point](#2-task-the-breaking-point)
3. [Action: The Decoupling Journey](#3-action-the-decoupling-journey)
4. [Result: The Reporting Microservice](#4-result-the-reporting-microservice)
5. [Outcomes & Metrics](#5-outcomes--metrics)

---

## 1. Situation: The Monolith

### Overview

The platform was a Java 11 / Spring MVC monolith that had grown organically over six years. Originally built for a handful of internal users, it now served 50+ bank branches managing hundreds of thousands of customer daily financial transactions. The codebase lived in a single Maven multi-module project deployed as a WAR to bare-metal VMs running Tomcat, with a single PostgreSQL 13 database shared across all business domains.

### Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 11 |
| Framework | Spring MVC 5.x (not Spring Boot) |
| Build | Maven multi-module (monorepo WAR) |
| Database | PostgreSQL 13 — single shared schema |
| ORM | Hibernate 5 + JPA repositories |
| Frontend | Thymeleaf templates |
| APIs | Spring MVC `@RestController` |
| Deployment | Tomcat WAR on bare-metal VMs |
| CI/CD | Jenkins — monthly release cadence |
| Migrations | Flyway |

### Internal Module Structure

The monolith was organized into five Maven modules, all sharing the same Spring application context and the same PostgreSQL schema:

| Module | Responsibility |
|---|---|
| `customer-module` | KYC, customer profiles, authentication |
| `transaction-module` | Payment processing, transfers, account management |
| `product-module` | Bank product catalog, interest rates, terms |
| `notification-module` | Email and SMS dispatch |
| `reporting-module` | Financial reports, banking dashboards, analytics |

### System Context Diagram

```mermaid
C4Context
    title System Context — Monolith Era

    Person(bankAnalyst, "Branch Manager / Bank Analyst", "Reviews financial reports and dashboards")
    Person(ops, "Ops Team", "Manages deployments, monitors system health")

    System_Boundary(monolith, "Core Banking Monolith (WAR on Tomcat)") {
        System(customer, "customer-module", "KYC & Customer Profiles")
        System(transaction, "transaction-module", "Payments & Transfers")
        System(product, "product-module", "Bank Products & Rates")
        System(notification, "notification-module", "Email / SMS")
        System(reporting, "reporting-module", "Reports & Dashboards")
    }

    SystemDb(postgres, "PostgreSQL 13", "Single shared database — all modules share schema")
    System_Ext(smtp, "SMTP / SMS Gateway", "External notification delivery")
    System_Ext(settlement, "Interbank Settlement", "SWIFT / SEPA network")

    Rel(bankAnalyst, reporting, "Uses", "HTTPS")
    Rel(ops, customer, "Deploys & Monitors", "SSH / Jenkins")
    Rel(reporting, postgres, "Reads & Writes", "JDBC / Hibernate")
    Rel(notification, smtp, "Sends notifications", "SMTP / REST")
    Rel(transaction, settlement, "Processes transactions", "REST / HTTPS")
```

### Module Dependency Diagram

```mermaid
graph TD
    subgraph Monolith["Core Banking Monolith (Maven WAR)"]
        UM[customer-module<br/>KYC · Profiles]
        OM[transaction-module<br/>Payments · Transfers · Account Ops]
        IM[product-module<br/>Bank Products · Rates · Terms]
        NM[notification-module<br/>Email · SMS]
        RM[reporting-module<br/>Reports · Dashboards]
    end

    DB[(PostgreSQL 13<br/>Shared Schema)]

    OM -->|depends on| UM
    OM -->|depends on| IM
    NM -->|depends on| OM
    RM -->|direct DB joins across| OM
    RM -->|direct DB joins across| IM
    RM -->|direct DB joins across| UM

    UM --> DB
    OM --> DB
    IM --> DB
    NM --> DB
    RM --> DB

    style RM fill:#ff6b6b,color:#fff
    style DB fill:#4ecdc4,color:#fff
```

> **Note:** The `reporting-module` had no service layer abstraction — it issued raw multi-table JOIN queries directly against the shared schema, spanning cross-domain joins across banking transaction, product, and customer data. This is where the pain began.

### The Pain: Report Generation Sequence

```mermaid
sequenceDiagram
    actor BankAnalyst
    participant API as Spring MVC Controller<br/>(reporting-module)
    participant Service as ReportService
    participant DB as PostgreSQL 13<br/>(Shared Schema)

    BankAnalyst->>API: GET /reports/financial?month=2022-01
    API->>Service: generateMonthlyFinancialReport(month)

    Service->>DB: BEGIN TRANSACTION
    Service->>DB: SELECT t.*, l.*, pr.*, c.*<br/>FROM transactions t<br/>JOIN loan_events l ON ...<br/>JOIN transaction_items ti ON ...<br/>JOIN products pr ON ...<br/>JOIN customers c ON ...<br/>WHERE t.transacted_at BETWEEN ... (120s query)

    Note over DB: TABLE LOCK acquired<br/>on transactions, loan_events,<br/>products tables

    DB-->>Service: 500k+ rows returned
    Service->>Service: Aggregate in Java heap<br/>(group-by, sum, pivot)
    Service->>DB: COMMIT

    Note over DB: Locks released — other<br/>modules unblocked

    Service-->>API: ReportDTO
    API-->>BankAnalyst: JSON response (after ~120 seconds)

    Note over BankAnalyst,DB: During report generation:<br/>transaction processing is degraded,<br/>product rate updates are slow,<br/>DB CPU spikes to 95%
```

---

## 2. Task: The Breaking Point

### Why This Became Urgent

By Q3 2021, the situation had deteriorated to the point of business risk:

- **120-second report generation** caused browser timeouts for branch managers and compliance officers — support tickets were piling up
- **Monthly releases** meant bug fixes took weeks to ship; compliance reporting SLAs were at risk
- **Table locks** during report queries caused transaction processing failures (revenue impact)
- **Primary DB CPU spiked to 95%** during business hours when multiple branches ran reports simultaneously
- **A new real-time banking analytics dashboard** was on the product roadmap — impossible with the current architecture

The mandate from engineering leadership: **extract the reporting domain into a standalone microservice without disrupting the existing monolith or banking operations**.

### Constraints

- Zero downtime migration (the platform ran 24/7)
- No monolith rewrite — the core transaction processing flow must remain stable
- Historical data (3 years of transactions) must be available in the new service
- Team of 4 backend engineers, 12-week timeline

---

## 3. Action: The Decoupling Journey

### Step 1 — Identify the Bounded Context (DDD Analysis)

The first step was a Domain-Driven Design workshop with the team and product owners. We mapped the reporting domain's ubiquitous language, entities, and aggregate boundaries:

- **Reporting domain owns:** `Report`, `ReportSnapshot`, `ClientMetrics`, `DashboardWidget`
- **Reporting domain consumes (read-only):** `Transaction`, `LoanEvent`, `BankProduct`, `Customer` — but only as *read projections*, not authoritative state
- **Key insight:** Reporting is a pure *read model* domain. It never mutates orders or payments. This made it an ideal CQRS candidate.

The reporting module had no legitimate reason to share a database with the transactional modules. Every cross-domain join was a symptom of missing domain boundaries.

### Step 2 — Strangler Fig Pattern: Route Traffic Gradually

Before writing a single line of new service code, we set up the routing infrastructure. An Nginx reverse proxy was placed in front of Tomcat:

```mermaid
graph LR
    Merchant["Bank Analyst / API Client"]
    Nginx["Nginx Reverse Proxy"]
    Monolith["Monolith (Tomcat WAR)"]
    NewService["Reporting Microservice (Spring Boot)"]

    Merchant -->|HTTPS| Nginx
    Nginx -->|"/reports/* → NEW"| NewService
    Nginx -->|"Everything else → MONOLITH"| Monolith

    style NewService fill:#51cf66,color:#fff
    style Monolith fill:#ffd43b,color:#000
```

**Nginx location config (simplified):**

```nginx
upstream monolith {
    server 10.0.1.10:8080;
}

upstream reporting_service {
    server 10.0.2.20:8080;
}

location /reports/ {
    proxy_pass http://reporting_service;
}

location / {
    proxy_pass http://monolith;
}
```

At this stage, the new service returned 404 for everything — but the routing layer was in place. We could flip traffic with a config reload, no deployment needed.

### Step 3 — Add Kafka Producers to the Monolith

We instrumented the monolith's service layer with Kafka producers at key transaction boundaries. This was the most politically sensitive step — touching the monolith — so changes were minimal and additive only.

**Events published by monolith:**

| Event | Published By | Payload |
|---|---|---|
| `TransactionCreated` | `TransactionService.processTransaction()` | transactionId, clientId, productType, amount, timestamp |
| `LoanDisbursed` | `LoanService.disburseLoan()` | transactionId, loanId, amount, currency, status |
| `TransactionReversed` | `TransactionService.cancelTransaction()` | transactionId, clientId, reason, timestamp |
| `ProductRateUpdated` | `ProductService.updateProductRate()` | productId, clientId, newRate, previousRate |
| `ChargebackProcessed` | `LoanService.processChargeback()` | loanId, transactionId, amount, timestamp |

**Example Kafka producer added to monolith's `TransactionService`:**

```java
// Existing monolith code — minimal addition
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    // NEW: injected Kafka producer
    @Autowired
    private ReportingEventPublisher reportingEventPublisher;

    @Transactional
    public Transaction processTransaction(ProcessTransactionRequest request) {
        Transaction tx = transactionRepository.save(buildTransaction(request));
        loanService.disburseLoan(tx);

        // Existing logic unchanged above
        // NEW: publish event after successful commit
        reportingEventPublisher.publishTransactionCreated(tx);

        return tx;
    }
}
```

```java
@Component
public class ReportingEventPublisher {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransactionCreated(Transaction tx) {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
            .transactionId(tx.getId())
            .clientId(tx.getClientId())
            .amount(tx.getAmount())
            .currency(tx.getCurrency())
            .productType(tx.getProductType())
            .occurredAt(Instant.now())
            .build();

        kafkaTemplate.send("reporting.transaction-created", tx.getClientId(), event);
    }
}
```

Kafka topics used `clientId` as the partition key — ensuring per-client ordering guarantees while allowing parallelism across clients.

### Step 4 — Stand Up the Reporting Microservice

A new Spring Boot 3.x project was created with its own lifecycle, independent of the monolith:

```
reporting-service/
├── src/main/java/com/company/reporting/
│   ├── api/                    # REST controllers (OpenAPI)
│   ├── application/            # Use cases / application services
│   ├── domain/                 # Domain model (Report, ClientMetrics)
│   ├── infrastructure/
│   │   ├── kafka/              # Kafka consumers
│   │   ├── elasticsearch/      # ES repositories & queries
│   │   ├── redis/              # Cache layer
│   │   └── postgres/           # Metadata & config persistence
│   └── ReportingServiceApplication.java
├── helm/                       # Kubernetes Helm chart
├── Dockerfile
└── pom.xml
```

**Key dependencies in `pom.xml`:**

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
</dependencies>
```

### Step 5 — Elasticsearch Read Model

Instead of running joins against PostgreSQL, the new service materializes denormalized documents in Elasticsearch — one document per transaction enriched with all the data reporting needs:

**Elasticsearch index mapping (`transaction_projections`):**

```json
{
  "mappings": {
    "properties": {
      "transactionId":  { "type": "keyword" },
      "clientId":       { "type": "keyword" },
      "status":         { "type": "keyword" },
      "amount":         { "type": "double" },
      "currency":       { "type": "keyword" },
      "paymentStatus":  { "type": "keyword" },
      "transactedAt":   { "type": "date" },
      "productDetails": {
        "type": "nested",
        "properties": {
          "productId":    { "type": "keyword" },
          "productName":  { "type": "keyword" },
          "amount":       { "type": "double" },
          "interestRate": { "type": "double" }
        }
      },
      "clientName":     { "type": "keyword" },
      "region":         { "type": "keyword" }
    }
  }
}
```

Aggregations like "total interest income by product type for client X in January" that previously required a 120s JOIN now execute as a single Elasticsearch aggregation query in under 200ms.

### Step 6 — CQRS Implementation

The architecture cleanly separates commands from queries:

```mermaid
graph TB
    subgraph CommandSide["Command Side (Monolith — unchanged)"]
        OrderSvc["TransactionService<br/>processTransaction()"]
        PaySvc["LoanService<br/>disburseLoan()"]
        InvSvc["ProductService<br/>updateProductRate()"]
        MonolithDB[(PostgreSQL 13<br/>Transactional Data)]

        OrderSvc --> MonolithDB
        PaySvc --> MonolithDB
        InvSvc --> MonolithDB
    end

    Kafka{{"Apache Kafka<br/>reporting.* topics"}}

    subgraph QuerySide["Query Side (Reporting Microservice — new)"]
        Consumer["Kafka Consumers<br/>EventProjectionHandler"]
        Projector["Projection Builder<br/>TransactionProjection"]
        ES[(Elasticsearch<br/>Read Model)]
        Redis[(Redis<br/>Aggregation Cache)]
        API["REST API<br/>/reports/*"]

        Consumer --> Projector
        Projector --> ES
        ES --> Redis
        Redis --> API
    end

    OrderSvc -->|"TransactionCreated event"| Kafka
    PaySvc -->|"LoanDisbursed event"| Kafka
    InvSvc -->|"ProductRateUpdated event"| Kafka
    Kafka --> Consumer

    style CommandSide fill:#ffd43b,color:#000
    style QuerySide fill:#51cf66,color:#fff
    style Kafka fill:#ff922b,color:#fff
```

**Kafka consumer in the reporting service:**

```java
@Component
public class TransactionEventConsumer {

    @Autowired
    private TransactionProjectionRepository projectionRepository;

    @KafkaListener(
        topics = "reporting.transaction-created",
        groupId = "reporting-service",
        containerFactory = "reportingKafkaListenerFactory"
    )
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        TransactionProjection projection = TransactionProjection.builder()
            .transactionId(event.getTransactionId())
            .clientId(event.getClientId())
            .amount(event.getAmount())
            .currency(event.getCurrency())
            .status("COMPLETED")
            .transactedAt(event.getOccurredAt())
            .productDetails(mapProductDetails(event.getProductType()))
            .build();

        projectionRepository.save(projection); // saves to Elasticsearch
    }

    @KafkaListener(
        topics = "reporting.loan-disbursed",
        groupId = "reporting-service"
    )
    public void handleLoanDisbursed(LoanDisbursedEvent event) {
        projectionRepository.findById(event.getTransactionId())
            .ifPresent(projection -> {
                projection.setPaymentStatus(event.getStatus());
                projection.setPaymentId(event.getLoanId());
                projectionRepository.save(projection);
            });
    }
}
```

### Step 7 — Saga Pattern for Resilience

Because the reporting service is eventually consistent, we needed to handle scenarios where the service was temporarily unavailable and events were buffered in Kafka. We implemented a simple choreography-based saga:

- Kafka consumer group offsets are committed **only after successful Elasticsearch write**
- If Elasticsearch is unavailable, the consumer pauses and retries with exponential backoff
- Dead-letter topic (`reporting.dlq`) captures events that fail after 3 retries — an alert fires and the team investigates
- A nightly reconciliation job compares Kafka offset lag vs. expected projections and alerts on discrepancies

```mermaid
sequenceDiagram
    participant Kafka
    participant Consumer as Kafka Consumer<br/>(Reporting Service)
    participant ES as Elasticsearch
    participant DLQ as Dead Letter Queue<br/>(reporting.dlq)
    participant Alert as PagerDuty

    Kafka->>Consumer: TransactionCreated event

    Consumer->>ES: Save projection
    alt ES available
        ES-->>Consumer: 201 Created
        Consumer->>Kafka: Commit offset
    else ES unavailable (attempt 1-3)
        ES-->>Consumer: Connection timeout
        Consumer->>Consumer: Exponential backoff retry
    else ES unavailable (attempt 4+)
        Consumer->>DLQ: Send to dead-letter topic
        DLQ->>Alert: Trigger PagerDuty alert
    end
```

### Step 8 — Historical Data Backfill

Three years of transaction history lived in PostgreSQL. We wrote a one-time backfill job that ran alongside the live system:

```java
@Component
public class HistoricalDataBackfillJob {

    @Autowired
    private JdbcTemplate monolithJdbcTemplate; // read-only connection to monolith DB

    @Autowired
    private TransactionProjectionRepository projectionRepository;

    // Runs once, in batches, idempotent (upsert by transactionId)
    @Scheduled(fixedDelay = Long.MAX_VALUE)
    public void backfill() {
        long offset = 0;
        int batchSize = 1000;
        List<TransactionProjection> batch;

        do {
            batch = monolithJdbcTemplate.query(
                """
                SELECT t.id, t.client_id, t.amount, t.currency,
                       t.transacted_at, l.status as payment_status, l.id as loan_id
                FROM transactions t
                LEFT JOIN loan_events l ON l.transaction_id = t.id
                ORDER BY t.transacted_at
                LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> mapToProjection(rs),
                batchSize, offset
            );

            projectionRepository.saveAll(batch); // bulk upsert to Elasticsearch
            offset += batchSize;

            log.info("Backfilled {} records (total offset: {})", batch.size(), offset);
        } while (!batch.isEmpty());
    }
}
```

The backfill completed in ~6 hours running at off-peak hours, with no impact to the live system (read-only queries against a read replica).

### Step 9 — Feature Flag / Traffic Cutover

We used a simple feature flag (backed by Redis) to gradually shift report traffic from the monolith to the new service, independently per client:

```mermaid
graph LR
    Nginx["Nginx / API Gateway"]
    FlagService["Feature Flag Service<br/>(Redis-backed)"]
    Monolith["Monolith /reports/*<br/>(legacy)"]
    NewService["Reporting Microservice<br/>(new)"]

    Nginx -->|"Check flag for clientId"| FlagService
    FlagService -->|"Flag = OFF (90% branches)"| Monolith
    FlagService -->|"Flag = ON (10% → 50% → 100%)"| NewService

    style NewService fill:#51cf66,color:#fff
    style Monolith fill:#adb5bd,color:#fff
```

**Rollout timeline:**

| Week | % of Branches / Clients on New Service | Notes |
|---|---|---|
| Week 1 | 5% | Internal test branches only |
| Week 2 | 20% | Low-volume branches |
| Week 3 | 50% | Mid-tier branches |
| Week 4 | 100% | Full cutover |

Each phase included: SLO monitoring, latency comparison, error rate tracking. No branch-reported issues during any phase.

### Step 10 — Decommission Reporting from Monolith

With 100% of traffic on the new service and metrics stable for two weeks, we removed the `reporting-module` from the monolith:

1. Deleted `reporting-module` Maven module and all its source code
2. Removed cross-module JPA repository injections from `reporting-module`
3. Ran `flyway` migration to drop reporting-specific tables from the shared schema (moved to Postgres metadata DB owned by new service)
4. Removed Nginx location block for monolith's `/reports/*` routing
5. Final monolith WAR shrank by ~18% in size; startup time improved by ~12s

---

## 4. Result: The Reporting Microservice

### Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Messaging | Apache Kafka (Spring Kafka) |
| Read Store | Elasticsearch 8.x (Spring Data Elasticsearch) |
| Cache | Redis 7 (Spring Data Redis) |
| Metadata DB | PostgreSQL 15 (service-owned schema) |
| Auth | OAuth2 / Keycloak (JWT bearer tokens) |
| Resilience | Resilience4j (Circuit Breaker, Retry, Rate Limiter) |
| Containerization | Docker + Kubernetes (Helm charts) |
| Observability | Prometheus + Grafana + Loki |
| CI/CD | GitHub Actions (daily deployments) |
| API Docs | OpenAPI 3 / Springdoc Swagger UI |

### Full System Architecture

```mermaid
graph TB
    subgraph Clients["Clients"]
        Browser["Bank Analyst Browser"]
        MobileApp["Mobile App"]
        InternalBI["Internal BI Tools"]
    end

    subgraph Gateway["API Gateway / Keycloak"]
        GW["API Gateway<br/>(Kong / Nginx)"]
        KC["Keycloak<br/>OAuth2 / JWT"]
    end

    subgraph ReportingService["Reporting Microservice (Kubernetes)"]
        API["REST API Layer<br/>Spring MVC @RestController<br/>OpenAPI / Swagger"]
        AppSvc["Application Services<br/>ReportQueryService<br/>DashboardService"]
        CircuitBreaker["Resilience4j<br/>Circuit Breaker"]
        KafkaConsumers["Kafka Consumers<br/>TransactionEventConsumer<br/>LoanEventConsumer"]
        Projector["Projection Builder<br/>Denormalized read models"]
    end

    subgraph DataStores["Data Stores"]
        ES[(Elasticsearch 8<br/>Order Projections<br/>Aggregation Index)]
        Redis[(Redis 7<br/>Aggregation Cache<br/>TTL: 5 min)]
        PG[(PostgreSQL 15<br/>Report Config<br/>Scheduled Reports<br/>Alert Rules)]
    end

    subgraph Monolith["Core Banking Monolith (Tomcat)"]
        MonolithCore["Core Business Logic<br/>Transactions · Loans · Products"]
        MonolithDB[(PostgreSQL 13<br/>Transactional Data)]
        KafkaProducers["Kafka Producers<br/>(additive, non-breaking)"]
    end

    Kafka{{"Apache Kafka<br/>reporting.transaction-created<br/>reporting.loan-disbursed<br/>reporting.product-rate-updated<br/>reporting.dlq"}}

    Observability["Prometheus + Grafana<br/>Loki (logs)"]

    Browser --> GW
    MobileApp --> GW
    InternalBI --> GW
    GW --> KC
    KC --> API
    API --> AppSvc
    AppSvc --> CircuitBreaker
    CircuitBreaker --> ES
    CircuitBreaker --> Redis
    AppSvc --> PG

    MonolithCore --> MonolithDB
    MonolithCore --> KafkaProducers
    KafkaProducers --> Kafka
    Kafka --> KafkaConsumers
    KafkaConsumers --> Projector
    Projector --> ES

    ReportingService --> Observability

    style ReportingService fill:#51cf66,color:#fff
    style Kafka fill:#ff922b,color:#fff
    style Monolith fill:#ffd43b,color:#000
```

### Kafka Consumer Pipeline

```mermaid
graph LR
    subgraph Topics["Kafka Topics (3 partitions each, keyed by clientId)"]
        T1["reporting.transaction-created"]
        T2["reporting.loan-disbursed"]
        T3["reporting.transaction-reversed"]
        T4["reporting.product-rate-updated"]
        T5["reporting.chargeback-processed"]
        DLQ["reporting.dlq"]
    end

    subgraph ConsumerGroup["Consumer Group: reporting-service"]
        C1["TransactionEventConsumer<br/>@KafkaListener"]
        C2["LoanEventConsumer<br/>@KafkaListener"]
        C3["ProductEventConsumer<br/>@KafkaListener"]
    end

    Validator["Schema Validator<br/>(Avro / JSON Schema)"]
    Projector["Projection Merger<br/>Upsert by transactionId"]
    ES[(Elasticsearch)]
    ErrorHandler["DeadLetterPublisher<br/>(after 3 retries)"]

    T1 --> C1
    T2 --> C2
    T3 --> C1
    T4 --> C3
    T5 --> C2

    C1 --> Validator
    C2 --> Validator
    C3 --> Validator

    Validator -->|Valid| Projector
    Validator -->|Invalid schema| ErrorHandler
    Projector --> ES
    Projector -->|Failure after retries| ErrorHandler
    ErrorHandler --> DLQ

    style ES fill:#4ecdc4,color:#fff
    style DLQ fill:#ff6b6b,color:#fff
```

### Elasticsearch Aggregation Flow

```mermaid
graph TB
    Query["API Request:<br/>GET /reports/revenue?clientId=X&period=2022-01"]

    Cache{"Redis Cache<br/>Hit?"}

    CacheHit["Return cached result<br/>(TTL: 5 minutes)"]

    ESQuery["Elasticsearch Aggregation Query<br/>index: transaction_projections<br/>filter: clientId + date range<br/>agg: sum(amount), count, avg"]

    ESResult["Aggregation Result<br/>(~150ms)"]

    Transform["Transform & Enrich<br/>Currency conversion<br/>Trend calculation"]

    CacheStore["Store in Redis<br/>(TTL: 5 min)"]

    Response["JSON Response<br/>to Client"]

    Query --> Cache
    Cache -->|HIT| CacheHit
    Cache -->|MISS| ESQuery
    ESQuery --> ESResult
    ESResult --> Transform
    Transform --> CacheStore
    CacheStore --> Response
    CacheHit --> Response

    style Cache fill:#ff922b,color:#fff
    style ESQuery fill:#4ecdc4,color:#fff
```

### Real-Time Report Generation Sequence

```mermaid
sequenceDiagram
    actor BankAnalyst
    participant GW as API Gateway<br/>(Keycloak JWT)
    participant API as REST Controller
    participant Cache as Redis Cache
    participant AppSvc as ReportQueryService
    participant CB as Circuit Breaker<br/>(Resilience4j)
    participant ES as Elasticsearch
    participant PG as PostgreSQL<br/>(Metadata)

    BankAnalyst->>GW: GET /reports/financial?month=2022-01
    GW->>GW: Validate JWT (Keycloak)
    GW->>API: Authenticated request + clientId claim

    API->>Cache: GET cache key (clientId + period)
    alt Cache HIT
        Cache-->>API: Cached aggregation result
        API-->>BankAnalyst: 200 JSON (~10ms)
    else Cache MISS
        API->>AppSvc: getFinancialReport(clientId, period)
        AppSvc->>PG: Load report config & currency settings
        PG-->>AppSvc: Config

        AppSvc->>CB: Execute ES query (circuit breaker wraps)
        CB->>ES: Aggregation query<br/>(filter + date_histogram + sum)
        ES-->>CB: Aggregation result (~150ms)
        CB-->>AppSvc: Result (circuit CLOSED)

        AppSvc->>AppSvc: Enrich with trends & comparisons
        AppSvc->>Cache: SET result (TTL: 5 min)
        AppSvc-->>API: FinancialReportDTO
        API-->>BankAnalyst: 200 JSON (~200ms total)
    end

    Note over BankAnalyst,ES: Total p99 latency: < 2 seconds<br/>vs. 120 seconds in monolith
```

### Kubernetes Deployment Topology

```mermaid
graph TB
    subgraph k8s["Kubernetes Cluster"]
        subgraph ns["Namespace: reporting"]
            subgraph deploy["Deployment: reporting-service"]
                P1["Pod 1<br/>reporting-service:v2.4.1"]
                P2["Pod 2<br/>reporting-service:v2.4.1"]
                P3["Pod 3<br/>reporting-service:v2.4.1"]
            end

            HPA["HorizontalPodAutoscaler<br/>min: 3, max: 10<br/>target CPU: 70%"]
            SVC["Service (ClusterIP)<br/>port: 8080"]
            Ingress["Ingress<br/>/reports/*"]
            CM["ConfigMap<br/>application.yaml"]
            Secret["Secret<br/>DB creds, Kafka creds"]
            PDB["PodDisruptionBudget<br/>minAvailable: 2"]
        end

        subgraph infra["Namespace: infra"]
            ES_STS["StatefulSet: elasticsearch<br/>3 nodes (data + master)"]
            Redis_STS["StatefulSet: redis<br/>1 primary + 2 replicas"]
            Kafka_STS["StatefulSet: kafka<br/>3 brokers + 3 zookeeper"]
        end

        subgraph monitoring["Namespace: monitoring"]
            Prom["Prometheus"]
            Grafana["Grafana"]
            Loki["Loki"]
        end
    end

    Internet["Ingress Controller<br/>(AWS ALB)"]

    Internet --> Ingress
    Ingress --> SVC
    SVC --> P1
    SVC --> P2
    SVC --> P3
    HPA -.->|scales| deploy
    P1 & P2 & P3 --> ES_STS
    P1 & P2 & P3 --> Redis_STS
    P1 & P2 & P3 -.->|consumes| Kafka_STS
    P1 & P2 & P3 -.->|metrics| Prom
    Prom --> Grafana

    style deploy fill:#51cf66,color:#fff
    style infra fill:#4ecdc4,color:#fff
    style monitoring fill:#845ef7,color:#fff
```

**Helm values (excerpt):**

```yaml
replicaCount: 3

image:
  repository: ghcr.io/company/reporting-service
  tag: "v2.4.1"
  pullPolicy: IfNotPresent

resources:
  requests:
    cpu: "500m"
    memory: "512Mi"
  limits:
    cpu: "2000m"
    memory: "1Gi"

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20

podDisruptionBudget:
  minAvailable: 2
```

---

## 5. Outcomes & Metrics

### Performance

| Metric | Before (Monolith) | After (Microservice) | Improvement |
|---|---|---|---|
| Report generation time (p99) | 120 seconds | < 2 seconds | **98.3% faster** |
| Primary DB CPU during reports | 95% | 55% | **42% reduction** |
| DB table locks during reports | Frequent (3–5/day) | None | **Eliminated** |
| Transaction processing error rate during reports | 0.8% | 0% | **Eliminated** |

### Operations

| Metric | Before | After | Improvement |
|---|---|---|---|
| Deployment frequency | Monthly | Daily | **30x more frequent** |
| Deploy lead time (code → prod) | 3–4 weeks | < 1 day | **~20x faster** |
| Service uptime (SLO) | 99.5% | 99.99% | **50x fewer outages** |
| Mean time to recovery (MTTR) | ~4 hours | ~8 minutes | **30x faster recovery** |

### Business Impact

| Outcome | Details |
|---|---|
| Real-time banking analytics dashboard | New product feature — impossible before; launched 6 weeks after service went live |
| Compliance SLA improvement | 12% reduction in support tickets about report timeouts |
| Transaction fee recovery | Transaction processing degradation eliminated — estimated $50K/month in recovered transaction fees |
| Engineering velocity | Reporting team can now ship independently without coordinating monthly monolith releases |

### Grafana Dashboard (Key Metrics Tracked)

- Kafka consumer lag per topic/partition
- Elasticsearch query latency (p50, p95, p99)
- Redis cache hit rate
- Circuit breaker state (CLOSED / OPEN / HALF_OPEN)
- Report API request rate, error rate, latency (RED metrics)
- Kubernetes pod autoscaling events

---

## Appendix: Key Design Decisions

### Why Elasticsearch over PostgreSQL read replica?

A read replica would have reduced load on the primary but kept the same data model — still requiring expensive JOINs. Elasticsearch's denormalized document model and native aggregation engine were the right fit for analytics workloads. A single document per transaction (with nested product details) eliminates joins entirely.

### Why Kafka over direct API calls from monolith?

Direct API calls would couple the monolith's transaction path to the reporting service's availability. Kafka decouples them temporally: the monolith's `processTransaction()` call completes immediately regardless of whether the reporting service is up. The event is buffered and processed when the consumer is ready.

### Why CQRS over a shared database?

A shared database creates invisible coupling — any schema change to `transactions` could break the reporting module without a compilation error. CQRS gives the reporting service full autonomy over its data model (Elasticsearch documents), allowing it to evolve its read projections without negotiating schema changes with the transaction team.

### Why the Strangler Fig pattern over a Big Bang rewrite?

The Strangler Fig allowed us to migrate incrementally with zero downtime and instant rollback capability (just flip the Nginx upstream back). A big bang cutover would have required a risky all-or-nothing deployment and made debugging much harder.

---

*This document reflects a real-world architectural journey and is intended as interview preparation material demonstrating senior-level system design, microservices patterns, and production engineering judgment.*
