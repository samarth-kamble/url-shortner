package com.shortlink.analytics;

import com.shortlink.cache.LinkCacheService;
import com.shortlink.domain.LinkRepository;
import org.awaitility.Awaitility;
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
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class ClickAnalyticsIntegrationTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

        @Container
        @ServiceConnection(name = "redis")
        static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
                        .withExposedPorts(6379);

        @Container
        @ServiceConnection
        static KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private LinkRepository linkRepository;

        @Autowired
        private ClickEventRepository clickEventRepository;

        @Autowired
        private ClickEventBatchRepository clickEventBatchRepository;

        @Autowired
        private LinkCacheService linkCacheService;

        @Autowired
        private StringRedisTemplate stringRedisTemplate;

        @Autowired
        private JsonMapper jsonMapper;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
                clickEventRepository.deleteAll();
                linkRepository.deleteAll();
                linkCacheService.clearL1();
                if (stringRedisTemplate.getConnectionFactory() != null) {
                        try (var conn = stringRedisTemplate.getConnectionFactory().getConnection()) {
                                conn.serverCommands().flushDb();
                        }
                }
        }

    @Test
    @DisplayName("End-to-end: Redirect emits Kafka event -> Consumer saves to DB -> Analytics API aggregates")
    void testEndToEndAnalyticsFlow() throws Exception {
        // 1. Create a short link
        String createPayload = """
                {"longUrl": "https://kafka.apache.org/documentation/"}
                """;

        String createResponse = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jsonNode = jsonMapper.readTree(createResponse);
        String shortCode = jsonNode.get("shortCode").asText();

        // 2. Perform 2 redirects with different headers
        mockMvc.perform(get("/" + shortCode)
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/128.0.0.0 Safari/537.36")
                        .header("Referer", "https://google.com")
                        .header("X-Forwarded-For", "203.0.113.195"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/" + shortCode)
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5) AppleWebKit/605.1.15 Safari/604.1")
                        .header("Referer", "https://t.co/shortlink")
                        .header("X-Forwarded-For", "203.0.113.196"))
                .andExpect(status().isFound());

        // 3. Await Kafka batch consumer persisting records into PostgreSQL
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    long count = clickEventRepository.countByShortCode(shortCode);
                    assertThat(count).isEqualTo(2L);
                });

        // 4. Query Analytics API
        mockMvc.perform(get("/api/v1/links/" + shortCode + "/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is(shortCode)))
                .andExpect(jsonPath("$.longUrl", is("https://kafka.apache.org/documentation/")))
                .andExpect(jsonPath("$.totalClicks", is(2)))
                .andExpect(jsonPath("$.uniqueVisitors", is(2)))
                .andExpect(jsonPath("$.deviceBreakdown.DESKTOP", is(1)))
                .andExpect(jsonPath("$.deviceBreakdown.MOBILE", is(1)))
                .andExpect(jsonPath("$.browserBreakdown.Chrome", is(1)))
                .andExpect(jsonPath("$.browserBreakdown.Safari", is(1)))
                .andExpect(jsonPath("$.topReferrers['https://google.com']", is(1)))
                .andExpect(jsonPath("$.topReferrers['https://t.co/shortlink']", is(1)))
                .andExpect(jsonPath("$.countryBreakdown.Unknown", is(2)))
                .andExpect(jsonPath("$.cityBreakdown.Unknown", is(2)));
    }

        @Test
        @DisplayName("Batch repository idempotency: duplicate eventId ignored with ON CONFLICT DO NOTHING")
        void testBatchRepositoryIdempotency() {
                String eventId = "test-uuid-" + System.nanoTime();
                ClickEventEntity event1 = ClickEventEntity.builder()
                                .eventId(eventId)
                                .shortCode("idemp001")
                                .longUrl("https://example.com/idempotent")
                                .clientIp("127.0.0.1")
                                .userAgent("curl/8.7.1")
                                .referrer(null)
                                .deviceType("BOT")
                                .browser("curl")
                                .country("Local Network")
                                .countryCode("LOCAL")
                                .city("Local")
                                .clickedAt(Instant.now())
                                .build();

                ClickEventEntity eventDuplicate = ClickEventEntity.builder()
                                .eventId(eventId)
                                .shortCode("idemp001")
                                .longUrl("https://example.com/idempotent")
                                .clientIp("127.0.0.1")
                                .userAgent("curl/8.7.1")
                                .referrer(null)
                                .deviceType("BOT")
                                .browser("curl")
                                .country("Local Network")
                                .countryCode("LOCAL")
                                .city("Local")
                                .clickedAt(Instant.now())
                                .build();

                clickEventBatchRepository.saveAllIdempotent(List.of(event1));
                assertThat(clickEventRepository.countByShortCode("idemp001")).isEqualTo(1L);

                // Save duplicate - should NOT throw and should NOT increase count
                clickEventBatchRepository.saveAllIdempotent(List.of(eventDuplicate));
                assertThat(clickEventRepository.countByShortCode("idemp001")).isEqualTo(1L);
        }
}
