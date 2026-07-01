package com.instagram.postservice.security;

import java.util.UUID;

/** The authenticated caller, extracted from the JWT (sub + username claims). */
public record AuthUser(UUID id, String username) {
}
