# materialGrid — User/Auth, Price Rate, Route, Vehicle, License & Fleet Operations Backend

A production-oriented Spring Boot backend demonstrating clean layered
architecture, DB-enforced single-active-session login, DB-enforced
single-active-price-rate business rules, concurrency-safe business-code
generation, and fleet-operations tracking (routes, vehicles, licenses,
expenses, and daily route billing) — all under real concurrency.


## 1. Project Overview

Three capabilities:

- **User management** — CRUD, unique usernames, BCrypt-hashed passwords.
- **Authentication & session management** — login/logout, server-side
  sessions, **only one active session per user at a time**, enforced so that
  it's actually true under concurrent requests, not just "true in the
  database eventually."
- **Price rate management** — CRUD with the rule that **at most one price
  rate can be ACTIVE at any moment**, also enforced under concurrency.

## 2. Architecture

Classic layered architecture:

```
controller  -> HTTP concerns only (mapping, status codes, @Valid)
service     -> business rules, transactions, orchestration
repository  -> Spring Data JPA, persistence only
entity      -> JPA-mapped persistence model, never returned from controllers
dto         -> request/response contracts, decoupled from entities
mapper      -> entity <-> DTO translation
security    -> authentication + the custom session-validation filter
exception   -> centralized error handling, typed business exceptions
config      -> Spring configuration (security, JPA, Jackson, OpenAPI)
constant    -> fixed strings/codes, no magic values scattered in code
```

Why this shape: each layer has one reason to change (SRP), controllers stay
thin and testable via MockMvc, services are the only place transactions and
business rules live (so those rules can't be bypassed by a different
controller), and swapping persistence or transport details doesn't ripple
through business logic. DTOs at the boundary mean the JPA entity graph is
never serialized directly, so adding a lazy-loaded relationship later can't
accidentally produce a `LazyInitializationException` or leak internal fields
(like `password`) through the API.

## 3. Technology Stack

Java 25 · Spring Boot 3.4 · Maven · Spring Web · Spring Data JPA · MySQL 8 ·
Spring Security · Jakarta Bean Validation · Lombok · Hibernate · Jackson ·
SLF4J/Logback · Flyway · JUnit 5 · Mockito · Testcontainers · springdoc-openapi.

## 4. Authentication & Session Design (read this first)

**Why not JWT?** A stateless JWT, once issued, stays cryptographically valid
until it expires — a DB "revoked" flag doesn't invalidate it unless every
request also checks the DB, at which point you've just reinvented server-side
sessions with extra steps. So this project uses **opaque, random,
server-side session tokens** directly:

1. `POST /api/v1/auth/login` verifies credentials, then calls
   `SessionService.createSession(userId)`.
2. That method (`SessionServiceImpl`) runs in `REQUIRES_NEW`, takes a
   **pessimistic write lock** on the user's current `ACTIVE` session row (if
   any) via `UserSessionRepository#findActiveByUserIdForUpdate`, flips it to
   `LOGGED_OUT`, and inserts a new `ACTIVE` row with a fresh random token.
3. Every subsequent request passes the token as
   `Authorization: Bearer <token>`. `SessionAuthenticationFilter` (a
   `OncePerRequestFilter` registered before Spring Security's own auth
   filter) looks the token up **fresh, on every request**, and only
   authenticates if the matching row is still `ACTIVE` (and not idle-expired).

Because step 3 re-checks the database every time, the moment a token's row is
flipped to `LOGGED_OUT` (superseded by a newer login, or an explicit logout),
that token is functionally dead on the very next request — not "eventually,"
immediately.

### Concurrency: two simultaneous logins for the same user

If two login requests race, both reach `createSession`. The pessimistic lock
means the second transaction blocks until the first commits; it then
re-reads the (now-committed) active session — which is the *first* request's
new session — and supersedes that one too, landing on a single, correct
final state. As a final backstop independent of the locking, the database
schema itself enforces at most one `ACTIVE` session per user (see below).

## 5. Price Rate Business Rules & Concurrency

