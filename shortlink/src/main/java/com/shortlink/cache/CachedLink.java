package com.shortlink.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

/**
 * Lightweight immutable representation of a cached short link entry.
 * Supports negative caching (notFound tombstone) to prevent cache penetration attacks.
 */
public record CachedLink(
        String longUrl,
        boolean active,
        Instant expiresAt,
        boolean notFound
) {
    public static CachedLink of(String longUrl, boolean active, Instant expiresAt) {
        return new CachedLink(longUrl, active, expiresAt, false);
    }

    public static CachedLink notFoundEntry() {
        return new CachedLink(null, false, null, true);
    }

    @JsonIgnore
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
