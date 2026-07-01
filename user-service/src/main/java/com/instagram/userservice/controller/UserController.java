package com.instagram.userservice.controller;

import com.instagram.userservice.dto.UserResponse;
import com.instagram.userservice.security.JwtAuthenticationFilter;
import com.instagram.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @GetMapping("/{id}/followers")
    public List<UserResponse> getFollowers(@PathVariable UUID id) {
        return userService.getFollowers(id);
    }

    @GetMapping("/{id}/following")
    public List<UserResponse> getFollowing(@PathVariable UUID id) {
        return userService.getFollowing(id);
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(@PathVariable UUID id, HttpServletRequest request) {
        UUID followerId = currentUserId(request);
        userService.follow(followerId, id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable UUID id, HttpServletRequest request) {
        UUID followerId = currentUserId(request);
        userService.unfollow(followerId, id);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(HttpServletRequest request) {
        return (UUID) request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTR);
    }
}
