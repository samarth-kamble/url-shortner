package com.shortlink.api;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class LinkControllerIntegrationTest {

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
    private LinkRepository linkRepository;

    @Autowired
    private LinkCacheService linkCacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
    @DisplayName("POST /api/v1/links creates short link with auto-generated Base62 code")
    void testCreateLinkAutoCode() throws Exception {
        String json = """
                {
                    "longUrl": "https://example.com/target"
                }
                """;

        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", notNullValue()))
                .andExpect(jsonPath("$.shortUrl", notNullValue()))
                .andExpect(jsonPath("$.longUrl", is("https://example.com/target")))
                .andExpect(jsonPath("$.isCustom", is(false)))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/links with custom alias and GET /{shortCode} HTTP 302 redirect")
    void testCreateAndRedirect() throws Exception {
        String alias = "my-alias-01";
        String json = String.format("""
                {
                    "longUrl": "https://example.com/docs",
                    "customAlias": "%s"
                }
                """, alias);

        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", is(alias)))
                .andExpect(jsonPath("$.isCustom", is(true)));

        // Perform HTTP 302 redirect
        mockMvc.perform(get("/" + alias))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/docs"));
    }

    @Test
    @DisplayName("POST /api/v1/links duplicate custom alias returns 409 Conflict")
    void testDuplicateCustomAliasReturnsConflict() throws Exception {
        String alias = "duplicate-alias";
        String json = String.format("""
                {
                    "longUrl": "https://example.com/first",
                    "customAlias": "%s"
                }
                """, alias);

        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        // Attempt to create second link with same alias
        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already taken")));
    }

    @Test
    @DisplayName("POST /api/v1/links with localhost or private IP returns 400 Bad Request (SSRF protection)")
    void testSsrfProtection() throws Exception {
        String json = """
                {
                    "longUrl": "http://localhost:8080/internal"
                }
                """;

        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("SSRF protection")));
    }

    @Test
    @DisplayName("POST /api/v1/links with reserved alias returns 400 Bad Request")
    void testReservedAliasRejected() throws Exception {
        String json = """
                {
                    "longUrl": "https://example.com",
                    "customAlias": "actuator"
                }
                """;

        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("reserved system path")));
    }

    @Test
    @DisplayName("GET /api/v1/links/{shortCode} returns link details")
    void testGetLinkDetails() throws Exception {
        String alias = "details-alias";
        String json = String.format("""
                {
                    "longUrl": "https://example.com/details",
                    "customAlias": "%s"
                }
                """, alias);

        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/links/" + alias))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is(alias)))
                .andExpect(jsonPath("$.longUrl", is("https://example.com/details")))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/v1/links/{shortCode} soft-deletes and subsequent redirect returns 410 Gone")
    void testSoftDeleteAnd410Gone() throws Exception {
        String alias = "to-delete";
        String json = String.format("""
                {
                    "longUrl": "https://example.com/delete-me",
                    "customAlias": "%s"
                }
                """, alias);

        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        // Soft delete -> 204 No Content
        mockMvc.perform(delete("/api/v1/links/" + alias))
                .andExpect(status().isNoContent());

        // Redirect attempt -> 410 Gone
        mockMvc.perform(get("/" + alias))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status", is(410)))
                .andExpect(jsonPath("$.message", containsString("deactivated")));
    }
}
