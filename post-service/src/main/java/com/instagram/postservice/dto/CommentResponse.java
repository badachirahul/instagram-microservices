package com.instagram.postservice.dto;

import com.instagram.postservice.domain.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID userId,
        String username,
        String text,
        Instant createdAt) {

    public static CommentResponse of(Comment c) {
        return new CommentResponse(c.getId(), c.getPostId(), c.getUserId(),
                c.getUsername(), c.getText(), c.getCreatedAt());
    }
}
