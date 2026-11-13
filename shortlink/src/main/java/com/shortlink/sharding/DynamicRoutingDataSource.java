package com.shortlink.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Spring AbstractRoutingDataSource implementation that dynamically selects
 * the target PostgreSQL shard DataSource according to the ShardContextHolder.
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(DynamicRoutingDataSource.class);

    private final String defaultShardKey;

    public DynamicRoutingDataSource(String defaultShardKey) {
        this.defaultShardKey = defaultShardKey;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String shard = ShardContextHolder.getShard();
        String selected = (shard != null && !shard.isBlank()) ? shard : defaultShardKey;
        log.debug("DynamicRoutingDataSource selected shard key: '{}'", selected);
        return selected;
    }
}

