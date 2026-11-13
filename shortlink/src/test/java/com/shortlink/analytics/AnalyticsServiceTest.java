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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        LinkAnalyticsResponse response = analyticsService.getAnalytics(shortCode);

        assertThat(response.shortCode()).isEqualTo(shortCode);
        assertThat(response.longUrl()).isEqualTo("https://spring.io");
        assertThat(response.totalClicks()).isEqualTo(42L);
        assertThat(response.uniqueVisitors()).isEqualTo(30L);
        assertThat(response.lastClickedAt()).isEqualTo(latestClick);
        assertThat(response.deviceBreakdown()).containsEntry("DESKTOP", 30L).containsEntry("MOBILE", 12L);
        assertThat(response.browserBreakdown()).containsEntry("Chrome", 35L).containsEntry("Safari", 7L);
        assertThat(response.topReferrers()).containsEntry("https://google.com", 25L).containsEntry("Direct", 17L);
    }
}

