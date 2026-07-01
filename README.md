# Instagram Clone — Microservices Project

A small **Instagram-style app** built as **microservices** to demonstrate an event-driven
architecture with **Apache Kafka**. Two developers build it in parallel; each owns
services and they integrate over a shared **event contract**.

> **Scope is deliberately small.** The goal is to show clean **service boundaries +
> Kafka producer/consumer + an API gateway**, NOT to build real Instagram. Keep it lean.

---

## 1. Team & ownership

| Developer | Owns | Kafka role |
|-----------|------|------------|
| **Rahul** | **Post service** + **API Gateway** + **Frontend** | Producer |
| **(Friend)** | **User service** + **Notification service** | Producer + Consumer |
| **Both (together only)** | Event contract, `docker-compose`, final integration, PR reviews | — |

**Golden rule:** design the boundaries + event contract **together**, then build your
services **apart**, then integrate **together**. The event contract (Section 6) is what
lets us work in parallel without waiting on each other.

---

## 2. Tech stack (LOCKED)

- **Backend:** Java 17, Spring Boot, Maven (one Maven project per service)
- **Messaging:** Apache Kafka (event backbone between services)
- **Database:** PostgreSQL — **one database per service** (no shared tables)
- **Gateway:** Spring Cloud Gateway
- **Frontend:** React + Vite + Tailwind CSS + axios (one SPA)
- **Auth:** JWT issued by the User service; every service validates it locally with a shared secret
- **Infra:** Docker Compose (Kafka + Postgres)

---

## 3. Architecture

```
                         ┌───────────────────────┐
                         │   Frontend (React)     │  :5173
                         │   talks ONLY to gateway│
                         └───────────┬───────────┘
                                     │ REST
                         ┌───────────▼───────────┐
                         │   API Gateway          │  :8080
                         │   (Spring Cloud Gateway)│
                         └───┬───────┬───────┬────┘
                REST │       │       │       │ REST
          ┌──────────▼──┐ ┌──▼─────────┐ ┌──▼───────────────┐
          │ User service│ │Post service│ │Notification service│
          │   :8081     │ │   :8082    │ │      :8083         │
          │ users,follow│ │posts,likes,│ │  notifications     │
          │             │ │  comments  │ │                    │
          └──────┬──────┘ └─────┬──────┘ └─────────▲──────────┘
                 │ publish       │ publish          │ consume
                 │  user.followed│  post.liked      │
                 │               │  comment.added   │
                 └───────────────┴──►  KAFKA  ──────┘
                                 (topics: user-events, post-events)
```

**How services communicate (important):**
- **Frontend → Gateway → services:** synchronous **REST** (request/response).
- **Service → service:** asynchronous **Kafka events** (fire-and-forget). No direct
  REST calls between services.
- **Auth:** the JWT is validated *locally* inside each service (shared secret), so no
  service has to call the User service just to check a token.
