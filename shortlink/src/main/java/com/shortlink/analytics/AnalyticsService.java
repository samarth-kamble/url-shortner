package com.shortlink.analytics;

import com.shortlink.api.exception.ResourceNotFoundException;
import com.shortlink.domain.Link;
import com.shortlink.domain.LinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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
        return getAnalytics(shortCode, "all");
    }

    @Transactional(readOnly = true)
    public LinkAnalyticsResponse getAnalytics(String shortCode, String windowParam) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found: " + shortCode));

        AnalyticsWindow window = AnalyticsWindow.fromString(windowParam);
        Instant now = Instant.now();
        Instant since = window.calculateSince(now);

        long totalClicks;
        long uniqueVisitors;
        Instant lastClickedAt = clickEventRepository.findLatestClickedAtByShortCode(shortCode);

        Map<String, Long> deviceBreakdown;
        Map<String, Long> browserBreakdown;
        Map<String, Long> topReferrers;
        Map<String, Long> countryBreakdown;
        Map<String, Long> cityBreakdown;

        if (since == null) {
            totalClicks = clickEventRepository.countByShortCode(shortCode);
            uniqueVisitors = clickEventRepository.countDistinctClientIpByShortCode(shortCode);
            deviceBreakdown = toMap(clickEventRepository.countByDeviceTypeGrouped(shortCode));
            browserBreakdown = toMap(clickEventRepository.countByBrowserGrouped(shortCode));
            topReferrers = toMap(clickEventRepository.countByReferrerGrouped(shortCode));
            countryBreakdown = toMap(clickEventRepository.countByCountryGrouped(shortCode));
            cityBreakdown = toMap(clickEventRepository.countByCityGrouped(shortCode));
        } else {
            totalClicks = clickEventRepository.countByShortCodeSince(shortCode, since);
            uniqueVisitors = clickEventRepository.countDistinctClientIpByShortCodeSince(shortCode, since);
            deviceBreakdown = toMap(clickEventRepository.countByDeviceTypeGroupedSince(shortCode, since));
            browserBreakdown = toMap(clickEventRepository.countByBrowserGroupedSince(shortCode, since));
            topReferrers = toMap(clickEventRepository.countByReferrerGroupedSince(shortCode, since));
            countryBreakdown = toMap(clickEventRepository.countByCountryGroupedSince(shortCode, since));
            cityBreakdown = toMap(clickEventRepository.countByCityGroupedSince(shortCode, since));
        }

        List<TimeSeriesPoint> timeSeries = getTimeSeries(shortCode, window, now);

        return new LinkAnalyticsResponse(
                link.getShortCode(),
                link.getLongUrl(),
                window.getKey(),
                totalClicks,
                uniqueVisitors,
                lastClickedAt,
                deviceBreakdown,
                browserBreakdown,
                topReferrers,
                countryBreakdown,
                cityBreakdown,
                timeSeries);
    }

    @Transactional(readOnly = true)
    public List<TimeSeriesPoint> getTimeSeries(String shortCode, AnalyticsWindow window, Instant now) {
        Instant since = window.calculateSince(now);
        List<Object[]> rawBuckets;

        if (window == AnalyticsWindow.LAST_24_HOURS) {
            rawBuckets = clickEventRepository.findHourlyTimeSeriesSince(shortCode, since);
            return zeroFill(rawBuckets, since, now, ChronoUnit.HOURS);
        } else if (since != null) {
            rawBuckets = clickEventRepository.findDailyTimeSeriesSince(shortCode, since);
            return zeroFill(rawBuckets, since, now, ChronoUnit.DAYS);
        } else {
            rawBuckets = clickEventRepository.findDailyTimeSeriesAllTime(shortCode);
            return toTimeSeriesList(rawBuckets);
        }
    }

    private List<TimeSeriesPoint> zeroFill(
            List<Object[]> rawBuckets,
            Instant since,
            Instant until,
            ChronoUnit unit) {

        Map<Instant, Long> countsByBucket = new LinkedHashMap<>();
        if (rawBuckets != null) {
            for (Object[] row : rawBuckets) {
                if (row.length >= 2 && row[0] != null && row[1] instanceof Number num) {
                    Instant bucket = toInstant(row[0]);
                    countsByBucket.put(bucket.truncatedTo(unit), num.longValue());
                }
            }
        }

        List<TimeSeriesPoint> continuous = new ArrayList<>();
        Instant cursor = since.truncatedTo(unit);
        Instant end = until.truncatedTo(unit);

        while (!cursor.isAfter(end)) {
            long count = countsByBucket.getOrDefault(cursor, 0L);
            continuous.add(new TimeSeriesPoint(cursor, count));
            cursor = cursor.plus(1, unit);
        }

        return continuous;
    }

    private List<TimeSeriesPoint> toTimeSeriesList(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<TimeSeriesPoint> points = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            if (row.length >= 2 && row[0] != null && row[1] instanceof Number num) {
                points.add(new TimeSeriesPoint(toInstant(row[0]), num.longValue()));
            }
        }
        return points;
    }

    private Instant toInstant(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        } else if (value instanceof Instant inst) {
            return inst;
        } else if (value instanceof java.util.Date d) {
            return d.toInstant();
        } else {
            return Instant.parse(value.toString().replace(" ", "T") + "Z");
        }
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