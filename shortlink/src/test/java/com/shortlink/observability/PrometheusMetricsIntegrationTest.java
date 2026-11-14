package com.shortlink.observability;

import com.shortlink.cache.LinkCacheService;
import com.shortlink.domain.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PrometheusMetricsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private LinkCacheService linkCacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
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
    @DisplayName("GET /actuator/prometheus exposes Prometheus formatted metrics with latency percentiles and cache telemetry")
    void testPrometheusEndpointExposition() throws Exception {
        String shortCode = "prom-test";
        String requestJson = String.format("""
                {
                    "longUrl": "https://prometheus.io/docs/introduction/overview/",
                    "customAlias": "%s"
                }
                """, shortCode);

        // 1. Create a link via HTTP POST
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/links"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        HttpResponse<String> createRes = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
        assertThat(createRes.statusCode()).isEqualTo(201);

        // 2. Perform 3 redirects via HTTP GET to record HTTP server requests and cache
        // metrics
        HttpRequest redirectReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/" + shortCode))
                .GET()
                .build();
        for (int i = 0; i < 3; i++) {
            HttpResponse<Void> redirectRes = httpClient.send(redirectReq, HttpResponse.BodyHandlers.discarding());
            assertThat(redirectRes.statusCode()).isEqualTo(302);
        }

        // 3. Query /actuator/prometheus endpoint
        HttpRequest promReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/prometheus"))
                .GET()
                .build();
        HttpResponse<String> promRes = httpClient.send(promReq, HttpResponse.BodyHandlers.ofString());
        assertThat(promRes.statusCode()).isEqualTo(200);

        String body = promRes.body();

        // 4. Validate presence of core metrics in Prometheus exposition format
        assertThat(body).isNotBlank();
        assertThat(body).contains("http_server_requests_seconds");
        assertThat(body).contains("cache_gets_total");
        assertThat(body).contains("shortlink_ratelimit_requests_total");
        assertThat(body).contains("jvm_memory_used_bytes");
        assertThat(body).contains("process_cpu_usage");
        assertThat(body).contains("system_cpu_usage");
    }
}