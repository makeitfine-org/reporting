# Plan: Re-domain Architecture Doc to Banking

## Context

The `architecture and decoupling.md` document currently describes an e-commerce platform serving merchants with orders, payments, and inventory. The user wants to re-skin the business domain to a **core banking system** — with bank products (loans, credit cards, accounts) and a bank reporting microservice — removing all merchant/commerce domain concepts.

The technical architecture, patterns, metrics, and code structure remain identical. Only the **business domain vocabulary** changes.

---

## Domain Mapping

| Current (E-commerce) | New (Banking) |
|---|---|
| Commerce Monolith | Core Banking Monolith |
| 800+ merchants | 50+ bank branches |
| Merchant (person) | Branch Manager / Bank Analyst (person) |
| `merchantId` | `clientId` (partition key, per-client ordering) |
| `order-module` (Checkout, cart, payments) | `transaction-module` (Payments, transfers, account ops) |
| `inventory-module` (Product catalog, stock) | `product-module` (Bank products, interest rates, terms) |
| `user-module` | `customer-module` (KYC, customer profiles) |
| Orders / `orders` table | Transactions / `transactions` table |
| Payments / `payments` table | Loan disbursements / `loan_events` table |
| Product / `order_items` | Bank product details (loan type, account type) |
| `OrderPlaced` event | `TransactionCreated` event |
| `PaymentProcessed` event | `LoanDisbursed` event |
| `OrderCancelled` event | `TransactionReversed` event |
| `InventoryUpdated` event | `ProductRateUpdated` event |
| `RefundIssued` event | `ChargebackProcessed` event |
| `OrderService.placeOrder()` | `TransactionService.processTransaction()` |
| `PaymentService.processPayment()` | `LoanService.disburseLoan()` |
| `InventoryService.updateStock()` | `ProductService.updateProductRate()` |
| `OrderProjection` / `order_projections` ES index | `TransactionProjection` / `transaction_projections` ES index |
| `MerchantMetrics` domain entity | `ClientMetrics` domain entity |
| Report query: revenue by product category for merchant X | Report query: interest income by product type for client X |
| Real-time merchant dashboard | Real-time banking dashboard |
| Merchant churn (support tickets) | Branch performance monitoring / compliance delays |
| $50K/month checkout revenue recovery | $50K/month transaction processing fee recovery |
| Checkout degradation / checkout error rate | Transaction processing degradation / transaction error rate |
| GET /reports/revenue?merchantId=X | GET /reports/revenue?clientId=X |
| % of merchants on new service rollout | % of branches on new service rollout |

---

## Changes by Section

### Section 1 — Situation: The Monolith

- Overview paragraph: replace "800+ merchants processing tens of thousands of orders daily" → "50+ bank branches managing hundreds of thousands of customer accounts and daily financial transactions"
- Module table: rename `order-module` → `transaction-module` (desc: "Payment processing, transfers, account management"), `inventory-module` → `product-module` (desc: "Bank product catalog, interest rates, terms"), `user-module` → `customer-module` (desc: "KYC, customer profiles, authentication")
- C4 diagram: Person `merchant` → `bankAnalyst` ("Branch Manager / Bank Analyst", "Reviews financial reports and dashboards"), "Commerce Monolith" → "Core Banking Monolith", external `payments` → `settlement` ("Interbank Settlement", "SWIFT / SEPA network"), update all component names to match new modules
- Module dependency diagram: update node labels to match new module names
- Note about `reporting-module`: update to reference "cross-domain joins" with bank context

### Section 2 — Task: The Breaking Point

- "120-second report generation caused browser timeouts for merchants" → "…for branch managers and compliance officers"
- "merchants were threatening to leave" → "compliance reporting SLAs were at risk"
- "A new real-time merchant dashboard" → "A new real-time banking analytics dashboard"
- "extract the reporting domain … without disrupting … merchant operations" → "… without disrupting banking operations"
- "core e-commerce flow" → "core transaction processing flow"
- "3 years of transactions" → same phrase, fine as-is (banking context is natural)

### Section 3 — Action: The Decoupling Journey

**Step 1 (DDD):**
- Domain entities: `Report`, `ReportSnapshot`, `MerchantMetrics` → `ClientMetrics`, `DashboardWidget` (same)
- Consumed entities: `Order`, `Payment`, `Product`, `User` → `Transaction`, `LoanEvent`, `BankProduct`, `Customer`

**Step 2 (Strangler Fig):**
- Diagram labels: "Merchant Browser / API Client" → "Bank Analyst / API Client"; names otherwise fine

**Step 3 (Kafka producers):**
- Events table: replace all 5 events per mapping above
- Code examples:
  - `OrderService` → `TransactionService`, `placeOrder` → `processTransaction`, `PlaceOrderRequest` → `ProcessTransactionRequest`
  - `Order order = orderRepository.save(...)` → `Transaction tx = transactionRepository.save(...)`
  - `payment = paymentService.charge(order)` → `loanService.disburseLoan(tx)`
  - `publishOrderPlaced(order, payment)` → `publishTransactionCreated(tx)`
  - `ReportingEventPublisher.publishOrderPlaced` → `publishTransactionCreated`
  - `OrderPlacedEvent` fields: `orderId` → `transactionId`, `merchantId` → `clientId`, `totalAmount` → `amount`, `items` → remove (or replace with `productType`)
  - Kafka topic: `reporting.order-placed` → `reporting.transaction-created`
  - Partition key comment: `merchantId` → `clientId`

**Step 4 (Stand up microservice):**
- Domain folder comment: `Report, MerchantMetrics` → `Report, ClientMetrics`

