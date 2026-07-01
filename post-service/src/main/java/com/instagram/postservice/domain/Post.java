package com.instagram.postservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_username", nullable = false)
    private String authorUsername;

    @Column(name = "caption")
    private String caption;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Post(UUID authorId, String authorUsername, String caption, String imageUrl) {
        this.id = UUID.randomUUID();
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.createdAt = Instant.now();
    }
}
