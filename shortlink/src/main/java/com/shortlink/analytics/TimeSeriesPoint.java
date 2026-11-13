package com.shortlink.analytics;

import java.time.Instant;

/**
 * Single data point on a time-series click trend chart.
 */
public record TimeSeriesPoint(
        Instant timestamp,
        long clicks
) {
}

