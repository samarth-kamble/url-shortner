package com.shortlink.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Service managing shard routing execution and thread-local scoping.
 */
@Component
public class ShardRouter {

    private static final Logger log = LoggerFactory.getLogger(ShardRouter.class);

    private final ConsistentHashRing hashRing;

    public ShardRouter(ConsistentHashRing hashRing) {
        this.hashRing = hashRing;
    }

    public String resolveShard(String shortCode) {
        return hashRing.selectShard(shortCode);
    }

    public List<String> getAllShards() {
        return hashRing.getPhysicalShards();
    }

    /**
     * Executes the given task within the context of the shard mapped to the specified shortCode.
     */
    public <T> T executeOnShard(String shortCode, Supplier<T> task) {
        String shard = resolveShard(shortCode);
        String previous = ShardContextHolder.getShard();
        try {
            ShardContextHolder.setShard(shard);
            log.trace("Routing operation for shortCode '{}' to shard '{}'", shortCode, shard);
            return task.get();
        } finally {
            ShardContextHolder.setShard(previous);
        }
    }

    /**
     * Executes a runnable task within the context of the shard mapped to the specified shortCode.
     */
    public void executeOnShard(String shortCode, Runnable task) {
        executeOnShard(shortCode, () -> {
            task.run();
            return null;
        });
    }

    /**
     * Executes the given task on an explicitly specified shard key.
     */
    public <T> T executeExplicit(String shardKey, Supplier<T> task) {
        String previous = ShardContextHolder.getShard();
        try {
            ShardContextHolder.setShard(shardKey);
            return task.get();
        } finally {
            ShardContextHolder.setShard(previous);
        }
    }
}

