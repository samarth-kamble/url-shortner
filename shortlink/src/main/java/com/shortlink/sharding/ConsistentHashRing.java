package com.shortlink.sharding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Consistent Hash Ring implementation using virtual nodes to achieve uniform distribution
 * of short links across multiple database shards.
 */
public class ConsistentHashRing {

    private final int virtualNodesPerShard;
    private final SortedMap<Long, String> ring = new TreeMap<>();
    private final List<String> physicalShards;

    public ConsistentHashRing(Collection<String> shards) {
        this(shards, 160);
    }

    public ConsistentHashRing(Collection<String> shards, int virtualNodesPerShard) {
        if (shards == null || shards.isEmpty()) {
            throw new IllegalArgumentException("At least one shard must be specified");
        }
        this.virtualNodesPerShard = Math.max(1, virtualNodesPerShard);
        this.physicalShards = List.copyOf(shards);

        for (String shard : physicalShards) {
            addShard(shard);
        }
    }

    private void addShard(String shard) {
        for (int i = 0; i < virtualNodesPerShard; i++) {
            long hash = hash(shard + "#vn#" + i);
            ring.put(hash, shard);
        }
    }

    /**
     * Resolves the target shard key for a given short code.
     *
     * @param shortCode link shortCode
     * @return shard key (e.g. "shard-0", "shard-1")
     */
    public String selectShard(String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            return physicalShards.getFirst();
        }
        long hash = hash(shortCode);
        if (!ring.containsKey(hash)) {
            SortedMap<Long, String> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }

    public List<String> getPhysicalShards() {
        return physicalShards;
    }

    public int getVirtualNodesPerShard() {
        return virtualNodesPerShard;
    }

    /**
     * Deterministic 64-bit hash using SHA-256 digest prefix.
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) {
                h = (h << 8) | (digest[i] & 0xFFL);
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

