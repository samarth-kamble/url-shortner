package com.shortlink.sharding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistentHashRingTest {

    @Test
    @DisplayName("Selects shard deterministically for the same short code")
    void deterministicSelection() {
        ConsistentHashRing ring = new ConsistentHashRing(List.of("shard-0", "shard-1"), 160);

        String shardA = ring.selectShard("testCode123");
        String shardB = ring.selectShard("testCode123");

        assertThat(shardA).isEqualTo(shardB);
        assertThat(shardA).isIn("shard-0", "shard-1");
    }

    @Test
    @DisplayName("Distributes keys uniformly across shards with minimal standard deviation")
    void uniformDistribution() {
        ConsistentHashRing ring = new ConsistentHashRing(List.of("shard-0", "shard-1"), 160);

        Map<String, Integer> counts = new HashMap<>();
        counts.put("shard-0", 0);
        counts.put("shard-1", 0);

        int totalKeys = 10_000;
        for (int i = 0; i < totalKeys; i++) {
            String key = UUID.randomUUID().toString();
            String shard = ring.selectShard(key);
            counts.put(shard, counts.get(shard) + 1);
        }

        // Each shard should receive roughly 50% (between 40% and 60% with 160 virtual nodes)
        assertThat(counts.get("shard-0")).isBetween(4_000, 6_000);
        assertThat(counts.get("shard-1")).isBetween(4_000, 6_000);
    }

    @Test
    @DisplayName("Handles null or blank shortCode by defaulting to first physical shard")
    void handlesNullOrBlank() {
        ConsistentHashRing ring = new ConsistentHashRing(List.of("shard-0", "shard-1"), 160);

        assertThat(ring.selectShard(null)).isEqualTo("shard-0");
        assertThat(ring.selectShard("")).isEqualTo("shard-0");
        assertThat(ring.selectShard("   ")).isEqualTo("shard-0");
    }
}

