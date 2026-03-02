# Q&A — Reporting Microservice Architecture

---

## Q1: What are the reasons to use Elasticsearch for the reporting microservice?

### 1. Eliminates expensive JOIN queries
The core pain point was a 120-second PostgreSQL query doing multi-table JOINs across `transactions`, `loan_events`, `products`, and `customers`. Elasticsearch stores **denormalized documents** — one document per transaction with all related data embedded (including nested `productDetails`). No joins needed at query time.

### 2. Native aggregation engine built for analytics
Queries like *"total interest income by product type for client X in January"* execute as a **single Elasticsearch aggregation** in ~150ms vs. 120s in PostgreSQL. The aggregation engine (sum, count, avg, date_histogram) is purpose-built for this workload.

### 3. A read replica wouldn't have solved the problem
A PostgreSQL read replica would have reduced load on the primary DB, but **the data model would remain the same** — expensive JOINs would still be required. The problem wasn't the load, it was the schema.

### 4. Decouples the read model from the transactional schema
With CQRS + Elasticsearch, the reporting service owns its own data model (ES documents). It can evolve its projections — add fields, restructure documents — **without negotiating schema changes** with the transaction team. A shared DB creates invisible coupling where a `transactions` table schema change could silently break reporting.

### 5. Fits the domain: reporting is a pure read model
The DDD analysis identified that reporting **never mutates** transactions, loans, or products — it only reads them. Elasticsearch is optimized for exactly this: high-throughput reads and aggregations over immutable or append-heavy data.

**In short:** the combination of denormalized documents + native aggregations + schema autonomy made Elasticsearch the right fit, while a read replica would have only partially addressed the problem.

---

## Q2: Is reporting data saved in ES or each time taken from the new metadata DB? What is saved in the metadata DB?

### Elasticsearch — the reporting data (read model)
All **transaction projections** are stored and persisted in ES. These are built from Kafka events and the one-time historical backfill:

- `transactionId`, `clientId`, `status`, `amount`, `currency`
- `paymentStatus`, `transactedAt`
- Nested `productDetails` (productId, productName, interestRate)
- `clientName`, `region`

When a report is requested, the data is **read from ES** (not recomputed from scratch). It's pre-materialized and kept up to date as new Kafka events arrive.

Redis sits in front of ES as a cache (TTL: 5 min) to avoid hitting ES for repeated identical queries.

### PostgreSQL 15 (Metadata DB) — configuration & operational data

| Stored | Purpose |
|---|---|
| **Report Config** | Per-client report settings, currency preferences |
| **Scheduled Reports** | Report scheduling rules (e.g. "run every Monday at 8am") |
| **Alert Rules** | Thresholds that trigger notifications |

In the request flow, Postgres is hit once to **load report config & currency settings** before querying ES — it's lightweight metadata, not the reporting data itself.

### Summary

```
Kafka events → Elasticsearch   (the actual data — transactions, projections)
                Redis           (cache layer on top of ES)
                PostgreSQL 15   (report config, schedules, alert rules only)
```

The metadata DB holds *how* to run reports; Elasticsearch holds the *data* those reports are built from.

---

## Q3: What if a customer wants not a monthly report but weekly, or for some random period, or a few days/weeks? Is the reporting microservice adapted for that?

### What Makes It Flexible

**Elasticsearch handles any date range natively.**
The index has `transactedAt` as a `date` type field. ES aggregation queries use `filter: clientId + date range` — you can pass any `from`/`to` date range, not just monthly boundaries. Weekly, 10-day, custom ranges — all work the same way at the ES level.

**Data is pre-materialized per transaction.**
Events are stored individually by `transactedAt`, not pre-aggregated into monthly buckets. So the raw granularity is already there to slice any way you want.

### What Would Need Adaptation

**1. The API parameter design**
The current API suggests a month-oriented design:
```
GET /reports/financial?month=2022-01
GET /reports/revenue?clientId=X&period=2022-01
```
Supporting arbitrary ranges would require adding parameters like `?from=2022-01-10&to=2022-01-24`.

**2. Redis cache key strategy**
Currently the cache key is `clientId + period`. Monthly periods have high cache reuse — many users request the same month. Arbitrary date ranges (e.g. "Jan 10–23") are unique per request, so **cache hit rate would drop significantly**, pushing more load to ES. A mitigation would be caching at per-day granularity and composing results, but that requires additional design.

**3. Scheduled reports (metadata DB)**
If a client wants "every Monday" or "every 2 weeks", the scheduler would need to support arbitrary cron-like intervals — doable, but requires additional implementation beyond what's described.

### Summary

| Scenario | Supported as-is? |
|---|---|
| Monthly reports | Yes, fully |
| Weekly reports (fixed Mon–Sun) | Mostly — ES supports it, API/cache need minor changes |
| Arbitrary custom date range | ES supports it, API needs redesign, cache effectiveness drops |
| Scheduled arbitrary intervals | Requires scheduler logic not described in the doc |

The **data layer (ES) is ready**. The **API and cache layers** would need intentional design work to handle arbitrary periods efficiently.
