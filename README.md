# Banking Reporting Microservice

A production-grade Spring Boot 3.x microservice implementing a **CQRS read model** for banking financial reporting. Extracted from a Java 11 monolith using the Strangler Fig pattern, achieving a **98.3% reduction in report generation time** (120s → <2s).

## Architecture Overview

```
Monolith (Kafka Producers)
    ↓ Events (reporting.transaction-created, etc.)
Apache Kafka (3 partitions, clientId partition key)
    ↓ Consumers (TransactionEventConsumer, LoanEventConsumer, ProductEventConsumer)
Elasticsearch 8 (denormalized TransactionProjection documents)
    ↓ Aggregation queries (~150ms)
Redis 7 (5-minute TTL cache)
    ↓
REST API (Spring MVC + OAuth2/Keycloak)
    ↓
Bank Analysts / BI Tools
```

**Key Patterns:** CQRS, Event Sourcing, Strangler Fig, Saga (choreography), Circuit Breaker, Bulkhead

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.9+ |
| Docker + Compose | 24+ |
| Helm | 3.13+ |
| kubectl | 1.28+ |
| AWS CLI | 2+ (for AWS deployment) |

## Local Development

### 1. Start the full local stack

```bash
# From repository root
docker-compose up -d

# Watch logs
docker-compose logs -f reporting-service
```

Services available:
| Service | URL |
|---------|-----|
| Reporting API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Keycloak | http://localhost:8180 |
| Elasticsearch | http://localhost:9200 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |

### 2. Run locally with Maven

```bash
# Start dependencies only
docker-compose up -d postgres redis elasticsearch kafka zookeeper keycloak

# Run the application
cd reporting-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Get a Keycloak JWT

```bash
# Create test client/user in Keycloak first, then:
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/banking/protocol/openid-connect/token \
  -d "grant_type=password&client_id=reporting-client&username=analyst&password=password" \
  | jq -r '.access_token')
```

### 4. Call the API

```bash
# Financial report
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/reports/financial?clientId=cli-001&period=2022-01"

# Revenue report
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/reports/revenue?clientId=cli-001&period=2022-01"

# Dashboard
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/reports/dashboard?clientId=cli-001"
```

## Running Tests

### Unit Tests

```bash
cd reporting-service
mvn test -Dtest="com.banking.reporting.unit.*"
```

### Integration Tests (Testcontainers — requires Docker)

```bash
cd reporting-service
mvn verify -Dtest="com.banking.reporting.integration.*"
```

### All Tests + Coverage Report

```bash
mvn clean verify
# Coverage report: reporting-service/target/site/jacoco/index.html
```

### From the root (all modules)

```bash
mvn clean verify
```

## Building Docker Image

```bash
cd reporting-service

# Build only
docker build -t reporting-service:local .

# Build via Maven (includes layered JAR)
mvn package -DskipTests
docker build -t reporting-service:latest .
```

## Kubernetes Deployment

### Prerequisites

```bash
# Lint the Helm chart
helm lint reporting-service/helm/reporting-service

# Dry-run
helm install reporting-service reporting-service/helm/reporting-service \
  --dry-run --debug
```

### Deploy to Dev

```bash
helm upgrade --install reporting-service \
  reporting-service/helm/reporting-service \
  --namespace reporting \
  --create-namespace \
  -f reporting-service/helm/reporting-service/values.yaml \
  -f reporting-service/helm/reporting-service/values-dev.yaml \
  --set image.tag=<SHA>
```

### Deploy to Staging

```bash
helm upgrade --install reporting-service \
  reporting-service/helm/reporting-service \
  --namespace reporting \
  -f reporting-service/helm/reporting-service/values.yaml \
  -f reporting-service/helm/reporting-service/values-staging.yaml \
  --set image.tag=<SHA>
```

### Deploy to Production (requires manual approval in CI)

```bash
helm upgrade --install reporting-service \
  reporting-service/helm/reporting-service \
  --namespace reporting \
  -f reporting-service/helm/reporting-service/values.yaml \
  -f reporting-service/helm/reporting-service/values-prod.yaml \
  --set image.tag=<SHA> \
  --atomic  # auto-rollback on failure
```

### Verify deployment

```bash
kubectl get pods -n reporting
kubectl describe hpa reporting-service -n reporting
kubectl logs -l app=reporting-service -n reporting --tail=100
```

## AWS Deployment

Deploy CloudFormation stacks in order:

```bash
# 1. ECR Repository
aws cloudformation deploy \
  --template-file reporting-service/aws/ecr.yaml \
  --stack-name banking-ecr

# 2. EKS Cluster
aws cloudformation deploy \
  --template-file reporting-service/aws/eks-cluster.yaml \
  --stack-name banking-eks \
  --parameter-overrides VpcId=vpc-xxx SubnetIds=subnet-a,subnet-b,subnet-c \
  --capabilities CAPABILITY_IAM

# 3. RDS PostgreSQL
aws cloudformation deploy \
  --template-file reporting-service/aws/rds-postgresql.yaml \
  --stack-name banking-reporting-rds \
  --parameter-overrides VpcId=vpc-xxx SubnetIds=subnet-a,subnet-b

# 4. MSK Kafka
aws cloudformation deploy \
  --template-file reporting-service/aws/msk-kafka.yaml \
  --stack-name banking-msk \
  --parameter-overrides VpcId=vpc-xxx SubnetIds=subnet-a,subnet-b,subnet-c

