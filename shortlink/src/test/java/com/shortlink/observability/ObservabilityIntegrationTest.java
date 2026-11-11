package com.shortlink.observability;

import com.shortlink.cache.LinkCacheService;
import com.shortlink.domain.LinkRepository;
import com.shortlink.ratelimit.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class ObservabilityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private LinkCacheService linkCacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(rateLimitFilter)
                .build();
        linkRepository.deleteAll();
        linkCacheService.clearL1();
        if (stringRedisTemplate.getConnectionFactory() != null) {
            try (var conn = stringRedisTemplate.getConnectionFactory().getConnection()) {
                conn.serverCommands().flushDb();
            }
        }
    }

    @Test
    @DisplayName("Actuator metrics expose L1 Caffeine cache hits and Redis rate limiter counters")
    void testCacheAndRateLimitTelemetryExposed() throws Exception {
        String alias = "metrics-alias";
        String json = String.format("""
                {
                    "longUrl": "https://github.com/samarth-kamble-7/shortlink",
                    "customAlias": "%s"
                }
                """, alias);

        // 1. Create link (populates L1/L2 cache and increments write rate-limiter counter)
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // 2. Execute 3 redirects -> should record L1 Caffeine hits and read rate-limiter allowed counts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/" + alias))
                    .andExpect(status().isFound());
        }

        // 3. Verify metrics catalog lists both custom telemetry names
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem("cache.gets")))
                .andExpect(jsonPath("$.names", hasItem("shortlink.ratelimit.requests")));

        // 4. Verify L1 cache hit counter >= 3
        mockMvc.perform(get("/actuator/metrics/cache.gets")
                        .param("tag", "cache:shortlink.l1.cache")
                        .param("tag", "result:hit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(3.0)));

        // 5. Verify rate limiter allowed counter >= 4 (1 write + 3 reads)
        mockMvc.perform(get("/actuator/metrics/shortlink.ratelimit.requests")
                        .param("tag", "outcome:allowed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(4.0)));
    }
}

