package com.shortlink.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Distributed atomic Token Bucket rate limiter backed by Redis + Lua scripting.
 * Fails open if Redis is unavailable so cache outages never disrupt core redirects.
 */
@Slf4j
@Service
public class RateLimiterService {

    public enum Tier {
        WRITE("write"),
        READ("read");

        private final String keySegment;

        Tier(String keySegment) {
            this.keySegment = keySegment;
        }

        public String getKeySegment() {
            return keySegment;
        }
    }

    private static final String KEY_PREFIX = "shortlink:rl:";

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final DefaultRedisScript<List> rateLimitScript;

    public RateLimiterService(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setLocation(new ClassPathResource("scripts/rate_limiter.lua"));
        this.rateLimitScript.setResultType(List.class);
    }

    /**
     * Atomically evaluates and consumes 1 token from the client's bucket for the given tier.
     */
    public RateLimitResult checkLimit(String clientIp, Tier tier) {
        RateLimitProperties.TierConfig config = tier == Tier.WRITE
                ? properties.getWrite()
                : properties.getRead();

        if (!properties.isEnabled()) {
            return RateLimitResult.failOpen(config.getCapacity());
        }

        String key = KEY_PREFIX + tier.getKeySegment() + ":" + clientIp;
        long nowMs = System.currentTimeMillis();

        try {
            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(config.getCapacity()),
                    String.valueOf(config.getRefillTokensPerSecond()),
                    String.valueOf(nowMs),
                    "1"
            );

            if (result == null || result.size() < 3) {
                log.warn("Unexpected null/short response from Redis Lua rate limiter for key {}", key);
                return RateLimitResult.failOpen(config.getCapacity());
            }

            long allowedFlag = toLong(result.get(0));
            long remaining = toLong(result.get(1));
            long retryAfter = toLong(result.get(2));

            return new RateLimitResult(allowedFlag == 1L, config.getCapacity(), remaining, retryAfter);
        } catch (Exception ex) {
            log.warn("Redis rate limiter unavailable for IP '{}', failing open: {}", clientIp, ex.getMessage());
            return RateLimitResult.failOpen(config.getCapacity());
        }
    }

    private long toLong(Object val) {
        if (val instanceof Number num) {
            return num.longValue();
        }
        return Long.parseLong(String.valueOf(val));
    }
}

