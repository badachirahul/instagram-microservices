package com.instagram.postservice.dto;

import com.instagram.postservice.domain.Post;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        String authorUsername,
        String caption,
        String imageUrl,
        Instant createdAt,
        long likeCount,
        long commentCount,
        boolean likedByMe) {

    public static PostResponse of(Post p, long likeCount, long commentCount, boolean likedByMe) {
        return new PostResponse(p.getId(), p.getAuthorId(), p.getAuthorUsername(),
                p.getCaption(), p.getImageUrl(), p.getCreatedAt(),
                likeCount, commentCount, likedByMe);
    }
}
