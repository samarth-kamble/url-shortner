package com.shortlink.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for tiered token-bucket rate limiting.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "shortlink.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private TierConfig write = new TierConfig(20, 0.5);
    private TierConfig read = new TierConfig(1000, 500.0);

    @Getter
    @Setter
    public static class TierConfig {
        private long capacity;
        private double refillTokensPerSecond;

        public TierConfig() {
        }

        public TierConfig(long capacity, double refillTokensPerSecond) {
            this.capacity = capacity;
            this.refillTokensPerSecond = refillTokensPerSecond;
        }
    }
}

