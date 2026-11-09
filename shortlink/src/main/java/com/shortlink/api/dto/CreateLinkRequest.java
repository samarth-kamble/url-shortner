package com.shortlink.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank(message = "longUrl is required")
        String longUrl,

        String customAlias,

        Instant expiresAt
) {
}
