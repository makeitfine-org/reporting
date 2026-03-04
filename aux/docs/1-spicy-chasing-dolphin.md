# Plan: Create Architecture & Decoupling Documentation

## Context

The project at /home/eug/dev/projects/my/reporting is a fresh repo (only .gitignore committed).
The user wants a detailed Markdown document (`architecture and decoupling.md`) that tells
a compelling "STAR method" story of extracting a Reporting microservice from a Java monolith —
useful for senior Java/microservices interview preparation.

## Goal

Create `architecture and decoupling.md` with:

1. Fictional-but-realistic monolith architecture (technologies, diagrams)
2. Decoupling process (step-by-step migration story)
3. Resulting standalone Reporting microservice architecture

---

## Document Structure

### 1. Monolith Architecture

**Technologies:**

- Java 11 / Spring MVC (not Spring Boot — older era)
- Maven multi-module project
- Single PostgreSQL 13 database (all modules share same schema)
- Hibernate ORM + JPA repositories
- Thymeleaf templates for frontend
- REST APIs via Spring MVC @RestController
- Tomcat WAR deployment on bare-metal VMs
- Jenkins for CI/CD (monthly releases)
- Flyway for DB migrations

**Modules inside monolith:**

- `user-module` — auth, profiles
- `order-module` — checkout, cart, payments
- `inventory-module` — product catalog, stock
- `notification-module` — emails, SMS
- `reporting-module` — financial reports, dashboards (embedded, shares DB)

**Mermaid diagrams:**

- System context diagram (monolith + single DB + users)
- Internal module dependency diagram
- Sequence diagram: report generation (massive DB joins causing table locks)

### 2. Decoupling Process

Step-by-step migration story:

1. **Identify bounded context** — DDD analysis, reporting as separate domain
2. **Strangler Fig pattern** — route /reports/* to new service while monolith still exists
3. **Add Kafka producers** to monolith on transaction events (OrderPlaced, PaymentProcessed, etc.)
4. **Stand up new Spring Boot microservice** with separate lifecycle
5. **Elasticsearch read model** — denormalized documents for fast aggregations
6. **CQRS implementation** — command side stays in monolith, query side in new service
7. **Saga pattern** — handle eventual consistency if reporting service is down
8. **Data backfill** — historical data migration from PostgreSQL to Elasticsearch
9. **Feature flag / traffic cutover** — gradual traffic migration
10. **Decommission** reporting module from monolith

**Mermaid diagrams:**

- Strangler Fig pattern diagram
- Event flow: monolith → Kafka → reporting service → Elasticsearch
- CQRS diagram showing command/query separation

### 3. Reporting Microservice Architecture

**Technologies:**

- Java 21 / Spring Boot 3.x
- Spring Kafka (consumer)
- Spring Data Elasticsearch
- Redis (caching aggregated results)
- Postgres (service-owned metadata/config — separate schema)
- Docker + Kubernetes (Helm charts)
- Prometheus + Grafana (observability)
- GitHub Actions CI/CD (daily deployments)
- OAuth2 / Keycloak for auth
- OpenAPI / Swagger for API docs

**Patterns:**

- CQRS (read model only in this service)
- Event-driven via Kafka (eventual consistency)
- Saga pattern for resilience
- Circuit Breaker (Resilience4j)

**Mermaid diagrams:**

- Full system architecture diagram
- Kafka consumer pipeline diagram
- Elasticsearch index/aggregation flow
- Sequence diagram: real-time report generation (< 2s)
- Kubernetes deployment topology

### 4. Results / Metrics

- Report time: 120s → < 2s (98% improvement)
- Primary DB CPU: reduced 40%
- Deployment frequency: monthly → daily
- Uptime: 99.99%
- Enabled: Real-time merchant dashboard (new product feature)

---

## File to Create

- `/home/eug/dev/projects/my/reporting/architecture and decoupling.md`

## Verification

- Open the file and confirm all Mermaid diagrams render (e.g., in VS Code with Mermaid plugin or GitHub preview)
- Check all sections are present and coherent
- Validate the STAR narrative flows naturally throughout
