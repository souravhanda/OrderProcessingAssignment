# Design notes

## Architecture

This is a modular monolith. `api` validates and maps HTTP requests, `service` owns transactions and business rules, `domain` holds the order model and transition policy, and `repository` handles persistence. The scheduler invokes the order service rather than editing data itself.

```text
Client → OrderController → OrderService → OrderRepository → PostgreSQL
                                  ↑                 ↓
                     PendingOrderScheduler     Flyway schema
```

One process and database keep the create-order transaction simple. Microservices or a queue would add operational and consistency work without serving the assignment's requirements.

## Data and invariants

- `products` is a fixed, seeded catalog for this assignment. An order request supplies SKU and quantity; pricing comes from the server. There is no product create or update API.
- `orders` stores customer ID, status, currency, total, and UTC timestamps.
- `order_items` stores SKU, product name, unit price, quantity, and line total at purchase time. These snapshots preserve order history if the catalog changes later.
- An order must have at least one item. Quantities must be positive, SKUs must be unique within one order, all products must be active and use the same currency, and totals are calculated with `BigDecimal`.
- The order header and all items are saved in one transaction. Database constraints reinforce the application checks.

The seeded catalog makes the API runnable without adding product administration. In a larger system, catalog reads could come from a separate service, but order item snapshots would remain local to the order record.

A future catalog service could own product additions and updates. Before processing, a production workflow could recheck availability and reserve inventory. The current scheduler only checks that an order is still `PENDING`; it does not call a catalog or inventory service. Such a workflow would also need an explicit outcome for an unavailable item, instead of silently advancing the order.

## State transitions

```text
PENDING ──→ PROCESSING ──→ SHIPPED ──→ DELIVERED
   │
   └──────→ CANCELLED
```

Only `PENDING` orders can be cancelled. Manual status changes may advance exactly one step. `DELIVERED` and `CANCELLED` are terminal. Invalid transitions return HTTP 409.

The scheduler runs at five-minute wall-clock boundaries in UTC. It changes all rows still pending at that moment; this is a periodic sweep, not a promise that each order remains pending for five minutes. A deployment outage delays processing until the next run after recovery.

## Concurrency

The read used to validate a requested transition may become stale. Therefore, the write uses an atomic `UPDATE ... WHERE id = ? AND status = ?`. If another request or the scheduler wins first, the affected-row count is zero and the API returns 409. The scheduled sweep also changes rows only while they are `PENDING`. A second sweep cannot advance them again.

The bulk scheduler update keeps this small assignment simple. With a large order table, it would be replaced by bounded batches with an index on status and creation time. If several application instances run the scheduler, conditional updates preserve correctness, but all instances still perform the scan; a distributed lock or platform scheduler would remove that duplicate work.

## API and errors

The API uses order IDs as UUIDs. Creation returns 201 and a `Location` header. Reads return 200 or 404. Invalid JSON, validation failures, unknown SKUs, and invalid pagination return 400. Illegal or concurrent state changes return 409. Listing is paginated to avoid unbounded responses and returns an explicit response shape.

Springdoc generates an OpenAPI document and Swagger UI from the controller and DTO annotations. This gives reviewers an interactive way to issue requests at `/swagger-ui.html`; the feature can be disabled with `API_DOCS_ENABLED=false` when an exposed deployment does not need it.

OAuth 2.0 is not implemented. The API records the caller-supplied `customerId` but cannot prove ownership. A public deployment would need token validation and authorization checks before allowing customers to read or cancel their orders.

## Verification

Integration tests start the Spring context against H2 in PostgreSQL mode, run the Flyway migrations, and exercise creation, order details, status filtering, validation, transitions, cancellation, and the scheduler service. A unit test covers the transition policy. The tests disable the timer and invoke its service action directly, so they are deterministic and fast.

H2 differs from PostgreSQL in locking and SQL behavior. The packaged application was also started against PostgreSQL 17: Flyway migrations and schema validation completed, and create, retrieve, and cancel calls succeeded. A production deployment would additionally need authentication and operational monitoring.
