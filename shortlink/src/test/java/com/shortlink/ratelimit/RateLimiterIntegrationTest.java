package com.shortlink.ratelimit;

import com.shortlink.cache.LinkCacheService;
import com.shortlink.domain.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "shortlink.rate-limit.enabled=true",
        "shortlink.rate-limit.write.capacity=3",
        "shortlink.rate-limit.write.refill-tokens-per-second=1.0"
})
class RateLimiterIntegrationTest {

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
    @DisplayName("Token bucket allows up to capacity, rejects excess with 429, isolates IPs, and refills")
    void testTokenBucketWriteEnforcementAndRefill() throws Exception {
        String ipA = "198.51.100.10";
        String ipB = "203.0.113.42";
        String payload = """
                {
                    "longUrl": "https://github.com/samarth-kamble-7/shortlink"
                }
                """;

        // 1. Consume all 3 tokens for ipA
        mockMvc.perform(post("/api/v1/links")
                        .header("X-Forwarded-For", ipA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Limit", "3"))
                .andExpect(header().string("X-RateLimit-Remaining", "2"));

        mockMvc.perform(post("/api/v1/links")
                        .header("X-Forwarded-For", ipA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Remaining", "1"));

        mockMvc.perform(post("/api/v1/links")
                        .header("X-Forwarded-For", ipA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        // 2. 4th request exceeds capacity -> 429 Too Many Requests
        mockMvc.perform(post("/api/v1/links")
                        .header("X-Forwarded-For", ipA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "3"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", containsString("Rate limit exceeded")));

        // 3. Different IP (ipB) has its own isolated bucket -> Allowed
        mockMvc.perform(post("/api/v1/links")
                        .header("X-Forwarded-For", ipB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Remaining", "2"));

        // 4. Health & Ping bypass rate limiting even when ipA bucket is empty
        mockMvc.perform(get("/api/v1/ping").header("X-Forwarded-For", ipA))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-RateLimit-Limit"));

        // 5. Wait 1.1s for 1 token refill (rate = 1.0 token/sec) -> ipA succeeds again
        Thread.sleep(1100);
        mockMvc.perform(post("/api/v1/links")
                        .header("X-Forwarded-For", ipA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }
}

