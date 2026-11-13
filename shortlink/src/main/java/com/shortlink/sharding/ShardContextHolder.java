package com.shortlink.sharding;

/**
 * ThreadLocal holder storing the active shard key for the current database operation.
 */
public final class ShardContextHolder {

    private static final ThreadLocal<String> CURRENT_SHARD = new ThreadLocal<>();

    private ShardContextHolder() {}

    public static void setShard(String shardKey) {
        if (shardKey == null || shardKey.isBlank()) {
            CURRENT_SHARD.remove();
        } else {
            CURRENT_SHARD.set(shardKey);
        }
    }

    public static String getShard() {
        return CURRENT_SHARD.get();
    }

    public static void clear() {
        CURRENT_SHARD.remove();
    }
}

