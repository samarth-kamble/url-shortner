package com.shortlink.api.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String role,
        Instant createdAt
) {
}

