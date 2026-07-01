package com.instagram.postservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes post-events. Keyed by postId so all events for one post keep their
 * order on the same partition.
 */
@Component
@RequiredArgsConstructor
public class PostEventPublisher {

    static final String TOPIC = "post-events";

    private final KafkaTemplate<String, Object> kafka;

    public void postCreated(PostEvents.PostCreated event) {
        kafka.send(TOPIC, event.postId(), event);
    }

    public void postLiked(PostEvents.PostLiked event) {
        kafka.send(TOPIC, event.postId(), event);
    }

    public void commentAdded(PostEvents.CommentAdded event) {
        kafka.send(TOPIC, event.postId(), event);
    }
}
