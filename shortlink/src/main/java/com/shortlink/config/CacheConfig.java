package com.shortlink.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shortlink.cache.CachedLink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures L1 in-memory Caffeine cache for ultra-low-latency local link lookups.
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, CachedLink> localLinkCache(
            @Value("${shortlink.cache.l1-max-size:10000}") long maxSize,
            @Value("${shortlink.cache.l1-ttl-minutes:10}") long ttlMinutes
    ){
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofMillis(ttlMinutes))
                .recordStats()
                .build();
    }
}
