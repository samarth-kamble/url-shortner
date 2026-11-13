package com.shortlink.ratelimit;

/**
 * Outcome of an atomic rate limit check.
 */
public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds
) {
    public static RateLimitResult failOpen(long limit) {
        return new RateLimitResult(true, limit, limit, 0);
    }
}

