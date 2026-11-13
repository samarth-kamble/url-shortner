package com.shortlink.api;

import com.shortlink.analytics.AnalyticsService;
import com.shortlink.analytics.LinkAnalyticsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing real-time click statistics and telemetry breakdowns for short links.
 */
@RestController
@RequestMapping("/api/v1/links")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<LinkAnalyticsResponse> getAnalytics(@PathVariable("shortCode") String shortCode) {
        LinkAnalyticsResponse response = analyticsService.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }
}

