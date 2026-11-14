package com.shortlink.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages distributed refresh tokens stored in Redis for cluster-wide revocation.
 */
@Service
public class RefreshTokenService {

    private static final String REFRESH_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${shortlink.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenTtl = Duration.ofMillis(refreshTokenExpirationMs);
    }

    /**
     * Mints a cryptographically random refresh token and saves it in Redis with a TTL.
     */
    public String createRefreshToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String key = REFRESH_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId.toString(), refreshTokenTtl);
        return token;
    }

    /**
     * Validates that the refresh token exists in Redis and returns the associated userId.
     */
    public Optional<Long> validateAndGetUserId(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        String key = REFRESH_PREFIX + refreshToken;
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(val));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Revokes a refresh token instantly across the cluster.
     */
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(REFRESH_PREFIX + refreshToken);
        }
    }
}

