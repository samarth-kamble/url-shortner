package com.shortlink.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.shortlink.domain.Link;
import com.shortlink.domain.LinkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Multi-tier (L1 Caffeine + L2 Redis) cache-aside service for short link resolution.
 * Includes fail-open Redis degradation, striped locking to prevent cache stampedes,
 * TTL jitter to avoid cache avalanches, and negative caching to block cache penetration.
 */
@Slf4j
@Service
public class LinkCacheService {

    private static final String REDIS_KEY_PREFIX = "shortlink:link:";
    private static final int LOCK_STRIPE_COUNT = 1024;
    private static final int MAX_TTL_JITTER_SECONDS = 300;

    private final Cache<String, CachedLink> l1Cache;
    private final StringRedisTemplate redisTemplate;
    private final LinkRepository linkRepository;
    private final JsonMapper jsonMapper;
    private final long l2TtlHours;
    private final long negativeTtlSeconds;
    private final Object[] lockStripes;

    public LinkCacheService(
            Cache<String, CachedLink> l1Cache,
            StringRedisTemplate redisTemplate,
            LinkRepository linkRepository,
            JsonMapper jsonMapper,
            @Value("${shortlink.cache.l2-ttl-hours:24}") long l2TtlHours,
            @Value("${shortlink.cache.negative-ttl-seconds:60}") long negativeTtlSeconds) {
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        this.linkRepository = linkRepository;
        this.jsonMapper = jsonMapper;
        this.l2TtlHours = l2TtlHours;
        this.negativeTtlSeconds = negativeTtlSeconds;
        this.lockStripes = new Object[LOCK_STRIPE_COUNT];
        for (int i = 0; i < LOCK_STRIPE_COUNT; i++) {
            this.lockStripes[i] = new Object();
        }
    }

    /**
     * Resolves a link using L1 (Caffeine) -> L2 (Redis) -> Database cache-aside flow.
     */
    public CachedLink getLink(String shortCode) {
        // 1. Check L1 Local In-Memory Cache
        CachedLink l1Hit = l1Cache.getIfPresent(shortCode);
        if (l1Hit != null) {
            return l1Hit;
        }

        // 2. Check L2 Distributed Redis Cache
        CachedLink l2Hit = readFromRedis(shortCode);
        if (l2Hit != null) {
            l1Cache.put(shortCode, l2Hit);
            return l2Hit;
        }

        // 3. Cache Stampede Prevention: Striped synchronization per shortCode
        Object stripeLock = lockStripes[(shortCode.hashCode() & Integer.MAX_VALUE) % LOCK_STRIPE_COUNT];
        synchronized (stripeLock) {
            // Double-check L1 and L2 in case another thread populated cache while waiting
            l1Hit = l1Cache.getIfPresent(shortCode);
            if (l1Hit != null) {
                return l1Hit;
            }
            l2Hit = readFromRedis(shortCode);
            if (l2Hit != null) {
                l1Cache.put(shortCode, l2Hit);
                return l2Hit;
            }

            // 4. Cold Read from PostgreSQL
            Optional<Link> dbResult = linkRepository.findByShortCode(shortCode);
            CachedLink entry = dbResult
                    .map(link -> CachedLink.of(link.getLongUrl(), link.isActive(), link.getExpiresAt()))
                    .orElseGet(CachedLink::notFoundEntry);

            writeToRedis(shortCode, entry);
            l1Cache.put(shortCode, entry);
            return entry;
        }
    }

    /**
     * Proactively populates L1 and L2 caches upon link creation (write-through).
     */
    public void putLink(String shortCode, CachedLink entry) {
        l1Cache.put(shortCode, entry);
        writeToRedis(shortCode, entry);
    }

    /**
     * Immediately invalidates a link across both L1 and L2 caches (used on delete/update).
     */
    public void evict(String shortCode) {
        l1Cache.invalidate(shortCode);
        try {
            redisTemplate.delete(REDIS_KEY_PREFIX + shortCode);
        } catch (Exception ex) {
            log.warn("Failed to evict key '{}' from Redis L2 cache: {}", shortCode, ex.getMessage());
        }
    }

    public void clearL1() {
        l1Cache.invalidateAll();
    }

    public CacheStats getL1Stats() {
        return l1Cache.stats();
    }

    private CachedLink readFromRedis(String shortCode) {
        try {
            String raw = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + shortCode);
            if (raw == null) {
                return null;
            }
            return jsonMapper.readValue(raw, CachedLink.class);
        } catch (Exception ex) {
            log.warn("Redis L2 read failed for '{}', falling back gracefully: {}", shortCode, ex.getMessage());
            return null;
        }
    }

    private void writeToRedis(String shortCode, CachedLink entry) {
        try {
            String json = jsonMapper.writeValueAsString(entry);
            Duration ttl;
            if (entry.notFound() || !entry.active()) {
                ttl = Duration.ofSeconds(negativeTtlSeconds);
            } else {
                long jitter = ThreadLocalRandom.current().nextLong(MAX_TTL_JITTER_SECONDS + 1);
                ttl = Duration.ofHours(l2TtlHours).plusSeconds(jitter);
            }
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + shortCode, json, ttl);
        } catch (Exception ex) {
            log.warn("Redis L2 write failed for '{}', continuing without L2 cache: {}", shortCode, ex.getMessage());
        }
    }
}

