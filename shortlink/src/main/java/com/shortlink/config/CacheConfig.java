package com.shortlink.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shortlink.cache.CachedLink;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures L1 in-memory Caffeine cache and registers Micrometer cache
 * telemetry.
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, CachedLink> localLinkCache(
            MeterRegistry meterRegistry,
            @Value("${shortlink.cache.l1-max-size:10000}") long maxSize,
            @Value("${shortlink.cache.l1-ttl-minutes:10}") long ttlMinutes) {
        Cache<String, CachedLink> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(meterRegistry, cache, "shortlink.l1.cache");
        return cache;
    }
}
