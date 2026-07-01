package com.instagram.userservice.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserEventPublisher {

    private static final String TOPIC = "user-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserFollowed(UUID followerId, String followerUsername, UUID followeeId) {
        UserFollowedEvent event = UserFollowedEvent.of(followerId, followerUsername, followeeId);
        kafkaTemplate.send(TOPIC, followeeId.toString(), event);
    }
}
