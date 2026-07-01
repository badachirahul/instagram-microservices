# JWT Contract

> **Single source of truth** for the JWT that user-service issues and every service
> validates *locally*. Freeze this alongside the event contract — both Rahul and Divya
> build against it. No service calls user-service to check a token.

## Algorithm & key

- **Algorithm:** `HS256` (HMAC-SHA256, symmetric — same secret signs and verifies).
- **Shared secret (dev):**

  ```
  I0KQjwMx0u/vwwt0/UQ2bu9uePtTQm8aysfPmd/OucvzNt7sDrNwVLeTcJPyNuMu
  ```

  This is a **dev-only** secret, shared so both developers' services interoperate on one
  machine / one Kafka+Postgres. Every service reads it from `app.jwt.secret`, which
  defaults from the `JWT_SECRET` env var (see each `application.yml`). For a real
  deployment this would be injected per-environment, never committed — fine to commit here
  because it's a student/demo project.

  To use it, export before running any service (or leave the yml default in place):
  ```bash
  export JWT_SECRET='I0KQjwMx0u/vwwt0/UQ2bu9uePtTQm8aysfPmd/OucvzNt7sDrNwVLeTcJPyNuMu'
  ```

## Claims

Every token carries exactly these claims:

| Claim      | Type            | Meaning                                      |
|------------|-----------------|----------------------------------------------|
| `sub`      | string (uuid)   | the user's id — the authenticated user       |
| `username` | string          | the user's username                          |
| `iat`      | number (epoch s)| issued-at                                    |
| `exp`      | number (epoch s)| expiry — **24h** after `iat`                 |

Example decoded payload:
```json
{
  "sub": "3f8c1e2a-...-uuid",
  "username": "rahul",
  "iat": 1751365200,
  "exp": 1751451600
}
```

## How each side uses it

- **user-service (Divya)** — *issues* the token on `register`/`login`: sign with the shared
  secret, set `sub`, `username`, `iat`, `exp` (24h).
- **post-service (Rahul) & notification-service (Divya)** — *validate* the token locally on
  every protected request: verify the signature with the shared secret, check `exp`, then
  read `sub` (userId) and `username` from the claims.

The extracted `userId`/`username` become the identity used in business logic **and** in the
event fields defined by the event contract — e.g. post-service fills `likerId`/
`likerUsername` from the token when publishing `post.liked`. This is why the token carries
`username`: so producers never have to look it up.

## Transport

Clients send it as: `Authorization: Bearer <token>`.
