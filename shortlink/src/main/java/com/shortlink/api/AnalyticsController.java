package com.shortlink.api;

import com.shortlink.analytics.AnalyticsService;
import com.shortlink.analytics.AnalyticsWindow;
import com.shortlink.analytics.LinkAnalyticsResponse;
import com.shortlink.analytics.TimeSeriesPoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Controller exposing real-time click statistics, time-series trends, and
 * telemetry breakdowns for short links.
 */
@RestController
@RequestMapping("/api/v1/links")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{shortCode}/analytics")

    public ResponseEntity<LinkAnalyticsResponse> getAnalytics(
            @PathVariable("shortCode") String shortCode,
            @RequestParam(name = "window", defaultValue = "all") String window) {
        LinkAnalyticsResponse response = analyticsService.getAnalytics(shortCode, window);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}/analytics/timeseries")
    public ResponseEntity<List<TimeSeriesPoint>> getTimeSeries(
            @PathVariable("shortCode") String shortCode,
            @RequestParam(name = "window", defaultValue = "24h") String window) {
        AnalyticsWindow analyticsWindow = AnalyticsWindow.fromString(window);
        List<TimeSeriesPoint> points = analyticsService.getTimeSeries(shortCode, analyticsWindow, Instant.now());
        return ResponseEntity.ok(points);
    }
}
