# Order Processing System

Java 21 / Spring Boot API for creating, retrieving, listing, advancing, and cancelling orders. Pending orders move to `PROCESSING` at each five-minute scheduler run.

## Run

Requires Java 21 and Docker Desktop (or another running Docker daemon). The Maven wrapper is included.

1. Start Docker Desktop and wait until its daemon is ready.
2. From the project root, start PostgreSQL:

```bash
./scripts/start-db.sh
```

The script starts the database container and waits for it to be ready on port `15432`.

3. Start the Spring Boot application in a terminal:

```bash
./mvnw spring-boot:run
```

4. Once the application has started, open [Swagger UI](http://localhost:8080/swagger-ui.html). Expand `POST /orders`, choose **Try it out**, enter the sample request below, and choose **Execute**. Use the returned order ID with the get, list, status, and cancel endpoints. The OpenAPI JSON is at [`/v3/api-docs`](http://localhost:8080/v3/api-docs).

The database uses PostgreSQL on local port `15432`; `DB_URL`, `DB_USER`, and `DB_PASSWORD` can override the defaults.

Seeded products: `BOOK-001` (USD 12.50), `PEN-001` (USD 2.25), and `BAG-001` (USD 24.00). For example, create an order with:

```json
{"customerId":"customer-123","items":[{"sku":"BOOK-001","quantity":1},{"sku":"PEN-001","quantity":2}]}
```

| Operation | Endpoint |
| --- | --- |
| Create | `POST /orders` |
| Get details | `GET /orders/{id}` |
| List/filter | `GET /orders?status=PENDING&page=0&size=20` |
| Advance status | `PATCH /orders/{id}/status` |
| Cancel pending order | `POST /orders/{id}/cancel` |

Valid transitions: `PENDING → PROCESSING → SHIPPED → DELIVERED`, or `PENDING → CANCELLED`. An invalid transition returns `409 Conflict`.

## Assumptions and scope

- The product catalog is fixed and seeded for this assignment. The backend validates SKUs and takes prices from that catalog when an order is **created**; order items keep price and name snapshots.
- The scheduler advances all still-pending orders every five minutes. It does **not** recheck product availability or reserve inventory before processing.
- A future catalog service could add and update products. A production processing flow could check availability and reserve stock before moving an order to `PROCESSING`, with a defined failure state if that check fails.
- OAuth 2.0 authentication and customer ownership checks are **not implemented**. `customerId` is supplied by the caller and is not verified.

More design and trade-offs are in [DESIGN.md](docs/DESIGN.md).

## Tests

Run `./mvnw test`. Seven tests cover the API, status rules, scheduler service, and OpenAPI endpoints using H2; startup and core requests were also verified against PostgreSQL 17.
