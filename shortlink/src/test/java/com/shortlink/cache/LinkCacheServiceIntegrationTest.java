package com.shortlink.cache;

import com.shortlink.domain.Link;
import com.shortlink.domain.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class LinkCacheServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private LinkCacheService linkCacheService;

    @MockitoSpyBean
    private LinkRepository linkRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAll();
        linkCacheService.clearL1();
        if (stringRedisTemplate.getConnectionFactory() != null) {
            try (var conn = stringRedisTemplate.getConnectionFactory().getConnection()) {
                conn.serverCommands().flushDb();
            }
        }
        clearInvocations(linkRepository);
    }

    @Test
    @DisplayName("Multi-tier L1 Caffeine -> L2 Redis -> DB cache-aside flow minimizes database hits")
    void testMultiTierCacheHitFlow() {
        String code = "cache-test-01";
        Link entity = Link.builder()
                .id(100001L)
                .shortCode(code)
                .longUrl("https://github.com/samarth-kamble-7/shortlink")
                .isCustom(true)
                .isActive(true)
                .createdAt(Instant.now())
                .build();
        linkRepository.saveAndFlush(entity);
        clearInvocations(linkRepository);

        // 1. Cold Read -> DB Miss in L1 & L2 -> Hits Postgres once & populates L2 + L1
        CachedLink first = linkCacheService.getLink(code);
        assertThat(first.longUrl()).isEqualTo("https://github.com/samarth-kamble-7/shortlink");
        assertThat(first.notFound()).isFalse();
        verify(linkRepository, times(1)).findByShortCode(code);

        // Verify entry exists in Redis L2
        String redisVal = stringRedisTemplate.opsForValue().get("shortlink:link:" + code);
        assertThat(redisVal).isNotNull().contains("https://github.com/samarth-kamble-7/shortlink");

        // 2. Second Read -> Hits L1 Caffeine -> DB NOT touched
        CachedLink second = linkCacheService.getLink(code);
        assertThat(second.longUrl()).isEqualTo("https://github.com/samarth-kamble-7/shortlink");
        verify(linkRepository, times(1)).findByShortCode(code);

        // 3. Clear L1 -> Third Read hits L2 Redis and backfills L1 -> DB STILL NOT touched
        linkCacheService.clearL1();
        CachedLink third = linkCacheService.getLink(code);
        assertThat(third.longUrl()).isEqualTo("https://github.com/samarth-kamble-7/shortlink");
        verify(linkRepository, times(1)).findByShortCode(code);

        // 4. Evict from both L1 & L2 -> Fourth Read falls back to DB
        linkCacheService.evict(code);
        assertThat(stringRedisTemplate.opsForValue().get("shortlink:link:" + code)).isNull();
        CachedLink fourth = linkCacheService.getLink(code);
        assertThat(fourth.longUrl()).isEqualTo("https://github.com/samarth-kamble-7/shortlink");
        verify(linkRepository, times(2)).findByShortCode(code);
    }

    @Test
    @DisplayName("Negative caching protects PostgreSQL against cache penetration for non-existent codes")
    void testNegativeCachingBlocksRepeatedDbQueries() {
        String missingCode = "non-existent-999";

        // 1. First read queries DB -> stores NOT_FOUND tombstone in L1 & L2
        CachedLink first = linkCacheService.getLink(missingCode);
        assertThat(first.notFound()).isTrue();
        verify(linkRepository, times(1)).findByShortCode(missingCode);

        // 2. Second read hits L1 tombstone -> DB NOT queried
        CachedLink second = linkCacheService.getLink(missingCode);
        assertThat(second.notFound()).isTrue();
        verify(linkRepository, times(1)).findByShortCode(missingCode);

        // 3. Clear L1 -> Third read hits Redis L2 tombstone -> DB STILL NOT queried
        linkCacheService.clearL1();
        CachedLink third = linkCacheService.getLink(missingCode);
        assertThat(third.notFound()).isTrue();
        verify(linkRepository, times(1)).findByShortCode(missingCode);
    }
}

