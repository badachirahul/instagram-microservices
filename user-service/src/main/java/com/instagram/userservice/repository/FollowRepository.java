package com.instagram.userservice.repository;

import com.instagram.userservice.entity.Follow;
import com.instagram.userservice.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    List<Follow> findByIdFollowerId(UUID followerId);

    List<Follow> findByIdFolloweeId(UUID followeeId);

    long countByIdFolloweeId(UUID followeeId);

    long countByIdFollowerId(UUID followerId);
}
