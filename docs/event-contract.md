# Event Contract

> **Single source of truth** for the Kafka events exchanged between services.
> Freeze this first — it is the boundary that lets us build services in parallel.
> (Mirrors Section 6 of the root `README.md`.)

All events are JSON. Every event shares an **envelope** (`eventId`, `type`,
`occurredAt`) plus event-specific fields. Producers must fill every field a
consumer needs (**event-carried state** — no callbacks between services).

## Topics

| Topic         | Published by         | Consumed by            |
|---------------|----------------------|------------------------|
| `user-events` | User service         | Notification service   |
| `post-events` | Post service         | Notification service   |

---

## `user.followed`  (topic: `user-events`)

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

## `post.created`  (topic: `post-events`)

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

## `post.liked`  (topic: `post-events`)

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

## `comment.added`  (topic: `post-events`)

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

---

## Consumer rules (Notification service)

The Notification service switches on `type` and applies these rules. It can
build every notification from the event alone — it never calls another service.

| Event consumed  | Notify whom    | Message                                    | Skip when          |
|-----------------|----------------|--------------------------------------------|--------------------|
| `post.liked`    | `postOwnerId`  | "{likerUsername} liked your post"          | liker == owner     |
| `comment.added` | `postOwnerId`  | "{commenterUsername} commented on your post" | commenter == owner |
| `user.followed` | `followeeId`   | "{followerUsername} started following you" | —                  |

Kafka consumer `group-id` (Notification service): `notification-service`.
