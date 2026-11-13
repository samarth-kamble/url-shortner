package com.shortlink.analytics;

import com.shortlink.api.exception.ResourceNotFoundException;
import com.shortlink.domain.Link;
import com.shortlink.domain.LinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final LinkRepository linkRepository;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(LinkRepository linkRepository, ClickEventRepository clickEventRepository) {
        this.linkRepository = linkRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public LinkAnalyticsResponse getAnalytics(String shortCode) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found: " + shortCode));

        long totalClicks = clickEventRepository.countByShortCode(shortCode);
        long uniqueVisitors = clickEventRepository.countDistinctClientIpByShortCode(shortCode);
        Instant lastClickedAt = clickEventRepository.findLatestClickedAtByShortCode(shortCode);

        Map<String, Long> deviceBreakdown = toMap(clickEventRepository.countByDeviceTypeGrouped(shortCode));
        Map<String, Long> browserBreakdown = toMap(clickEventRepository.countByBrowserGrouped(shortCode));
        Map<String, Long> topReferrers = toMap(clickEventRepository.countByReferrerGrouped(shortCode));
        Map<String, Long> countryBreakdown = toMap(clickEventRepository.countByCountryGrouped(shortCode));
        Map<String, Long> cityBreakdown = toMap(clickEventRepository.countByCityGrouped(shortCode));

        return new LinkAnalyticsResponse(
                link.getShortCode(),
                link.getLongUrl(),
                totalClicks,
                uniqueVisitors,
                lastClickedAt,
                deviceBreakdown,
                browserBreakdown,
                topReferrers,
                countryBreakdown,
                cityBreakdown);
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row.length >= 2 && row[0] != null && row[1] instanceof Number num) {
                    map.put(row[0].toString(), num.longValue());
                }
            }
        }
        return map;
    }
}