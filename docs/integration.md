# Integration Guide

How to run the whole system locally, and what **Divya** does after pulling once her
**user-service** (and **notification-service**) are implemented. Rahul's side
(post-service + gateway + frontend) is already built and verified end-to-end.

---

## 1. Run the whole stack locally

### Shared infra (Postgres + Kafka + Kafka UI)
```bash
docker compose up -d
```
- Postgres → `localhost:5433` (user/pass `insta`/`insta`, DBs `userdb` / `postdb` /
  `notificationdb` created on first boot).
- Kafka → `localhost:9092`. Kafka UI → http://localhost:8090
- **Note:** Kafka image is `apache/kafka:3.7.0` (the old `bitnami/kafka:3.7` tag was
  removed from Docker Hub). If port `5433` is already taken on your machine, stop whatever
  owns it or change the mapping in `docker-compose.yml`.

### Services (each in its own terminal, from the service dir)
```bash
cd user-service      && ./mvnw spring-boot:run   # :8081
cd post-service      && ./mvnw spring-boot:run   # :8082
cd notification-service && ./mvnw spring-boot:run # :8083
cd gateway           && ./mvnw spring-boot:run   # :8080
```
All services read the **same** shared JWT secret from `app.jwt.secret` (env `JWT_SECRET`,
with a dev default baked in) — see `docs/jwt-contract.md`.

### Frontend
```bash
cd frontend
npm install     # first time only
npm run dev     # http://localhost:5173
```

---

## 2. Divya — checklist after pulling (user-service implemented)

### 2a. Make the JWT match, exactly
The whole system hinges on every service verifying the same token. In **user-service**:
- Sign with **HS256** using `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`
  — the secret's **raw UTF-8 bytes**, same secret string as everyone else. Do **not**
  base64-decode it. (jjwt `0.12.x`.)
- Claims: `sub` = userId (uuid), `username`, `iat`, `exp` (24h).
- See `docs/jwt-contract.md` — this is frozen; if a token from user-service is rejected by
  post-service, the mismatch is almost always the key-derivation step above.

**Quick cross-service check** (proves your token works against Rahul's service):
```bash
# 1) register/login via YOUR user-service to get a token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"divya","password":"..."}' | jq -r .token)   # adjust to your response shape

# 2) call Rahul's post-service THROUGH THE GATEWAY with it — expect 200
curl -s -w '\n%{http_code}\n' -H "Authorization: Bearer $TOKEN" localhost:8080/api/feed
```
`200` = the shared-secret contract works. `401` = re-check the key derivation / claims.

### 2b. Events
- **user-service** publishes `user.followed` to the `user-events` topic (include
  `followerUsername` — event-carried state).
- **notification-service** consumes **both** `user-events` and `post-events`
  (group-id `notification-service`), switches on the `type` field, applies the notify/skip
  rules in `docs/event-contract.md`. Watch the topics:
```bash
docker exec insta-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic post-events --from-beginning
```

### 2c. Gateway routes (already wired — verify only)
`gateway/src/main/resources/application.yml` already routes `/api/auth/**` and
`/api/users/**` → `:8081`, and `/api/notifications/**` → `:8083`. Nothing to add unless
your paths differ.

---

## 3. Swap the frontend's dev-login for the real one

Right now the frontend mints a JWT **in the browser** (dev shortcut) because there was no
user-service to log in against. Once user-service is live, replace it with a real call:

- `frontend/src/devAuth.js` — delete or stop using; instead POST to `/api/auth/login`
  (and `/api/auth/register`) and store the returned token.
- `frontend/src/App.jsx` — the `Login` component currently calls `devLogin(username)`;
  point it at the real endpoint and add a password field.
- Everything else in the frontend (`api.js` interceptor, feed, like, comment) stays the
  same — it already sends `Authorization: Bearer <token>` to the gateway.

---

## 4. Full end-to-end smoke test (once both sides run)

1. Register two users (rahul, divya) via user-service.
2. rahul creates a post → `post.created` → divya gets no notification (self skip N/A).
3. divya likes + comments on rahul's post → `post.liked` + `comment.added` → **rahul** gets
   two notifications (`GET /api/notifications`).
4. divya follows rahul → `user.followed` → **rahul** gets a "started following you"
   notification.
5. Unread badge: `GET /api/notifications/unread-count`.
