package com.shortlink.analytics;

import com.shortlink.api.exception.ResourceNotFoundException;
import com.shortlink.domain.Link;
import com.shortlink.domain.LinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("Throws ResourceNotFoundException when link does not exist")
    void throwsWhenLinkNotFound() {
        when(linkRepository.findByShortCode("missing123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getAnalytics("missing123"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing123");
    }

    @Test
    @DisplayName("Aggregates link analytics correctly when clicks exist")
    void aggregatesAnalyticsCorrectly() {
        String shortCode = "abc12345";
        Link link = Link.builder()
                .id(123456L)
                .shortCode(shortCode)
                .longUrl("https://spring.io")
                .createdAt(Instant.now())
                .isActive(true)
                .build();

        Instant latestClick = Instant.parse("2026-11-13T10:00:00Z");

        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(link));
        when(clickEventRepository.countByShortCode(shortCode)).thenReturn(42L);
        when(clickEventRepository.countDistinctClientIpByShortCode(shortCode)).thenReturn(30L);
        when(clickEventRepository.findLatestClickedAtByShortCode(shortCode)).thenReturn(latestClick);

        List<Object[]> deviceRows = List.of(new Object[]{"DESKTOP", 30L}, new Object[]{"MOBILE", 12L});
        when(clickEventRepository.countByDeviceTypeGrouped(shortCode)).thenReturn(deviceRows);

        List<Object[]> browserRows = List.of(new Object[]{"Chrome", 35L}, new Object[]{"Safari", 7L});
        when(clickEventRepository.countByBrowserGrouped(shortCode)).thenReturn(browserRows);

        List<Object[]> referrerRows = List.of(new Object[]{"https://google.com", 25L}, new Object[]{"Direct", 17L});
        when(clickEventRepository.countByReferrerGrouped(shortCode)).thenReturn(referrerRows);

        List<Object[]> countryRows = List.of(new Object[]{"United States", 28L}, new Object[]{"India", 14L});
        when(clickEventRepository.countByCountryGrouped(shortCode)).thenReturn(countryRows);

        List<Object[]> cityRows = List.of(new Object[]{"San Francisco", 20L}, new Object[]{"Mumbai", 14L});
        when(clickEventRepository.countByCityGrouped(shortCode)).thenReturn(cityRows);

        List<Object[]> dailyRows = new java.util.ArrayList<>();
        dailyRows.add(new Object[]{Timestamp.from(Instant.parse("2026-11-13T00:00:00Z")), 42L});
        when(clickEventRepository.findDailyTimeSeriesAllTime(shortCode)).thenReturn(dailyRows);

        LinkAnalyticsResponse response = analyticsService.getAnalytics(shortCode);

        assertThat(response.shortCode()).isEqualTo(shortCode);
        assertThat(response.longUrl()).isEqualTo("https://spring.io");
        assertThat(response.window()).isEqualTo("all");
        assertThat(response.totalClicks()).isEqualTo(42L);
        assertThat(response.uniqueVisitors()).isEqualTo(30L);
        assertThat(response.lastClickedAt()).isEqualTo(latestClick);
        assertThat(response.deviceBreakdown()).containsEntry("DESKTOP", 30L).containsEntry("MOBILE", 12L);
        assertThat(response.browserBreakdown()).containsEntry("Chrome", 35L).containsEntry("Safari", 7L);
        assertThat(response.topReferrers()).containsEntry("https://google.com", 25L).containsEntry("Direct", 17L);
        assertThat(response.countryBreakdown()).containsEntry("United States", 28L).containsEntry("India", 14L);
        assertThat(response.cityBreakdown()).containsEntry("San Francisco", 20L).containsEntry("Mumbai", 14L);
        assertThat(response.timeSeries()).hasSize(1);
        assertThat(response.timeSeries().getFirst().clicks()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Aggregates 24-hour window analytics with zero-filled hourly time-series")
    void aggregates24HourWindowWithZeroFilling() {
        String shortCode = "hour123";
        Link link = Link.builder()
                .id(999L)
                .shortCode(shortCode)
                .longUrl("https://spring.io/projects")
                .createdAt(Instant.now())
                .isActive(true)
                .build();

        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(link));
        when(clickEventRepository.countByShortCodeSince(eq(shortCode), any(Instant.class))).thenReturn(5L);
        when(clickEventRepository.countDistinctClientIpByShortCodeSince(eq(shortCode), any(Instant.class))).thenReturn(3L);

        Instant hour1 = Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS).truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        List<Object[]> hourlyRows = new java.util.ArrayList<>();
        hourlyRows.add(new Object[]{Timestamp.from(hour1), 5L});
        when(clickEventRepository.findHourlyTimeSeriesSince(eq(shortCode), any(Instant.class))).thenReturn(hourlyRows);

        LinkAnalyticsResponse response = analyticsService.getAnalytics(shortCode, "24h");

        assertThat(response.window()).isEqualTo("24h");
        assertThat(response.totalClicks()).isEqualTo(5L);
        assertThat(response.uniqueVisitors()).isEqualTo(3L);
        // 24 hours plus current hour = 25 hourly buckets
        assertThat(response.timeSeries()).hasSizeGreaterThanOrEqualTo(24);
        // Ensure bucket with clicks has 5, and others are zero-filled
        long clicksInBucket = response.timeSeries().stream()
                .filter(pt -> pt.timestamp().equals(hour1))
                .mapToLong(TimeSeriesPoint::clicks)
                .findFirst()
                .orElse(0L);
        assertThat(clicksInBucket).isEqualTo(5L);
    }
}

