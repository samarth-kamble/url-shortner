package com.shortlink.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persisted record of a short-link click event.
 */
@Entity
@Table(name = "click_events")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ClickEventEntity {

    @Id
    @Column(name = "event_id", length = 36, nullable = false, updatable = false)
    private String eventId;

    @Column(name = "short_code", length = 16, nullable = false, updatable = false)
    private String shortCode;

    @Column(name = "long_url", nullable = false, updatable = false)
    private String longUrl;

    @Column(name = "client_ip", length = 45, updatable = false)
    private String clientIp;

    @Column(name = "user_agent", updatable = false)
    private String userAgent;

    @Column(name = "referrer", updatable = false)
    private String referrer;

    @Column(name = "device_type", length = 32, nullable = false, updatable = false)
    private String deviceType;

    @Column(name = "browser", length = 64, nullable = false, updatable = false)
    private String browser;

    @Column(name = "clicked_at", nullable = false, updatable = false)
    private Instant clickedAt;
}