- **Event-carried state:** events include everything the consumer needs (e.g. the
  liker's username), so the Notification service never has to call another service.

---

## 4. The services

### 4.1 User service — `:8081` — DB `userdb`
Owns identity and the social graph.

**Tables:** `users`, `follows`

**REST endpoints:**
| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/auth/register` | no | create user, return JWT |
| POST | `/api/auth/login` | no | login, return JWT |
| GET | `/api/users/{id}` | yes | user profile |
| POST | `/api/users/{id}/follow` | yes | follow a user |
| DELETE | `/api/users/{id}/follow` | yes | unfollow |
| GET | `/api/users/{id}/followers` | yes | list followers |
| GET | `/api/users/{id}/following` | yes | list following |

**Publishes:** `user.followed`

---

### 4.2 Post service — `:8082` — DB `postdb`
Owns posts and engagement.

> **Images via Cloudinary.** The frontend uploads the file **directly to Cloudinary**
> (unsigned) and gets back a hosted `secure_url`; it then sends only that **URL string** to
> this service. So the Post service never handles files — it just stores an `imageUrl`.
> Do NOT build your own file storage / S3 / resizing. See §5.1 for setup.

**Tables:** `posts`, `likes`, `comments`

**REST endpoints:**
| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/posts` | yes | create post `{caption, imageUrl}` |
| GET | `/api/posts/{id}` | yes | one post |
| GET | `/api/posts?userId=` | yes | a user's posts |
| GET | `/api/feed` | yes | posts (newest first) — simple, no ranking |
| POST | `/api/posts/{id}/like` | yes | like |
| DELETE | `/api/posts/{id}/like` | yes | unlike |
| POST | `/api/posts/{id}/comments` | yes | comment `{text}` |
| GET | `/api/posts/{id}/comments` | yes | list comments |

**Publishes:** `post.created`, `post.liked`, `comment.added`

---

### 4.3 Notification service — `:8083` — DB `notificationdb`
Purely reactive. It has **no interesting write endpoints** — it just consumes events and
produces notifications.

**Tables:** `notifications`

**REST endpoints:**
| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/notifications` | yes | my notifications (newest first) |
| GET | `/api/notifications/unread-count` | yes | badge count |
| PATCH | `/api/notifications/{id}/read` | yes | mark read |

**Consumes:** `post.liked`, `comment.added`, `user.followed`

**Notification rules:**
| Event consumed | Notify whom | Message | Skip when |
|----------------|-------------|---------|-----------|
| `post.liked` | `postOwnerId` | "{likerUsername} liked your post" | liker == owner |
| `comment.added` | `postOwnerId` | "{commenterUsername} commented on your post" | commenter == owner |
| `user.followed` | `followeeId` | "{followerUsername} started following you" | — |

---

### 4.4 API Gateway — `:8080`
Thin routing layer (Spring Cloud Gateway). The frontend only ever calls `:8080`.

Route rules (example):
```
/api/auth/**          -> user-service:8081
/api/users/**         -> user-service:8081
/api/posts/**         -> post-service:8082
/api/feed             -> post-service:8082
/api/notifications/** -> notification-service:8083
```
Also handles CORS for the frontend.

---

## 5. Frontend — `:5173`
**One** React + Vite app that talks only to the gateway. Reuse the Vite + Tailwind + axios
setup you already know.

**Keep it minimal — build it LAST, after the services + Kafka work end to end.** Pages:
- Login / Register
- Feed (list posts; a post = image + caption + like button + comments)
- Create post (**upload a photo → Cloudinary** returns a URL + caption)
- Notifications (list + unread badge)

> If time runs out, **Postman/curl against the gateway is an acceptable substitute** for a
> UI — the backend event flow is the real deliverable. (For Postman you can skip the upload
> and pass any image URL as `imageUrl` directly.)

### 5.1 Image uploads — Cloudinary (unsigned, from the frontend)

Real photo uploads *without* building storage yourself. The **browser uploads directly to
Cloudinary**; only the resulting URL touches our services — so the backend and the event
contract stay unchanged (`imageUrl` is always just a string).

```
[React] --(file)--> [Cloudinary] --(secure_url)--> [React] --(POST /api/posts {caption, imageUrl})--> Post service
```

**Setup (one-time, ~5 min):**
1. Create a free **Cloudinary** account → note your **cloud name**.
2. Settings → **Upload** → add an **unsigned upload preset** → note the **preset name**.
3. In the frontend, upload the selected file, then use the returned `secure_url` as `imageUrl`:

```js
async function uploadImage(file) {
  const form = new FormData();
  form.append("file", file);
  form.append("upload_preset", CLOUDINARY_UPLOAD_PRESET); // unsigned preset
  const res = await fetch(
    `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`,
    { method: "POST", body: form }
  );
  const data = await res.json();
  return data.secure_url; // <-- send this as imageUrl to the Post service
}
```

> This lives entirely in the **frontend** (Rahul's side) — it does not affect the User,
> Post, or Notification services. Unsigned uploads are fine for a student demo; signed
> uploads (backend generates a signature) are a nice-to-have, not required.

---

## 6. EVENT CONTRACT (freeze this first — it is the boundary)

All events are JSON. Every event shares an **envelope** (`eventId`, `type`, `occurredAt`)
plus event-specific fields. Producers must fill every field a consumer needs
(**event-carried state** — no callbacks between services).

**Topics**
- `user-events` — published by User service
- `post-events` — published by Post service

### `user.followed`  (topic: `user-events`)
```json
{
  "eventId": "uuid",
  "type": "user.followed",
  "occurredAt": "2026-07-01T10:00:00Z",
  "followerId": "uuid",
  "followerUsername": "rahul",
  "followeeId": "uuid"
}
```

### `post.created`  (topic: `post-events`)
```json
{
  "eventId": "uuid",
  "type": "post.created",
  "occurredAt": "2026-07-01T10:00:00Z",
  "postId": "uuid",
  "authorId": "uuid",
  "authorUsername": "rahul",
  "caption": "sunset",
  "imageUrl": "https://.../pic.jpg"
}
```

### `post.liked`  (topic: `post-events`)
```json
{
  "eventId": "uuid",
  "type": "post.liked",
  "occurredAt": "2026-07-01T10:00:00Z",
  "postId": "uuid",
  "postOwnerId": "uuid",
  "likerId": "uuid",
  "likerUsername": "rahul"
}
```

### `comment.added`  (topic: `post-events`)
```json
{
  "eventId": "uuid",
  "type": "comment.added",
  "occurredAt": "2026-07-01T10:00:00Z",
  "postId": "uuid",
  "postOwnerId": "uuid",
  "commenterId": "uuid",
  "commenterUsername": "rahul",
  "text": "nice shot!"
}
```

**Consumer note:** the Notification service switches on `type` and applies the rules in
Section 4.3. It can build every notification from the event alone — it never calls another
service.

---

## 7. Repository structure (monorepo)

One repo, one folder per service, so the two of you almost never touch the same files.

```
instagram-microservices/
├── README.md                 <- this file
├── docker-compose.yml        <- Kafka + Postgres (shared, edit together)
├── docs/
│   └── event-contract.md     <- (optional) Section 6 split out for quick reference
├── gateway/                  <- Rahul
├── user-service/             <- Friend
├── post-service/             <- Rahul
├── notification-service/     <- Friend
└── frontend/                 <- Rahul (built last, together)
```

Each backend folder is an independent Spring Boot Maven project (its own `pom.xml`,
`application.yml`, Flyway migrations).

---

## 8. Infrastructure (Docker Compose)

Run **infra in Docker, services locally** via `./mvnw spring-boot:run` (same workflow as a
normal Spring project — faster to iterate than dockerising every service).

`docker-compose.yml` provides:
- **Kafka** (single broker, KRaft mode — no separate Zookeeper needed) on `:9092`
- **PostgreSQL** on host port **`5433`** (→ container 5432), with three databases:
  `userdb`, `postdb`, `notificationdb`

Bring infra up:
```bash
docker compose up -d
```

---

## 9. Ports

| Component | Port |
|-----------|------|
| Frontend (Vite) | 5173 |
| API Gateway | 8080 |
| User service | 8081 |
| Post service | 8082 |
| Notification service | 8083 |
| Kafka | 9092 |
| PostgreSQL | 5433 (host) |

---

## 10. Configuration each service needs

- `spring.datasource.url` → its own DB (`jdbc:postgresql://localhost:5433/<db>`)
- `spring.kafka.bootstrap-servers=localhost:9092`
- `app.jwt.secret=<SAME shared secret in every service>` (env var — never commit it)
- Kafka consumer `group-id` (Notification service): `notification-service`

---

## 11. Git workflow

- Default branch: **`main`**.
- Work on a **feature branch** (`feat/post-like`, `feat/notif-consumer`), open a **Pull
  Request**, the **other person reviews**, then merge.
- You'll rarely conflict because you're in different folders. The only **shared files**
  (`docker-compose.yml`, `README.md`/contract) — change those **together** or announce it.
- Both people commit under their own name so `git log` shows real 2-person work.

---

## 12. Build order (phases)

**Phase 0 — TOGETHER (do first):**
1. Agree service split + **freeze the event contract** (Section 6).
2. Write `docker-compose.yml` (Kafka + Postgres, 3 DBs).
3. Create the empty Spring Boot skeletons that just boot.

**Phase 1 — IN PARALLEL (nobody waits):**
- *Rahul:* User? no — **Post service**: CRUD posts/likes/comments, **produce** events to
  `post-events`. Verify events land in Kafka (console consumer).
- *Friend:* **User service** (auth + follow, produce `user.followed`) **and** **Notification
  service** (consume events → notifications). Test the consumer with a **hand-pushed event**
  (Kafka console producer) so you don't wait for Rahul's producer.

**Phase 2 — TOGETHER:**
- Add the **Gateway**, run the full stack, watch **like → Kafka → notification** end to end.
- Debug integration jointly.

**Phase 3 — TOGETHER (if time):**
- Minimal **frontend**, README polish, architecture diagram, demo script.

---

## 13. How to run (local dev)

```bash
# 1. clone
git clone <repo-url> && cd instagram-microservices

# 2. start infra (Kafka + Postgres)
docker compose up -d

# 3. run each service (separate terminals)
cd user-service && ./mvnw spring-boot:run          # :8081
cd post-service && ./mvnw spring-boot:run          # :8082
cd notification-service && ./mvnw spring-boot:run  # :8083
cd gateway && ./mvnw spring-boot:run               # :8080

# 4. frontend
cd frontend && npm install && npm run dev          # :5173
```

**Watch events on Kafka (handy for debugging):**
```bash
# consume everything on post-events
docker exec -it <kafka-container> kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic post-events --from-beginning
```

---

## 14. Definition of done (demo script)

You can demo the whole thing without a UI:
1. **Register** two users (User service).
2. User A **likes** User B's post (Post service) → a `post.liked` event appears on
   `post-events` (visible in the console consumer).
3. **GET** User B's notifications (Notification service) → "A liked your post" is there.
4. User A **follows** User B → `user.followed` → B gets "A started following you".

If that flow works, the assignment's core — **3 services, an API gateway, and Kafka
carrying events between producers and a consumer** — is complete. The frontend is icing.

---

## 15. Things to deliberately SKIP (protect your time)

- ❌ Your **own** image storage / S3 / resizing — use **Cloudinary** (unsigned frontend upload); services still just store an `imageUrl` string (§5.1)
- ❌ Feed ranking / algorithms (just newest-first)
- ❌ A 4th/5th service — three is enough
- ❌ Fancy UI — minimal or Postman
- ❌ Direct REST calls between services — use events (put needed data *in* the event)
```