- **Rule 1–3**: creating or updating a rate to `ACTIVE` auto-deactivates
  whatever rate was previously active, in the same transaction.
- **Rule 4** (judgment call, documented here since the prompt asked for the
  decision to be made explicit): deactivating the *sole* active rate
  (`ACTIVE -> INACTIVE` with nothing to replace it) is **rejected** with a
  409 `BUSINESS_RULE_VIOLATION`. Rationale: a back-office pricing system
  being priceless is a worse failure mode than a rejected request — a caller
  who wants "no rate in effect" should get there by never creating one, not
  by deactivating the last one. If your business actually wants to allow a
  gap with no active rate, remove the `becomingInactive` guard in
  `PriceRateServiceImpl.updatePriceRate`.
- **Concurrency**: `PriceRateRepository#findActiveForUpdate` takes a
  pessimistic write lock on the currently-active row before deactivating it,
  inside the same transaction as activating the new one. Two concurrent
  "activate rate A" / "activate rate B" requests are serialized by this
  lock, not left to race — see `PriceRateConcurrencyIntegrationTest`, which
  fires both concurrently against a real MySQL container and asserts exactly
  one `ACTIVE` row survives.

## 6. Database-Level Enforcement (the real backstop)

MySQL has no native *partial* unique index (`UNIQUE ... WHERE status =
'ACTIVE'` isn't supported), so both "only one active X" rules are enforced
with the same trick: a `STORED` generated column that evaluates to a
non-null value only for the "active" row, with a `UNIQUE` constraint on that
column. MySQL unique indexes permit unlimited `NULL`s but never more than one
of any given non-null value — so the database itself physically refuses a
second concurrently-active row, independent of whether application code got
the locking right. See `V2__create_user_sessions_table.sql` and
`V3__create_price_rates_table.sql`.

Locking is what makes the *outcome* correct (the loser of a race gets a
consistent, expected result instead of an exception); the generated-column
constraint is what makes an inconsistent state *impossible* even if a future
code change forgets the locking.

## 7. Database Setup

```sql
CREATE DATABASE materialGrid CHARACTER SET utf8mb4;
CREATE USER 'materialGrid'@'%' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON materialGrid.* TO 'materialGrid'@'%';
```

Schema is managed entirely by **Flyway** (`src/main/resources/db/migration`);
`ddl-auto` is set to `validate`, never `create`/`update`, so the database
schema is the single source of truth and drift is caught, not silently
patched, at startup.

## 8. Environment Variables

See `.env.example`. Required: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
Optional: `SPRING_PROFILES_ACTIVE` (`dev`/`test`/`prod`), `SERVER_PORT`,
`SESSION_IDLE_TIMEOUT_MINUTES`. Nothing sensitive is hard-coded or committed.

## 9. Running the Project

```bash
export $(cat .env | xargs)   # or set the vars another way
mvn spring-boot:run
```

A real Maven Wrapper wasn't generated in this environment (no network access
to Maven Central here) — the included `mvnw` is a placeholder. Generate the
real one locally with:

```bash
mvn -N io.takari:maven:wrapper -Dmaven=3.9.9
```

or simply use a locally installed `mvn`.

## 10. Running Tests

```bash
mvn test                 # unit tests (Mockito) - no external dependencies
mvn verify -Pintegration # integration tests - require a Docker daemon
                          # (Testcontainers spins up real MySQL 8)
```

Unit tests (`src/test/.../service/*Test.java`) cover user CRUD, duplicate
username, price-rate activation/deactivation rules, sole-active-rate
deletion/deactivation guards, and session supersession logic in isolation.
Integration tests (`src/test/.../integration/*IntegrationTest.java`) run the
actual race conditions — concurrent price-rate activation, and "old session
token stops working the instant a new login happens" — against a real MySQL
instance via Testcontainers.

## 11. API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 12. Example Requests

**Login**
```
POST /api/v1/auth/login
{ "username": "admin", "password": "Passw0rd1" }

200 OK
{
  "success": true,
  "message": "Login successful",
  "data": { "sessionToken": "xY3f...", "username": "admin", "tokenType": "Bearer" },
  "timestamp": "2026-08-16T10:00:00"
}
```

