package com.shortlink.sharding;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configures the Consistent Hash Ring, Shard DataSources,
 * DynamicRoutingDataSource,
 * and executes Flyway schema migrations across each individual shard on
 * startup.
 */
@Configuration
public class ShardingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ShardingConfiguration.class);

    @Bean
    public ConsistentHashRing consistentHashRing(
            Environment environment,
            @Value("${shortlink.sharding.virtual-nodes:160}") int virtualNodes,
            @Value("${shortlink.sharding.default-shard:shard-0}") String defaultShard) {

        Map<String, String> shardUrls = extractShardUrls(environment);
        if (shardUrls.isEmpty()) {
            shardUrls.put(defaultShard, "default");
        }
        log.info("Initialized ConsistentHashRing with shards: {} and {} virtual nodes per shard",
                shardUrls.keySet(), virtualNodes);
        return new ConsistentHashRing(shardUrls.keySet(), virtualNodes);
    }

    @Bean
    @Primary
    public DataSource dataSource(
            Environment environment,
            @Value("${shortlink.sharding.default-shard:shard-0}") String defaultShard,
            @Value("${shortlink.sharding.enabled:false}") boolean shardingEnabled,
            @Value("${spring.flyway.locations:classpath:db/migration}") String flywayLocations) {

        if (!shardingEnabled) {
            log.info("Sharding is disabled, using default Spring Boot DataSource");
            String defaultUrl = environment.getProperty("spring.datasource.url");
            String defaultUser = environment.getProperty("spring.datasource.username");
            String defaultPass = environment.getProperty("spring.datasource.password");

            HikariConfig fallbackConfig = new HikariConfig();
            fallbackConfig.setJdbcUrl(defaultUrl);
            fallbackConfig.setUsername(defaultUser);
            fallbackConfig.setPassword(defaultPass);
            fallbackConfig.setDriverClassName("org.postgresql.Driver");
            fallbackConfig.setPoolName("HikariPool-default");
            fallbackConfig.setMaximumPoolSize(10);
            fallbackConfig.setMinimumIdle(2);
            HikariDataSource defaultDs = new HikariDataSource(fallbackConfig);
            runFlyway(defaultDs, "default", flywayLocations);
            return defaultDs;
        }

        Map<String, String> shardUrls = extractShardUrls(environment);
        Map<Object, Object> targetDataSources = new HashMap<>();
        DataSource defaultTargetDataSource = null;

        for (Map.Entry<String, String> entry : shardUrls.entrySet()) {
            String shardKey = entry.getKey();
            String prefix = "shortlink.sharding.shards." + shardKey + ".";

            String url = environment.getProperty(prefix + "url");
            String username = environment.getProperty(prefix + "username",
                    environment.getProperty("spring.datasource.username", "shortlink"));
            String password = environment.getProperty(prefix + "password",
                    environment.getProperty("spring.datasource.password", "shortlink"));

            if (url == null || url.isBlank()) {
                continue;
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");
            config.setPoolName("HikariPool-" + shardKey);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);

            HikariDataSource ds = new HikariDataSource(config);
            targetDataSources.put(shardKey, ds);

            if (shardKey.equals(defaultShard)) {
                defaultTargetDataSource = ds;
            }

            // Run Flyway migrations on every shard database
            runFlyway(ds, shardKey, flywayLocations);
        }

        if (targetDataSources.isEmpty() || !shardingEnabled) {
            log.info("Sharding disabled or no shards found, using standalone default DataSource");
            String fallbackUrl = environment.getProperty("spring.datasource.url",
                    "jdbc:postgresql://localhost:5432/shortlink");
            HikariConfig fallbackConfig = new HikariConfig();
            fallbackConfig.setJdbcUrl(fallbackUrl);
            fallbackConfig.setUsername(environment.getProperty("spring.datasource.username", "shortlink"));
            fallbackConfig.setPassword(environment.getProperty("spring.datasource.password", "shortlink"));
            fallbackConfig.setPoolName("HikariPool-standalone");
            HikariDataSource fallbackDs = new HikariDataSource(fallbackConfig);
            runFlyway(fallbackDs, "standalone", flywayLocations);
            return fallbackDs;
        }

        if (defaultTargetDataSource == null) {
            defaultTargetDataSource = (DataSource) targetDataSources.values().iterator().next();
        }

        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource(defaultShard);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(defaultTargetDataSource);
        routingDataSource.afterPropertiesSet();

        log.info("Configured DynamicRoutingDataSource with {} active shards. Default shard: {}",
                targetDataSources.keySet(), defaultShard);
        return routingDataSource;
    }

    private void runFlyway(DataSource ds, String shardName, String locations) {
        try {
            log.info("Running Flyway migrations on shard '{}'...", shardName);
            String[] locationArray = locations.split(",");
            for (int i = 0; i < locationArray.length; i++) {
                locationArray[i] = locationArray[i].trim();
            }
            Flyway flyway = Flyway.configure()
                    .dataSource(ds)
                    .locations(locationArray)
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            log.info("Flyway migrations successfully applied on shard '{}'", shardName);
        } catch (Exception ex) {
            log.warn(
                    "Could not execute Flyway migrations on shard '{}': {}. (Shard may be offline or standalone mode active)",
                    shardName, ex.getMessage());
        }
    }

    private Map<String, String> extractShardUrls(Environment environment) {
        Map<String, String> shards = new LinkedHashMap<>();
        int index = 0;
        while (true) {
            String key = "shard-" + index;
            String prefix = "shortlink.sharding.shards." + key + ".";
            String url = environment.getProperty(prefix + "url");
            if (url != null && !url.isBlank()) {
                shards.put(key, url);
                index++;
            } else {
                break;
            }
        }
        return shards;
    }
}