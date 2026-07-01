package com.instagram.userservice.dto;

import java.util.UUID;

public record UserResponse(UUID id, String username, String email, long followerCount, long followingCount) {
}