**Authenticated request**
```
GET /api/v1/price-rates/active
Authorization: Bearer xY3f...
```

**Activating a price rate**
```
PUT /api/v1/price-rates/2
Authorization: Bearer xY3f...
{ "price": 150.0000, "status": "ACTIVE" }
```
→ rate `2` becomes `ACTIVE`; whatever rate was previously `ACTIVE`
automatically flips to `INACTIVE` in the same transaction.

**Error shape**
```json
{
  "success": false,
  "message": "Cannot deactivate the only active price rate. Activate a replacement rate instead.",
  "data": null,
  "timestamp": "2026-08-16T10:05:00",
  "errorCode": "BUSINESS_RULE_VIOLATION"
}
```

## 13. Auditing Approach

`addedBy`/`modifiedBy`/`addedDate`/`modifiedDate` (and the `User` equivalents)
are populated **explicitly in the service layer** from
`SecurityUtil.getCurrentUsername()` (which reads the validated
`SecurityContext`, never a client-supplied field) rather than via Spring Data
JPA's `@CreatedBy`/`@LastModifiedBy` auditing infrastructure. This is a
deliberate choice for a security-sensitive audit trail: an explicit,
one-line, code-reviewable assignment in the service method is easier to
verify correct than an implicit `AuditorAware`-driven entity listener. See
`config/JpaConfig.java` for the fuller note and the extension point if you'd
prefer to switch to JPA auditing later.


## 14. Security Configuration Notes

- **CSRF is disabled deliberately**, not blindly: this API authenticates via
  a bearer token in the `Authorization` header, never a cookie, so it isn't
  exposed to the cross-site-cookie attack CSRF protection defends against.
  If cookie-based auth is ever introduced, re-enable CSRF protection.
- **`SessionCreationPolicy.STATELESS`**: Spring Security itself holds no
  `HttpSession`. All session semantics are the explicit, DB-backed
  `UserSession` described above — avoiding two different, driftable notions
  of "session" in the same app.
- **BCrypt** for password hashing (adaptive cost, built-in salt). Passwords
  are never logged, never returned in API responses, never compared in
  plain text.

## 15. materialGrid Extension — Route, Vehicle, License, VehicleLicense, VehicleExpense, DailyRoute

The base project above was extended (package `com.pixelMind.materialGrid`, artifact `materialGrid`) with six operational modules on top of the existing User/Auth/PriceRate foundation. All previously implemented behavior (auth, sessions, price rate rules, exception handling, etc.) is unchanged.

### A. Route Code / License Code generation — concurrency-safe by design

Both codes (`RT000001`, `LIC000001`, ...) are generated by `CodeGeneratorService`, backed by a dedicated `code_sequences` table (one counter row per code type, seeded by Flyway). Generation locks the relevant counter row with `SELECT ... FOR UPDATE` inside its own `REQUIRES_NEW` transaction, increments it, and returns the formatted code — all before the Route/License row itself is even built. This is deliberately **not** `SELECT COUNT(*) + 1`: count-based generation lets two concurrent inserts both read the same count and mint the same "next" code, and breaks permanently the moment any row is ever deleted. The dedicated counter table can't do either, and `RouteCodeConcurrencyIntegrationTest` fires 20 concurrent route-creation requests against a real MySQL instance and asserts all 20 codes come back unique. A rolled-back creation leaves a small gap in the sequence rather than ever reusing a number — the correct trade-off for business identifiers (gaps are fine; duplicates are not).

### B. Why VehicleLicense is a full entity, not `@ManyToMany`

The Vehicle↔License relationship carries its own business data (`date`, `status`) that a bare join table can't express — JPA's `@ManyToMany` only models the *existence* of a link, not data *about* the link. `VehicleLicense` is therefore a first-class entity with `@ManyToOne` to both `Vehicle` and `License`, exposed through its own CRUD API.

### C/D. Deletion strategy & historical data

