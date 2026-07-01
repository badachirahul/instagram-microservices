package com.instagram.userservice.service;

import com.instagram.userservice.dto.UserResponse;
import com.instagram.userservice.entity.Follow;
import com.instagram.userservice.entity.FollowId;
import com.instagram.userservice.entity.User;
import com.instagram.userservice.event.UserEventPublisher;
import com.instagram.userservice.exception.BadRequestException;
import com.instagram.userservice.exception.ConflictException;
import com.instagram.userservice.exception.NotFoundException;
import com.instagram.userservice.repository.FollowRepository;
import com.instagram.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserEventPublisher userEventPublisher;

    public UserService(UserRepository userRepository, FollowRepository followRepository, UserEventPublisher userEventPublisher) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.userEventPublisher = userEventPublisher;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        User user = findUser(id);
        long followers = followRepository.countByIdFolloweeId(id);
        long following = followRepository.countByIdFollowerId(id);
        return toResponse(user, followers, following);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFollowers(UUID id) {
        findUser(id);
        return followRepository.findByIdFolloweeId(id).stream()
                .map(f -> toResponse(findUser(f.getId().getFollowerId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFollowing(UUID id) {
        findUser(id);
        return followRepository.findByIdFollowerId(id).stream()
                .map(f -> toResponse(findUser(f.getId().getFolloweeId())))
                .toList();
    }

    @Transactional
    public User follow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BadRequestException("Cannot follow yourself");
        }
        User follower = findUser(followerId);
        findUser(followeeId);

        FollowId id = new FollowId(followerId, followeeId);
        if (followRepository.existsById(id)) {
            throw new ConflictException("Already following this user");
        }
        followRepository.save(Follow.of(followerId, followeeId));
        userEventPublisher.publishUserFollowed(followerId, follower.getUsername(), followeeId);
        return follower;
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followeeId) {
        FollowId id = new FollowId(followerId, followeeId);
        if (!followRepository.existsById(id)) {
            throw new NotFoundException("Not following this user");
        }
        followRepository.deleteById(id);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    private UserResponse toResponse(User user) {
        long followers = followRepository.countByIdFolloweeId(user.getId());
        long following = followRepository.countByIdFollowerId(user.getId());
        return toResponse(user, followers, following);
    }

    private UserResponse toResponse(User user, long followers, long following) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), followers, following);
    }
}
