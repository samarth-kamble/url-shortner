package com.shortlink.analytics;

import com.shortlink.cache.LinkCacheService;
import com.shortlink.domain.LinkRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
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
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class ClickEventProducerIntegrationTest {

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
    private LinkCacheService linkCacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        linkRepository.deleteAll();
        linkCacheService.clearL1();
        if (stringRedisTemplate.getConnectionFactory() != null) {
            try (var conn = stringRedisTemplate.getConnectionFactory().getConnection()) {
                conn.serverCommands().flushDb();
            }
        }
    }

    @Test
    @DisplayName("GET /{shortCode} emits asynchronous ClickEvent to Kafka topic partitioned by shortCode")
    void testRedirectPublishesClickEventToKafka() throws Exception {
        String alias = "kafka-test";
        String createJson = String.format("""
                {
                    "longUrl": "https://github.com/samarth-kamble-7/shortlink",
                    "customAlias": "%s"
                }
                """, alias);

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated());

        // Trigger HTTP 302 redirect with analytics headers
        mockMvc.perform(get("/" + alias)
                        .header("X-Forwarded-For", "203.0.113.55")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (iPhone)")
                        .header(HttpHeaders.REFERER, "https://twitter.com/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://github.com/samarth-kamble-7/shortlink"));

        // Consume directly from Kafka topic to verify event payload and partition key
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList("shortlink.clicks"));

            ConsumerRecord<String, String> received = null;
            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline && received == null) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
                for (ConsumerRecord<String, String> rec : records) {
                    if (alias.equals(rec.key())) {
                        received = rec;
                        break;
                    }
                }
            }

            assertThat(received).isNotNull();
            assertThat(received.key()).isEqualTo(alias);

            ClickEvent event = jsonMapper.readValue(received.value(), ClickEvent.class);
            assertThat(event.eventId()).isNotBlank();
            assertThat(event.shortCode()).isEqualTo(alias);
            assertThat(event.longUrl()).isEqualTo("https://github.com/samarth-kamble-7/shortlink");
            assertThat(event.clientIp()).isEqualTo("203.0.113.55");
            assertThat(event.userAgent()).isEqualTo("Mozilla/5.0 (iPhone)");
            assertThat(event.referrer()).isEqualTo("https://twitter.com/");
            assertThat(event.clickedAt()).isNotNull();
        }
    }
}