# 5. ElastiCache Redis
aws cloudformation deploy \
  --template-file reporting-service/aws/elasticache-redis.yaml \
  --stack-name banking-redis \
  --parameter-overrides VpcId=vpc-xxx SubnetIds=subnet-a,subnet-b,subnet-c

# 6. Amazon OpenSearch
aws cloudformation deploy \
  --template-file reporting-service/aws/opensearch.yaml \
  --stack-name banking-opensearch \
  --parameter-overrides VpcId=vpc-xxx SubnetIds=subnet-a,subnet-b,subnet-c

# 7. IAM Roles (IRSA)
aws cloudformation deploy \
  --template-file reporting-service/aws/iam.yaml \
  --stack-name banking-reporting-iam \
  --parameter-overrides EKSClusterOIDCIssuer=oidc.eks.us-east-1.amazonaws.com/id/XXX \
  --capabilities CAPABILITY_NAMED_IAM
```

## API Reference

### Authentication

All API endpoints require a JWT Bearer token with one of:
- `ROLE_ANALYST` — read access to reports and dashboard
- `ROLE_BI_SERVICE` — read access for BI tools
- `ROLE_ADMIN` — full access including config writes

### Endpoints

| Method | Path | Role Required | Description |
|--------|------|--------------|-------------|
| GET | `/api/reports/financial` | ANALYST/BI_SERVICE | Monthly financial report |
| GET | `/api/reports/revenue` | ANALYST/BI_SERVICE | Monthly revenue report |
| GET | `/api/reports/transactions` | ANALYST/BI_SERVICE | Transaction list |
| GET | `/api/reports/dashboard` | ANALYST/BI_SERVICE | Real-time dashboard |
| POST | `/api/reports/config` | ANALYST/ADMIN | Create/update report config |
| GET | `/actuator/health` | public | Health check |
| GET | `/actuator/prometheus` | public | Prometheus metrics |
| GET | `/swagger-ui.html` | public | API documentation |

### Query Parameters

```
GET /api/reports/financial?clientId={clientId}&period={yyyy-MM}
GET /api/reports/revenue?clientId={clientId}&period={yyyy-MM}
GET /api/reports/transactions?clientId={clientId}&period={yyyy-MM}
GET /api/reports/dashboard?clientId={clientId}
```

### Example Response (Financial Report)

```json
{
  "clientId": "cli-001",
  "periodFrom": "2022-01-01T00:00:00Z",
  "periodTo": "2022-01-31T23:59:59Z",
  "totalAmount": 1250000.00,
  "totalTransactions": 847,
  "completedTransactions": 823,
  "reversedTransactions": 18,
  "chargebacks": 6,
  "trendPercentage": 12.5,
  "generatedAt": "2026-03-03T10:30:00Z",
  "cacheSource": "ELASTICSEARCH"
}
```

## Configuration Reference

Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/reporting` |
| `POSTGRES_USER` | DB username | `reporting` |
| `POSTGRES_PASSWORD` | DB password | — |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `ES_URIS` | Elasticsearch URIs | `http://localhost:9200` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `KEYCLOAK_JWKS_URI` | Keycloak JWKS endpoint | `http://localhost:8180/realms/banking/...` |

## Observability

### Grafana Dashboards (pre-built panels)

1. **Kafka Consumer Lag** — per topic/partition lag
2. **Elasticsearch Query Latency** — p50/p95/p99
3. **Redis Cache Hit Rate** — cache efficiency
4. **Circuit Breaker State** — CLOSED/OPEN/HALF_OPEN transitions
5. **API RED Metrics** — Rate, Errors, Duration per endpoint
6. **HPA Scaling Events** — pod count over time

### Key Prometheus Metrics

```
# API latency
http_server_requests_seconds_bucket{application="reporting-service"}

# Circuit breaker
resilience4j_circuitbreaker_state{name="elasticsearch"}

# Cache hit rate
cache_gets_total{name="report",result="hit|miss"}

# Kafka consumer lag
kafka_consumer_records_lag_avg
```

### Log Pattern

Structured logs include `correlationId` in every line:
```
2026-03-03 10:30:00.123 [http-nio-8080-exec-1] INFO  c.b.r.api.ReportController [abc-123-def] - GET /api/reports/financial clientId=cli-001 period=2022-01
```

## Troubleshooting

### Circuit Breaker Open

```bash
# Check state
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'

# Reset manually (dev only)
curl -X POST http://localhost:8080/actuator/circuitbreakers/elasticsearch/reset
```

### Kafka Consumer Lag

```bash
# Check consumer lag
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group reporting-service \
  --describe
```

### DLQ Events

```bash
# Consume DLQ events
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic reporting.dlq \
  --from-beginning \
  --max-messages 10
```

### Elasticsearch Index Health

```bash
curl http://localhost:9200/_cluster/health?pretty
curl http://localhost:9200/transaction_projections/_stats?pretty
```

---

## Performance Results

| Metric | Before (Monolith) | After (Microservice) |
|--------|------------------|--------------------|
| Report generation p99 | 120s | <2s |
| Primary DB CPU during reports | 95% | 55% |
| DB table locks | Frequent (3–5/day) | None |
| Deployment frequency | Monthly | Daily |

*Based on the real-world migration documented in `architecture and decoupling.md`.*
