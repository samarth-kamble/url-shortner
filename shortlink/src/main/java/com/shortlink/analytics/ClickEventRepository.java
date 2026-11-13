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
}