| Entity | Delete behavior | Why |
|---|---|---|
| Route, Vehicle, License, PriceRate | **Blocked** (409) if any dependent record exists | Reference/master data referenced by history — deleting it (or cascading) would corrupt or silently alter historical records that point to it. |
| VehicleLicense | Hard delete allowed | A status/assignment marker, not a financial record — correcting a mis-entered assignment shouldn't require a compensating record. |
| VehicleExpense, DailyRoute | **Soft delete** (`deleted` flag; DELETE endpoint sets it, GET/list endpoints filter it out) | These are historical financial/operational records. A hard DELETE would destroy accounting history; soft delete preserves it while still making the record disappear from normal views. |

No entity in this extension uses `CascadeType.ALL`. Every parent→child relationship is `@ManyToOne` from the child side only (no bidirectional `@OneToMany` collections on Vehicle/Route/PriceRate), avoiding unbounded object graphs, accidental N+1 queries, and the temptation to cascade-delete by default.

### E. Price Rate historical integrity in DailyRoute

`DailyRoute.priceRate` is a normal FK to the *specific* `PriceRate` row used at creation — never "whatever is currently active." Combined with a hard block on deleting a `PriceRate` that's referenced by any `DailyRoute` (added to `PriceRateServiceImpl.deletePriceRate`), a historical `DailyRoute` always correctly reports which rate it was billed under, even after that rate is later deactivated or a different rate becomes active.

### F. Resolved ambiguities (flagged rather than silently assumed)

- **`amount = route.km × priceRate.price`** is treated as the intended rule. The client never supplies `amount`; `DailyRouteServiceImpl` computes it once at creation and persists it as a snapshot on the row — it is deliberately *not* recalculated on read, so a later edit to `PriceRate.price` can't silently change historical billed amounts.
- **Which PriceRate can be used for a new DailyRoute**: only the currently `ACTIVE` one. A `DailyRoute` cannot be created against an `INACTIVE` rate — this keeps new billing consistent with "the rate currently in effect," while not touching already-created `DailyRoute` rows if that rate is later deactivated.
- **Vehicle↔License uniqueness**: no `UNIQUE(vehicle_id, license_id)` constraint. A vehicle legitimately holds the same license type again after renewal (new `date`/`status` each time); enforcing uniqueness would make renewal history impossible to represent.
- **Immutability of identifiers/associations on update**: `routeCode`, `licenseCode`, and `vehicleNumber` cannot be changed via their update endpoints, and `VehicleLicense`/`VehicleExpense`/`DailyRoute` cannot have their parent-entity associations reassigned after creation — reassigning historical records in place would make audit trails unreliable. Corrections go through delete-and-recreate (soft-delete where applicable), which itself leaves a trail.

### Indexing strategy

Indexes were added deliberately, not blindly: every column actually used as an API filter (`vehicle_id`, `license_id`, `status`, `route_date`, `route_id`, `price_rate_id`, plus the `deleted` flags used on every read of the soft-deleted tables) is indexed, since `daily_routes` in particular is expected to be the largest table in the schema (one row per vehicle/route/day) and every one of its documented query parameters needs to avoid a full scan. Columns not used for lookups (e.g. `km`, `capacity`, `expenses` amounts) are not indexed.

### New migrations

`V4` seeds the code-sequence counters; `V5`–`V10` create `routes`, `vehicles`, `licenses`, `vehicle_licenses`, `vehicle_expenses`, and `daily_routes` respectively, continuing the existing numbering without touching `V1`–`V3`.

### New tests

- `CodeGeneratorServiceTest` — formatting and sequential increment.
- `RouteServiceImplTest`, `VehicleServiceImplTest`, `LicenseServiceImplTest`, `DailyRouteServiceImplTest` — generated-code usage (never client-supplied), duplicate/validation rejections, delete-guard rejections, amount computation, soft-delete behavior.
- `RouteCodeConcurrencyIntegrationTest` — 20 concurrent route creations against real MySQL, asserts zero duplicate codes.

Run everything with the same commands as before (`mvn test`, `mvn verify -Pintegration`).
