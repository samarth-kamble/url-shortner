package com.shortlink.analytics;

import java.time.Instant;
import java.util.Map;

/**
 * Aggregated analytics view for a shortened link.
 */
public record LinkAnalyticsResponse(
        String shortCode,
        String longUrl,
        long totalClicks,
        long uniqueVisitors,
        Instant lastClickedAt,
        Map<String, Long> deviceBreakdown,
        Map<String, Long> browserBreakdown,
        Map<String, Long> topReferrers,
        Map<String, Long> countryBreakdown,
        Map<String, Long> cityBreakdown
) {
}
