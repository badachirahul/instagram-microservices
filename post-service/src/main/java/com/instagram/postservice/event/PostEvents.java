package com.instagram.postservice.event;

import com.instagram.postservice.domain.Comment;
import com.instagram.postservice.domain.Post;

import java.time.Instant;
import java.util.UUID;

/**
 * The three events this service publishes to the {@code post-events} topic.
 * Shapes are frozen by docs/event-contract.md — all fields are strings so the
 * JSON is plain (uuids and timestamps rendered as ISO text). Producers fill
 * every field a consumer needs (event-carried state).
 */
public final class PostEvents {

    private PostEvents() {
    }

    private static String now() {
        return Instant.now().toString();
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    public record PostCreated(String eventId, String type, String occurredAt,
                              String postId, String authorId, String authorUsername,
                              String caption, String imageUrl) {
        public static PostCreated from(Post p) {
            return new PostCreated(id(), "post.created", now(),
                    p.getId().toString(), p.getAuthorId().toString(), p.getAuthorUsername(),
                    p.getCaption(), p.getImageUrl());
        }
    }

    public record PostLiked(String eventId, String type, String occurredAt,
                            String postId, String postOwnerId,
                            String likerId, String likerUsername) {
        public static PostLiked of(Post post, UUID likerId, String likerUsername) {
            return new PostLiked(id(), "post.liked", now(),
                    post.getId().toString(), post.getAuthorId().toString(),
                    likerId.toString(), likerUsername);
        }
    }

    public record CommentAdded(String eventId, String type, String occurredAt,
                               String postId, String postOwnerId,
                               String commenterId, String commenterUsername, String text) {
        public static CommentAdded of(Post post, Comment comment) {
            return new CommentAdded(id(), "comment.added", now(),
                    post.getId().toString(), post.getAuthorId().toString(),
                    comment.getUserId().toString(), comment.getUsername(), comment.getText());
        }
    }
}
