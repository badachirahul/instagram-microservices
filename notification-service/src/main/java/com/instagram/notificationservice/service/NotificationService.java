package com.instagram.notificationservice.service;

import com.instagram.notificationservice.dto.NotificationResponse;
import com.instagram.notificationservice.entity.Notification;
import com.instagram.notificationservice.exception.ForbiddenException;
import com.instagram.notificationservice.exception.NotFoundException;
import com.instagram.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void recordFromEvent(UUID eventId, UUID recipientId, String type, String message) {
        if (notificationRepository.existsByEventId(eventId)) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .eventId(eventId)
                .recipientId(recipientId)
                .type(type)
                .message(message)
                .read(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        if (!notification.getRecipientId().equals(requesterId)) {
            throw new ForbiddenException("Not your notification");
        }
        notification.setRead(true);
    }
}
