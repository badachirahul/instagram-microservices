package com.instagram.notificationservice.dto;

import com.instagram.notificationservice.entity.Notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, String message, boolean read, OffsetDateTime createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
