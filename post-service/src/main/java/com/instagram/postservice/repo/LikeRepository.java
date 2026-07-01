package com.instagram.postservice.repo;

import com.instagram.postservice.domain.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    long deleteByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);
}
