package com.shortlink.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Supported sliding time windows for filtering link analytics.
 */
public enum AnalyticsWindow {
    LAST_24_HOURS("24h", Duration.ofHours(24), "hour"),
    LAST_7_DAYS("7d", Duration.ofDays(7), "day"),
    LAST_30_DAYS("30d", Duration.ofDays(30), "day"),
    ALL_TIME("all", null, "day");

    private final String key;
    private final Duration duration;
    private final String defaultInterval;

    AnalyticsWindow(String key, Duration duration, String defaultInterval) {
        this.key = key;
        this.duration = duration;
        this.defaultInterval = defaultInterval;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultInterval() {
        return defaultInterval;
    }

    public Instant calculateSince(Instant now) {
        if (duration == null) {
            return null; // All time
        }
        return now.minus(duration);
    }

    public static AnalyticsWindow fromString(String value) {
        if (value == null || value.isBlank()) {
            return ALL_TIME;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "24h", "day", "today" -> LAST_24_HOURS;
            case "7d", "week" -> LAST_7_DAYS;
            case "30d", "month" -> LAST_30_DAYS;
            case "all", "all_time", "lifetime" -> ALL_TIME;
            default -> ALL_TIME;
        };
    }
}

