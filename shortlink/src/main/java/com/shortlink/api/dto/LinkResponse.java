package com.shortlink.api.dto;

import java.time.Instant;

public record LinkResponse(
        String shortCode,
        String shortUrl,
        String longUrl,
        boolean isCustom,
        boolean isActive,
        Instant createdAt,
        Instant expiresAt
        ) {
}
