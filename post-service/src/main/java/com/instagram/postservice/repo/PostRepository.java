package com.instagram.postservice.repo;

import com.instagram.postservice.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
}
