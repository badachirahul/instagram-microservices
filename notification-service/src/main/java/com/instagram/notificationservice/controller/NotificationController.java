package com.instagram.notificationservice.controller;

import com.instagram.notificationservice.dto.NotificationResponse;
import com.instagram.notificationservice.security.AuthUser;
import com.instagram.notificationservice.security.CurrentUser;
import com.instagram.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(@CurrentUser AuthUser user) {
        return notificationService.getNotifications(user.id());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@CurrentUser AuthUser user) {
        return Map.of("count", notificationService.unreadCount(user.id()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, @CurrentUser AuthUser user) {
        notificationService.markRead(id, user.id());
        return ResponseEntity.noContent().build();
    }
}
