package com.shortlink.sharding;

import com.shortlink.api.dto.CreateLinkRequest;
import com.shortlink.api.dto.LinkResponse;
import com.shortlink.cache.LinkCacheService;
import com.shortlink.domain.LinkRepository;
import com.shortlink.service.LinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DatabaseShardingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> shard0 = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shortlink")
            .withUsername("shortlink")
            .withPassword("shortlink");

    @Container
    static PostgreSQLContainer<?> shard1 = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shortlink")
            .withUsername("shortlink")
            .withPassword("shortlink");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

    @DynamicPropertySource
    static void configureShards(DynamicPropertyRegistry registry) {
        registry.add("shortlink.sharding.enabled", () -> "true");
        registry.add("shortlink.sharding.virtual-nodes", () -> "160");
        registry.add("shortlink.sharding.default-shard", () -> "shard-0");

        registry.add("shortlink.sharding.shards.shard-0.url", shard0::getJdbcUrl);
        registry.add("shortlink.sharding.shards.shard-0.username", shard0::getUsername);
        registry.add("shortlink.sharding.shards.shard-0.password", shard0::getPassword);

        registry.add("shortlink.sharding.shards.shard-1.url", shard1::getJdbcUrl);
        registry.add("shortlink.sharding.shards.shard-1.username", shard1::getUsername);
        registry.add("shortlink.sharding.shards.shard-1.password", shard1::getPassword);

        // Point default datasource to shard0 for bootstrap compatibility
        registry.add("spring.datasource.url", shard0::getJdbcUrl);
        registry.add("spring.datasource.username", shard0::getUsername);
        registry.add("spring.datasource.password", shard0::getPassword);
    }

    @Autowired
    private LinkService linkService;

    @Autowired
    private ShardRouter shardRouter;

    @Autowired
    private LinkCacheService linkCacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        linkCacheService.clearL1();
        if (stringRedisTemplate.getConnectionFactory() != null) {
            try (var conn = stringRedisTemplate.getConnectionFactory().getConnection()) {
                conn.serverCommands().flushDb();
            }
        }
    }

    @Test
    @DisplayName("Stores links across multiple PostgreSQL shards according to consistent hash ring")
    void distributesLinksAcrossShards() throws Exception {
        boolean foundInShard0 = false;
        boolean foundInShard1 = false;

        // Create links until we have confirmed at least one mapped to shard-0 and one to shard-1
        for (int i = 0; i < 20; i++) {
            LinkResponse response = linkService.createLink(
                    new CreateLinkRequest("https://example.com/link-" + i, null, null));
            String shortCode = response.shortCode();
            String mappedShard = shardRouter.resolveShard(shortCode);

            if ("shard-0".equals(mappedShard)) {
                foundInShard0 = true;
            } else if ("shard-1".equals(mappedShard)) {
                foundInShard1 = true;
            }

            // Verify details retrieval routes to the mapped shard and succeeds
            LinkResponse details = linkService.getLinkDetails(shortCode);
            assertThat(details.longUrl()).isEqualTo("https://example.com/link-" + i);

            if (foundInShard0 && foundInShard1) {
                break;
            }
        }

        assertThat(foundInShard0).as("At least one link should route to shard-0").isTrue();
        assertThat(foundInShard1).as("At least one link should route to shard-1").isTrue();

        // Direct SQL assertions against physical shard containers
        long countShard0 = queryCount(shard0.getJdbcUrl(), shard0.getUsername(), shard0.getPassword());
        long countShard1 = queryCount(shard1.getJdbcUrl(), shard1.getUsername(), shard1.getPassword());

        assertThat(countShard0).as("Shard 0 has links persisted").isGreaterThan(0);
        assertThat(countShard1).as("Shard 1 has links persisted").isGreaterThan(0);
    }

    private long queryCount(String jdbcUrl, String user, String pass) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM links")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}

