package com.shortlink.analytics;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain event emitted asynchronously whenever a short link is clicked.
 * Partitioned by shortCode in Apache Kafka to preserve per-link event ordering.
 */
public record ClickEvent (
    String eventId,
    String shortCode,
    String longUrl,
    String clientIp,
    String userAgent,
    String referrer,
    Instant clickedAt
    ){
    public static ClickEvent create (
            String shortCode,
            String longUrl,
            String clientIp,
            String userAgent,
            String referrer
    ) {
        return new ClickEvent (
                UUID.randomUUID().toString(),
                shortCode,
                longUrl,
                clientIp,
                userAgent,
                referrer,
                Instant.now()
        );
    }
}
