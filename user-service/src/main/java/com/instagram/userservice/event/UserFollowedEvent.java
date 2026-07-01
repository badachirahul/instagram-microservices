package com.instagram.userservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserFollowedEvent(
        UUID eventId,
        String type,
        Instant occurredAt,
        UUID followerId,
        String followerUsername,
        UUID followeeId
) {
    public static UserFollowedEvent of(UUID followerId, String followerUsername, UUID followeeId) {
        return new UserFollowedEvent(UUID.randomUUID(), "user.followed", Instant.now(), followerId, followerUsername, followeeId);
    }
}
