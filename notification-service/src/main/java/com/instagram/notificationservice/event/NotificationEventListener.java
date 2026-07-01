package com.instagram.notificationservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes user-events and post-events, switches on the event's own "type" field
 * (no shared Java classes with producers — see docs/event-contract.md), and applies
 * the notify/skip rules verbatim.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"user-events", "post-events"}, groupId = "notification-service")
    public void onEvent(String payload) {
        JsonNode node;
        try {
            node = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("Skipping unparseable event: {}", payload, e);
            return;
        }

        String type = node.path("type").asText(null);
        if (type == null) {
            log.warn("Skipping event with no type: {}", payload);
            return;
        }

        switch (type) {
            case "post.liked" -> handlePostLiked(node);
            case "comment.added" -> handleCommentAdded(node);
            case "user.followed" -> handleUserFollowed(node);
            default -> log.debug("Ignoring unrecognized event type: {}", type);
        }
    }

    private void handlePostLiked(JsonNode node) {
        UUID postOwnerId = uuid(node, "postOwnerId");
        UUID likerId = uuid(node, "likerId");
        if (likerId.equals(postOwnerId)) {
            return;
        }
        String likerUsername = node.path("likerUsername").asText();
        notificationService.recordFromEvent(uuid(node, "eventId"), postOwnerId, "post.liked",
                likerUsername + " liked your post");
    }

    private void handleCommentAdded(JsonNode node) {
        UUID postOwnerId = uuid(node, "postOwnerId");
        UUID commenterId = uuid(node, "commenterId");
        if (commenterId.equals(postOwnerId)) {
            return;
        }
        String commenterUsername = node.path("commenterUsername").asText();
        notificationService.recordFromEvent(uuid(node, "eventId"), postOwnerId, "comment.added",
                commenterUsername + " commented on your post");
    }

    private void handleUserFollowed(JsonNode node) {
        UUID followeeId = uuid(node, "followeeId");
        String followerUsername = node.path("followerUsername").asText();
        notificationService.recordFromEvent(uuid(node, "eventId"), followeeId, "user.followed",
                followerUsername + " started following you");
    }

    private UUID uuid(JsonNode node, String field) {
        return UUID.fromString(node.path(field).asText());
    }
}
