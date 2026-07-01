# CLAUDE.md

Guidance for Claude Code when working in this repo. Read `README.md` (full plan),
`docs/event-contract.md` (frozen Kafka contract), `docs/jwt-contract.md` (frozen JWT shape),
and `docs/integration.md` (how to run everything + Divya's post-pull checklist) before
making changes.

## What this is

An **Instagram-style app built as event-driven microservices** to demonstrate clean
service boundaries + Kafka producer/consumer + an API gateway. Scope is deliberately
small — it is a learning/demo project, not real Instagram. Keep additions lean.

Two developers build in parallel against a **frozen event contract**:
- **Rahul:** Post service + API Gateway + Frontend — Kafka *producer*
- **Divya:** User service + Notification service — Kafka *producer + consumer*

> **Note to Claude:** the repo is shared (both pull the same `main`). Work only on the
> current developer's services per the assignment table below. Do not modify the other
> person's service without being asked. Ownership: **Rahul** = gateway, post-service,
> frontend; **Divya** = user-service, notification-service.

## Architecture (the rules that matter)

- **Frontend → Gateway → services:** synchronous REST. The frontend only ever calls the
  gateway (`:8080`).
- **Service → service:** asynchronous **Kafka events only**. No direct REST calls between
  services.
- **Auth:** JWT is issued by the User service and validated *locally* inside each service
  with a **shared secret** (`app.jwt.secret`) — no service calls User service to check a
  token.
- **Event-carried state:** every event carries everything a consumer needs (e.g. the
  liker's username), so consumers never call back to another service. When producing a new
  event, include all fields the consumer needs rather than an ID to resolve later.
- **One database per service** — no shared tables, no cross-service DB access.

```
Frontend :5173 → Gateway :8080 → { user :8081, post :8082, notification :8083 }
user-service ──publish──> user-events ─┐
post-service ──publish──> post-events ─┴──> notification-service (consumes both)
```

Kafka topics: `user-events` (from user-service), `post-events` (from post-service).
Consumer `group-id` for notification-service: `notification-service`.

## Services & ports

| Service              | Port | DB               | Role                                   |
|----------------------|------|------------------|----------------------------------------|
| gateway              | 8080 | —                | Spring Cloud Gateway, routes + CORS    |
| user-service         | 8081 | `userdb`         | users, follows; publishes `user.followed` |
| post-service         | 8082 | `postdb`         | posts, likes, comments; publishes `post.created`, `post.liked`, `comment.added` |
| notification-service | 8083 | `notificationdb` | consumes events → notifications        |

Each service is a **standalone Maven project** (its own `pom.xml` + `mvnw`), not a
multi-module build. Package base: `com.instagram.<service>`.

## Tech stack (LOCKED — do not swap)

Java 17 · Spring Boot 3.3.4 · Maven · PostgreSQL 16 · Apache Kafka (spring-kafka) ·
Flyway · Lombok · Spring Cloud Gateway · React + Vite + Tailwind + axios (frontend, built
last). Images use **Cloudinary unsigned upload from the browser** — the backend only ever
stores an `imageUrl` string; do NOT build file storage/S3/resizing.

## Build & run

Infra (Postgres on host port **5433**, Kafka on **9092**, Kafka UI on **8090**):
```bash
docker compose up -d
```

Per service (run from the service directory):
```bash
./mvnw spring-boot:run      # run
./mvnw test                 # test
./mvnw clean package        # build jar
```

Health check for any booted service: `GET /health` → `OK`.

## Database / migrations

- Flyway is wired; `ddl-auto: validate` (JPA never creates schema — migrations own it).
- Current `V1__init.sql` is an intentional **no-op** (Phase 0 baseline). Real tables land
  as `V2__*.sql`, `V3__*.sql`, … Add new columns/tables via new numbered migrations; never
  edit an already-applied migration.
- Postgres is one server with 3 databases created on first startup by
  `docker/postgres-init/01-create-databases.sql`. Connect at `localhost:5433`, user/pass
  `insta`/`insta`.

## Current status — Phase 1 in progress

**Phase 0 (scaffolding) is complete:** all 4 services boot with only a `HealthController`,
Flyway no-op baseline, `docker-compose`, gateway routes + CORS already configured, and the
frozen event contract. **No domain code, entities, Kafka producers/consumers, JWT, or
frontend exist yet.**

**Phase 1 = build the actual services, in parallel.** Work plan below.

### Cross-team dependency to agree on FIRST (blocks parallel work)

The JWT is issued by user-service and validated *locally* by every service, but the
token's contents aren't in the event contract. Both developers must build against the
same JWT shape:
- **Algorithm:** HS256 (symmetric, shared secret `app.jwt.secret` / env `JWT_SECRET`).
- **Claims:** `sub` = userId (uuid), `username` (string), `exp`.
- Services extract `userId`/`username` from these to fill event fields (e.g.
  `likerUsername`, `followerUsername`). Once agreed, capture it in `docs/jwt-contract.md`.

### Divya — user-service, then notification-service

user-service is the **critical path** (it issues the JWTs everyone needs) — build it first.

1. **user-service** (`:8081`, `userdb`):
   - `V2__users_follows.sql` — `users`, `follows` tables
   - JPA entities + repositories
   - Auth: `POST /api/auth/register`, `POST /api/auth/login` → return JWT (BCrypt passwords,
     sign per the JWT shape above)
   - JWT validation filter for protected routes
   - `GET /api/users/{id}`, `POST`/`DELETE /api/users/{id}/follow`, `.../followers`,
     `.../following`
   - Kafka producer → publish `user.followed` to `user-events`
2. **notification-service** (`:8083`, `notificationdb`) — reactive, build after:
   - `V2__notifications.sql` — `notifications` table
   - Kafka consumer, group-id `notification-service`, subscribes to **both** `user-events`
     and `post-events`, switches on `type`, applies the contract's notify/skip rules
   - `GET /api/notifications`, `GET .../unread-count`, `PATCH .../{id}/read`

### Rahul — post-service, then gateway, then frontend

1. **post-service** (`:8082`, `postdb`) — ✅ **DONE & verified end-to-end** (schema
   validates, JWT filter, all endpoints, Kafka producer emitting the 3 events with the
   exact contract shape). Layout: `domain/` entities, `repo/`, `dto/`, `security/` (jjwt
   HS256 filter + `@CurrentUser` resolver), `event/` (producer + event records),
   `service/PostService`, `web/` controllers. Endpoints: `POST /api/posts`,
   `GET /api/posts/{id}`, `GET /api/posts?userId=`, `GET /api/feed`, like/unlike,
   `POST`/`GET /api/posts/{id}/comments`.
2. **gateway** — routes + CORS already wired; adjust only if routes change. ✅ runs.
3. **frontend** (`frontend/`, Vite React + Tailwind + axios, `:5173`) — ✅ **built &
   working** against the gateway: feed, create post, like/unlike, comments. **Uses a
   DEV-ONLY in-browser login** (`src/devAuth.js` mints a JWT with the shared secret) because
   user-service didn't exist yet — swap for real `/api/auth/login` once it does (see
   `docs/integration.md` §3). Cloudinary upload not wired yet (paste an image URL for now).

> **docker-compose note:** Kafka image is `apache/kafka:3.7.0` (the old `bitnami/kafka:3.7`
> tag was pulled from Docker Hub). Uses `KAFKA_*` (not `KAFKA_CFG_*`) env vars. Services run
> on the host and reach Kafka at `localhost:9092`.

## Conventions

- Match existing code style; keep it minimal. Endpoints and event shapes must match the
  tables in `README.md` §4 and `docs/event-contract.md` exactly.
- Do not change the event contract unilaterally — it is the boundary both developers build
  against. Contract changes require agreement (see README "golden rule").
- Build backend + Kafka flow end-to-end first; build the frontend last (curl/Postman
  against the gateway is an acceptable stand-in per the README).
