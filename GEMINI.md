# Banking Reporting Microservice

## Project Overview

This is a **production-grade Spring Boot 3.x microservice** responsible for banking financial reporting. It implements a **CQRS (Command Query Responsibility Segregation) read model** pattern, consuming events from a legacy monolith via Kafka to build high-performance query projections in Elasticsearch.

**Key Achievements:**
*   Reduced report generation time from 120s to <2s.
*   Decoupled reporting load from the primary transactional database.
*   Implements the "Strangler Fig" migration pattern.

## Tech Stack

*   **Language:** Java 21
*   **Framework:** Spring Boot 3.x
*   **Build Tool:** Maven 3.9+
*   **Databases:**
    *   **PostgreSQL 15:** Configuration, schedules, and alert rules.
    *   **Elasticsearch 8:** Denormalized transaction projections for fast aggregation.
    *   **Redis 7:** Caching for frequent report queries.
*   **Messaging:** Apache Kafka (Confluent Platform 7.5)
*   **Infrastructure:** Docker Compose (local), Kubernetes/Helm (prod), AWS (EKS, MSK, RDS, OpenSearch).
*   **Security:** OAuth2 / Keycloak (JWT).
*   **Observability:** Prometheus, Grafana, Micrometer, Resilience4j.

## Architecture

The system follows a layered architecture with a strong separation of concerns:

*   **API Layer (`api`):** REST controllers, DTOs, Exception handling.
*   **Application Layer (`application`):** Service logic, orchestrating domain and infrastructure.
*   **Domain Layer (`domain`):** Core business logic, models, and exceptions.
*   **Infrastructure Layer (`infrastructure`):** Implementations for persistence (Postgres, ES), messaging (Kafka), and caching (Redis).

**Data Flow:**
1.  **Ingestion:** Consumes `TransactionCreated`, `LoanDisbursed`, etc., from Kafka.
2.  **Projection:** Updates denormalized `TransactionProjection` documents in Elasticsearch.
3.  **Query:** API clients request reports; data is fetched from Redis (cache hit) or aggregated from Elasticsearch (cache miss).

## Key Directories

*   `reporting-service/`: Main microservice module.
    *   `src/main/java/`: Source code.
    *   `src/test/java/`: Unit, Integration, and Performance tests.
    *   `src/main/resources/`: Configuration (`application.yaml`), Flyway migrations, ES settings.
*   `helm/`: Kubernetes Helm charts for deployment.
*   `aws/`: CloudFormation templates for AWS infrastructure.
*   `docs/`: Documentation and architectural decisions.

## Build & Run

### Local Development (Docker Compose)

The easiest way to run the full stack (Service + DBs + Kafka + Keycloak):

```bash
# Start infrastructure and service
docker-compose up -d

# View logs
docker-compose logs -f reporting-service
```

### Manual Run (Maven)

If you prefer running the Java app outside Docker (requires supporting services running via Docker Compose):

```bash
# Start dependencies only
docker-compose up -d postgres redis elasticsearch kafka zookeeper keycloak

# Run the application
cd reporting-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Testing

*   **Unit Tests:** Fast, isolated tests.
    ```bash
    mvn test -Dtest="com.banking.reporting.unit.*"
    ```
*   **Integration Tests:** Uses Testcontainers (requires Docker).
    ```bash
    mvn verify -Dtest="com.banking.reporting.integration.*"
    ```
*   **Full Suite:**
    ```bash
    mvn clean verify
    ```

## Development Conventions

*   **Code Style:** Follows standard Java/Spring conventions. Checkstyle is configured.
*   **Logging:** Structured logging is enforced. All logs include a `correlationId` (MDC).
*   **Testing:**
    *   Prefer **Unit Tests** for business logic.
    *   Use **Integration Tests** for database, Kafka, and external service interactions.
    *   Do not mock `static` methods unless absolutely necessary.
*   **Git:** Use semantic commit messages (e.g., `feat: add revenue report endpoint`, `fix: correct kafka consumer group id`).
*   **Documentation:** Update `README.md` and `architecture and decoupling.md` if architectural patterns change.

## Common Tasks

*   **Database Migrations:** SQL files in `src/main/resources/db/migration` (Flyway).
*   **Elasticsearch Settings:** Index settings in `src/main/resources/elasticsearch`.
*   **Kafka Consumers:** Located in `infrastructure/kafka/consumer`.
*   **API Endpoints:** Defined in `api/*Controller.java`.

## Troubleshooting

*   **Circuit Breaker:** If Elasticsearch is down, the circuit breaker will open. Check `/actuator/health`.
*   **Kafka Lag:** Use `kafka-consumer-groups.sh` to check lag if reports are delayed.
*   **Auth:** Requires a valid JWT from Keycloak. See `README.md` for generating a test token.
