# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Purpose

Interview prep docs + Spring Boot 3.5 / Java 21 microservice (CQRS read model, extracted from Java 11 monolith via
Strangler Fig).

## Key Documents

- **`architecture and decoupling.md`** — STAR narrative of monolith → microservice extraction. 11 Mermaid diagrams.
- **`communication and ci-cd.md`** — Kafka topology, resilience config, CI/CD pipeline.
- **`aux/`** — Checkstyle rules and supplementary docs.

## Microservice Architecture

```
Monolith (Kafka Producers)
    ↓ Events (reporting.transaction-created, etc.)
Apache Kafka → TransactionEventConsumer / LoanEventConsumer / ProductEventConsumer
    ↓
Elasticsearch 8 (denormalized TransactionProjection documents)
    ↓ ~150ms aggregation queries
Redis 7 (5-min TTL cache)
    ↓
REST API (Spring MVC + OAuth2/Keycloak)
```

**Package root:** `com.banking.reporting`

**Layers:** `api/` (controllers, DTOs, GlobalExceptionHandler RFC 7807) → `application/` (ReportQueryService,
DashboardService) → `domain/` (models, exceptions) → `infrastructure/` (kafka/, elasticsearch/, postgres/, redis/)

## Commands

```bash
mvn clean verify                                              # build + all tests + coverage
cd reporting-service && mvn test -Dtest="com.banking.reporting.unit.*"        # unit only
cd reporting-service && mvn verify -Dtest="com.banking.reporting.integration.*"  # integration (Docker required)
cd reporting-service && mvn spring-boot:run -Dspring-boot.run.profiles=local  # run (start deps first)
docker-compose up -d                                          # full local stack
```

## Claude Code Workflow

- Use **Context7 MCP** proactively for library/API docs — don't wait to be asked
- Commits: semantic message (max 80 chars), no `Co-Authored-By` trailer
- JSON URLs opened in browser: pretty-print with dark-themed syntax highlighting via `page.evaluate()`
