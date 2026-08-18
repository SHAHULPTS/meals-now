# Meals Now

A multi-vendor food delivery backend built as a hands-on, production-grade learning project. Three roles — **Customer**, **Vendor**, **Admin** — share one system whose spine is the **order lifecycle** and (later) **real-time status updates**.

Built with Java 21 and Spring Boot 4, as a **modular monolith**: one deployable application, cleanly split into bounded modules (`identity`, `vendor`, `catalog`, `order`, `notification`) so the boundaries can be learned — and one module extracted into its own service later — without paying the operational cost of microservices up front.

> **Status:** work in progress. Phases 0–4 are complete; Phase 5 (Kafka + real-time) is in progress (step 2 done). See the [Roadmap](#roadmap) for exactly what works today.

---

## Table of contents

- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Domain model](#domain-model)
- [The order state machine](#the-order-state-machine)
- [Getting started](#getting-started)
- [API overview](#api-overview)
- [Project structure](#project-structure)
- [Design decisions](#design-decisions)
- [Roadmap](#roadmap)
- [License](#license)

---

## Tech stack

| Area | Choice |
| --- | --- |
| Language / runtime | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) |
| Security | Spring Security 7 + JWT (jjwt 0.12), BCrypt password hashing |
| Database | PostgreSQL 18 |
| Migrations | Flyway |
| API docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Maven (wrapper included) |
| Containers | Docker / Docker Compose |
| Messaging | Apache Kafka (real-time event streaming, order status updates) |
| Planned | WebSocket (real-time), React frontend, GCP Cloud Run + Cloud SQL |

---

## Architecture

Every request flows through the same layered path — understanding this layering is most of understanding the app:

```
HTTP request
  → Security filter   (valid JWT? which role?)
  → Controller        (HTTP concerns: parse request, status codes, response)
  → Service           (business logic, transactions, ownership rules)
  → Repository        (data access via Spring Data JPA)
  → PostgreSQL
```

Guiding principles:

- **DTOs never expose entities.** Controllers speak in request/response records; JPA entities stay behind the service layer. This decouples the public API from the database schema.
- **The service layer owns transactions and rules.** `@Transactional` boundaries, ownership checks ("a vendor edits only their own menu"), and state-transition guards all live here — not in controllers.
- **Repositories are interfaces.** Spring Data generates the queries; derived queries (`findByStatus`, `findByVendorIdAndAvailableTrue`) cover most needs.
- **Consistent errors.** A single `@RestControllerAdvice` maps exceptions to a stable JSON error shape with correct HTTP status codes.
- **Stateless auth.** JWTs carry the user id and role; no server-side session, which suits horizontally-scaled / serverless deployment.

---

## Domain model

| Entity | Notes |
| --- | --- |
| `User` | id, email, BCrypt password hash, role (`CUSTOMER` / `VENDOR` / `ADMIN`) |
| `Vendor` | owned by a `User`, name, status (`PENDING` / `APPROVED` / `SUSPENDED`) |
| `MenuItem` | belongs to a vendor; name, price, availability, category |
| `Order` | customer, vendor, status, total, items |
| `OrderItem` | belongs to an order; **price-at-time snapshot** (item name + unit price) + quantity |

All entities extend a `BaseEntity` with a UUID id and `created`/`updated` audit timestamps. Money is modelled with `BigDecimal` (never floating point). Status fields are enums persisted as strings.

**Price snapshotting.** `OrderItem` stores the item's name and unit price *as they were when the order was placed*. If a vendor later changes a menu price, past orders are unaffected — the snapshot is the source of truth for order history, not the live `MenuItem`.

---

## The order state machine

The heart of the system. Order status is an enum, and legal transitions are declared **once, as data** (an `EnumMap` of allowed successors) rather than scattered `if` checks. Every status change goes through a single guarded method; illegal jumps are rejected with a `409 Conflict`.

```
PLACED ──► ACCEPTED ──► PREPARING ──► READY ──► OUT_FOR_DELIVERY ──► DELIVERED
  │            │             │
  ├► CANCELLED ┘             │  (cancellable up to PREPARING)
  └► REJECTED                │
```

`DELIVERED`, `CANCELLED`, and `REJECTED` are terminal — they map to an empty successor set, so "terminal" needs no special-casing in code.

Payment is a mock behind a `PaymentService` interface (`charge(amount) → PaymentResult`). It runs as a gate *before* the order is persisted: on success the order is saved as `PLACED`; on failure the transactional method rolls back and no order is written. Swapping in a real gateway later means writing one new implementation of the interface — the order logic doesn't change.

---

## Getting started

### Prerequisites

- Java 21 (JDK)
- Docker + Docker Compose

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This runs PostgreSQL 18 on `localhost:5433` (database `mealsnow`, user `mealsnow`). Data persists in a named Docker volume.

### 2. Configure secrets

`application.yml` expects a JWT signing secret under `app.jwt.secret`. Provide a long random value (32+ bytes, base64) via an environment variable or a local profile — do not commit real secrets.

### 3. Run the app

```bash
./mvnw spring-boot:run
```

On startup, Flyway applies the migrations in `src/main/resources/db/migration`, and (under the `dev` profile) a seed runner inserts sample users, an approved vendor, and menu items.

The app boots on `http://localhost:8080`.

### 4. Explore

- Health: `GET /actuator/health`
- Swagger UI: `/swagger-ui.html` — use the **Authorize** button to paste a bearer token from `/auth/login`.

### Running tests

```bash
./mvnw test
```

Repository integration tests spin up a throwaway PostgreSQL via Testcontainers, so Docker must be running.

---

## API overview

All non-auth endpoints require `Authorization: Bearer <token>`. Role gates are enforced with `@PreAuthorize`.

### Auth
| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | public | Create an account (returns a JWT) |
| `POST` | `/auth/login` | public | Log in (returns a JWT) |

### Vendors & menu
| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| `POST` | `/vendors` | authenticated | Apply to become a vendor (status `PENDING`) |
| `GET` | `/vendors` | authenticated | List approved vendors (paginated) |
| `GET` | `/admin/vendors?status=` | `ADMIN` | List vendors by status |
| `POST` | `/admin/vendors/{id}/approve` | `ADMIN` | Approve a pending vendor |
| `POST` | `/admin/vendors/{id}/suspend` | `ADMIN` | Suspend an approved vendor |
| `GET/POST/PUT/DELETE` | `/vendors/{vendorId}/menu-items` | `VENDOR` | Menu CRUD (ownership enforced) |
| `PATCH` | `/vendors/{vendorId}/menu-items/{id}?available=` | `VENDOR` | Toggle availability |
| `GET` | `/vendors/{vendorId}/menu` | authenticated | Public menu of an approved vendor (paginated) |

### Orders
| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| `POST` | `/orders` | `CUSTOMER` | Place an order; server recomputes totals and snapshots prices |
| `POST` | `/orders/{id}/status` | `VENDOR` | Advance order status (vendor transitions: ACCEPTED → PREPARING → READY → OUT_FOR_DELIVERY → DELIVERED; can also REJECT) |
| `POST` | `/orders/{id}/cancel` | `CUSTOMER` | Cancel an order (cancellable only in PLACED, ACCEPTED, PREPARING states) |
| `GET` | `/orders` | `CUSTOMER` | List own orders (paginated) |
| `GET` | `/orders/vendor` | `VENDOR` | List orders for own vendors (paginated) |

---

## Project structure

```
src/main/java/com/mealsnow
├── common/            # BaseEntity, DataSeeder, global error handling
│   └── error/         # ApiError, exceptions, @RestControllerAdvice
├── identity/          # User, Role, auth (register/login/JWT), security config
│   ├── auth/
│   └── security/
├── vendor/            # Vendor entity, onboarding, admin approval
├── catalog/           # MenuItem entity + CRUD + public browse
└── order/             # Order, OrderItem, OrderStatus state machine
    ├── dto/
    └── payment/       # PaymentService interface + mock implementation

src/main/resources
├── application.yml
└── db/migration/      # Flyway: V1__enable_extensions, V2__create_core_tables
```

---

## Design decisions

- **Modular monolith over microservices** — learn clean module boundaries and deploy one image; extract a service only when real pressure demands it.
- **Mock payments behind an interface** — the order flow depends on `PaymentService`, not a concrete gateway, so a real provider drops in later with no changes to order logic.
- **State transitions as data** — one `EnumMap` + one guard method, so the rules can't be bypassed or contradicted across the codebase.
- **Guards throw, they don't skip** — an invalid order line (wrong vendor, unavailable item) aborts the whole transaction rather than silently dropping items and mis-totalling the order.
- **Server-authoritative pricing** — the client sends only *what* and *how many*; the server decides *how much* from live prices, then snapshots them.
- **Errors return 404 to hide existence** — customers hitting a non-approved vendor get `404`, not `403`, so the API doesn't leak which vendors exist.

---

## Roadmap

| Phase | Focus | Status |
| --- | --- | --- |
| 0 | Project skeleton, PostgreSQL, Docker, Flyway | ✅ Done |
| 1 | Domain model, migrations, repositories, seed data, Testcontainers | ✅ Done |
| 2 | Security: register/login, JWT, roles, Swagger | ✅ Done |
| 3 | Vendor onboarding, admin approval, menu CRUD, global error handling | ✅ Done |
| 4 | Order lifecycle: creation + price snapshot, mock payment, state machine, vendor/customer status updates, paginated queries, tests | ✅ Done |
| 5 | Kafka events + WebSocket real-time status updates | 🚧 In progress (steps 1–2 done: Kafka setup + event publishing; step 3 consumer + step 4 WebSocket next) |
| 6 | React frontend (all three roles, live order tracking) | ⬜ Planned |
| 7 | Production hardening: tests, observability, config/secrets | ⬜ Planned |
| 8 | Dockerize & deploy to GCP (Cloud Run + Cloud SQL + managed Kafka) | ⬜ Planned |

---

## License

No license specified yet. This is a personal learning project.