**Step 5 (Elasticsearch):**
- Index name: `order_projections` → `transaction_projections`
- Fields: `orderId` → `transactionId`, `merchantId` → `clientId`, `placedAt` → `transactedAt`, `items` nested → `productDetails` nested with `productId`, `productName` (loan/account type), `amount`, `interestRate`; remove `quantity`, `unitPrice`
- Aggregation description: "total revenue by product category for merchant X in January" → "total interest income by product type for client X in January"

**Step 6 (CQRS diagram):**
- `OrderSvc placeOrder()` → `TransactionSvc processTransaction()`
- `PaySvc processPayment()` → `LoanSvc disburseLoan()`
- `InvSvc updateStock()` → `ProductSvc updateProductRate()`
- Event labels: `OrderPlaced event` → `TransactionCreated event`, `PaymentProcessed event` → `LoanDisbursed event`, `InventoryUpdated event` → `ProductRateUpdated event`
- Consumer classes: `OrderEventConsumer` → `TransactionEventConsumer`, `PaymentEventConsumer` → `LoanEventConsumer`
- Kafka consumer code:
  - Class `OrderEventConsumer` → `TransactionEventConsumer`
  - `OrderProjectionRepository` → `TransactionProjectionRepository`
  - Topic `reporting.order-placed` → `reporting.transaction-created`
  - `handleOrderPlaced(OrderPlacedEvent)` → `handleTransactionCreated(TransactionCreatedEvent)`
  - `OrderProjection` fields: `orderId` → `transactionId`, `merchantId` → `clientId`, `status("PLACED")` → `status("COMPLETED")`
  - Topic `reporting.payment-processed` → `reporting.loan-disbursed`
  - `handlePaymentProcessed(PaymentProcessedEvent)` → `handleLoanDisbursed(LoanDisbursedEvent)`
  - `findById(event.getOrderId())` → `findById(event.getTransactionId())`

**Step 7 (Saga):** No domain-specific text to change.

**Step 8 (Backfill):**
- SQL query: `orders o` → `transactions t`, `merchant_id` → `client_id`, `created_at` → `transacted_at`; `payments p` → `loan_events l`, `p.order_id = o.id` → `l.transaction_id = t.id`
- Log message: "Backfilled {} records" → same, fine

**Step 9 (Feature flag / cutover):**
- Diagram: "Check flag for merchantId" → "Check flag for clientId"
- Rollout table: "% of Merchants on New Service" → "% of Branches / Clients on New Service"; "Internal test merchants" → "Internal test branches", "Small merchants" → "Low-volume branches", "Mid-tier merchants" → "Mid-tier branches"
- "No merchant-reported issues" → "No branch-reported issues"

**Step 10 (Decommission):**
- "merchant operations" not present here — no change needed

### Section 4 — Result: The Reporting Microservice

- Full system architecture diagram:
  - "Merchant Browser" → "Bank Analyst Browser"
  - `ReportQueryService` / `DashboardService` → same names fine
  - `OrderEventConsumer` → `TransactionEventConsumer`, `PaymentEventConsumer` → `LoanEventConsumer`
  - Monolith label: "Core Business Logic Orders · Payments · Inventory" → "Core Business Logic Transactions · Loans · Products"
  - Kafka topic labels: `reporting.order-placed` → `reporting.transaction-created`, `reporting.payment-processed` → `reporting.loan-disbursed`, `reporting.inventory-updated` → `reporting.product-rate-updated`

- Kafka consumer pipeline diagram:
  - Topics: `reporting.order-placed` → `reporting.transaction-created`, `reporting.payment-processed` → `reporting.loan-disbursed`, `reporting.order-cancelled` → `reporting.transaction-reversed`, `reporting.inventory-updated` → `reporting.product-rate-updated`, `reporting.refund-issued` → `reporting.chargeback-processed`
  - Consumer classes: `OrderEventConsumer` → `TransactionEventConsumer`, `PaymentEventConsumer` → `LoanEventConsumer`, `InventoryEventConsumer` → `ProductEventConsumer`
  - "Upsert by orderId" → "Upsert by transactionId"
  - Topics keyed: "keyed by merchantId" → "keyed by clientId"

- Elasticsearch aggregation flow diagram:
  - `GET /reports/revenue?merchantId=X&period=2024-01` → `GET /reports/revenue?clientId=X&period=2024-01`
  - `filter: merchantId + date range` → `filter: clientId + date range`

- Real-time report sequence:
  - Actor label: `Merchant` → `BankAnalyst`
  - `GET /reports/financial?month=2024-01` → same endpoint fine
  - `getFinancialReport(merchantId, period)` → `getFinancialReport(clientId, period)`
  - Cache key: `merchantId + period` → `clientId + period`
  - Note: "Total p99 latency: < 2 seconds vs. 120 seconds in monolith" — unchanged

### Section 5 — Outcomes & Metrics

- "Checkout error rate during reports" → "Transaction processing error rate during reports"
- Business Impact table:
  - "Real-time merchant dashboard" → "Real-time banking analytics dashboard"
  - "Merchant churn reduction" → "Compliance SLA improvement" (12% reduction in support tickets about report timeouts)
  - "Revenue protection / Checkout degradation" → "Transaction fee recovery / Transaction processing degradation eliminated"
  - "Reporting team can now ship independently without coordinating monthly monolith releases" — unchanged

---

## Critical File

- `/home/eug/dev/projects/my/reporting/architecture and decoupling.md`

---

## Verification

After edits:
1. Search for remaining "merchant", "order" (lowercase non-technical), "checkout", "inventory", "commerce" occurrences and verify they're all replaced
2. Verify all Mermaid diagrams still use valid syntax (no structural changes, only label text)
3. Verify all Java code examples compile logically (method names, variable names consistent)
4. Verify all Kafka topic names are consistent across all sections
5. Verify the ES index mapping field names match the Java code projection builder
