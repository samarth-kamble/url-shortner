package com.shortlink.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEventEntity, String> {

       long countByShortCode(String shortCode);

       @Query("SELECT COUNT(DISTINCT c.clientIp) FROM ClickEventEntity c WHERE c.shortCode = :shortCode AND c.clientIp IS NOT NULL")
       long countDistinctClientIpByShortCode(@Param("shortCode") String shortCode);

       @Query("SELECT MAX(c.clickedAt) FROM ClickEventEntity c WHERE c.shortCode = :shortCode")
       Instant findLatestClickedAtByShortCode(@Param("shortCode") String shortCode);

       @Query("SELECT c.deviceType, COUNT(c) FROM ClickEventEntity c WHERE c.shortCode = :shortCode GROUP BY c.deviceType")
       List<Object[]> countByDeviceTypeGrouped(@Param("shortCode") String shortCode);

       @Query("SELECT c.browser, COUNT(c) FROM ClickEventEntity c WHERE c.shortCode = :shortCode GROUP BY c.browser")
       List<Object[]> countByBrowserGrouped(@Param("shortCode") String shortCode);

       @Query("SELECT COALESCE(NULLIF(TRIM(c.referrer), ''), 'Direct'), COUNT(c) " +
                     "FROM ClickEventEntity c WHERE c.shortCode = :shortCode " +
                     "GROUP BY COALESCE(NULLIF(TRIM(c.referrer), ''), 'Direct') " +
                     "ORDER BY COUNT(c) DESC")
       List<Object[]> countByReferrerGrouped(@Param("shortCode") String shortCode);

       @Query("SELECT COALESCE(NULLIF(TRIM(c.country), ''), 'Unknown'), COUNT(c) " +
                     "FROM ClickEventEntity c WHERE c.shortCode = :shortCode " +
                     "GROUP BY COALESCE(NULLIF(TRIM(c.country), ''), 'Unknown') " +
                     "ORDER BY COUNT(c) DESC")
       List<Object[]> countByCountryGrouped(@Param("shortCode") String shortCode);

       @Query("SELECT COALESCE(NULLIF(TRIM(c.city), ''), 'Unknown'), COUNT(c) " +
                     "FROM ClickEventEntity c WHERE c.shortCode = :shortCode " +
                     "GROUP BY COALESCE(NULLIF(TRIM(c.city), ''), 'Unknown') " +
                     "ORDER BY COUNT(c) DESC")
       List<Object[]> countByCityGrouped(@Param("shortCode") String shortCode);

       // ================= Window-Filtered Aggregations =================

       @Query("SELECT COUNT(c) FROM ClickEventEntity c WHERE c.shortCode = :shortCode AND c.clickedAt >= :since")
       long countByShortCodeSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       @Query("SELECT COUNT(DISTINCT c.clientIp) FROM ClickEventEntity c WHERE c.shortCode = :shortCode AND c.clientIp IS NOT NULL AND c.clickedAt >= :since")
       long countDistinctClientIpByShortCodeSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       @Query("SELECT c.deviceType, COUNT(c) FROM ClickEventEntity c WHERE c.shortCode = :shortCode AND c.clickedAt >= :since GROUP BY c.deviceType")
       List<Object[]> countByDeviceTypeGroupedSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       @Query("SELECT c.browser, COUNT(c) FROM ClickEventEntity c WHERE c.shortCode = :shortCode AND c.clickedAt >= :since GROUP BY c.browser")
       List<Object[]> countByBrowserGroupedSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       @Query("SELECT COALESCE(NULLIF(TRIM(c.referrer), ''), 'Direct'), COUNT(c) " +
                     "FROM ClickEventEntity c WHERE c.shortCode = :shortCode AND c.clickedAt >= :since " +
                     "GROUP BY COALESCE(NULLIF(TRIM(c.referrer), ''), 'Direct') " +
                     "ORDER BY COUNT(c) DESC")
       List<Object[]> countByReferrerGroupedSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       @Query("SELECT COALESCE(NULLIF(TRIM(c.country), ''), 'Unknown'), COUNT(c) " +
                     "FROM ClickEventEntity c WHERE c.shortCode = :shortCode AND c.clickedAt >= :since " +
                     "GROUP BY COALESCE(NULLIF(TRIM(c.country), ''), 'Unknown') " +
                     "ORDER BY COUNT(c) DESC")
       List<Object[]> countByCountryGroupedSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       @Query("SELECT COALESCE(NULLIF(TRIM(c.city), ''), 'Unknown'), COUNT(c) " +
                     "FROM ClickEventEntity c WHERE c.shortCode = :shortCode AND c.clickedAt >= :since " +
                     "GROUP BY COALESCE(NULLIF(TRIM(c.city), ''), 'Unknown') " +
                     "ORDER BY COUNT(c) DESC")
       List<Object[]> countByCityGroupedSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       // ================= Time-Series Date Trunc Aggregations =================

       @Query(value = """
                     SELECT date_trunc('hour', clicked_at) AS bucket, COUNT(*) AS count
                     FROM click_events
                     WHERE short_code = :shortCode AND clicked_at >= :since
                     GROUP BY bucket
                     ORDER BY bucket ASC
                     """, nativeQuery = true)
       List<Object[]> findHourlyTimeSeriesSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       @Query(value = """
                     SELECT date_trunc('day', clicked_at) AS bucket, COUNT(*) AS count
                     FROM click_events
                     WHERE short_code = :shortCode AND clicked_at >= :since
                     GROUP BY bucket
                     ORDER BY bucket ASC
                     """, nativeQuery = true)
       List<Object[]> findDailyTimeSeriesSince(@Param("shortCode") String shortCode, @Param("since") Instant since);

       @Query(value = """
                     SELECT date_trunc('day', clicked_at) AS bucket, COUNT(*) AS count
                     FROM click_events
                     WHERE short_code = :shortCode
                     GROUP BY bucket
                     ORDER BY bucket ASC
                     """, nativeQuery = true)
       List<Object[]> findDailyTimeSeriesAllTime(@Param("shortCode") String shortCode);
}
