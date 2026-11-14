package com.shortlink.api;

import com.jayway.jsonpath.JsonPath;
import com.shortlink.api.dto.CreateLinkRequest;
import com.shortlink.api.dto.LoginRequest;
import com.shortlink.api.dto.RefreshTokenRequest;
import com.shortlink.api.dto.RegisterRequest;
import com.shortlink.domain.LinkRepository;
import com.shortlink.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class AuthIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private LinkRepository linkRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAll();
        userRepository.deleteAll();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Should register a new user with Snowflake ID and return JWT tokens")
    void shouldRegisterUserSuccessfully() throws Exception {
        String payload = """
                {
                    "email": "sarah.connor@resistance.org",
                    "password": "SkynetNeverWins2026!",
                    "fullName": "Sarah Connor"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is("sarah.connor@resistance.org")))
                .andExpect(jsonPath("$.user.fullName", is("Sarah Connor")))
                .andExpect(jsonPath("$.user.role", is("ROLE_USER")))
                .andExpect(jsonPath("$.user.id", notNullValue()));

        assertThat(userRepository.findByEmail("sarah.connor@resistance.org")).isPresent();
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to register duplicate email")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        String payload = """
                {
                    "email": "duplicate@shortlink.io",
                    "password": "Password12345!",
                    "fullName": "First Register"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("Should authenticate valid user and reject invalid password with 401")
    void shouldAuthenticateValidUserAndRejectInvalidPassword() throws Exception {
        String registerPayload = """
                {
                    "email": "john.wick@continental.com",
                    "password": "ContinentalGoldCoins123!",
                    "fullName": "John Wick"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated());

        // Correct password -> 200 OK
        String validLogin = """
                {
                    "email": "john.wick@continental.com",
                    "password": "ContinentalGoldCoins123!"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));

        // Wrong password -> 401 Unauthorized
        String invalidLogin = """
                {
                    "email": "john.wick@continental.com",
                    "password": "WrongPassword!"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLogin))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should enforce authentication on /api/v1/links/me and populate user dashboard")
    void shouldEnforceAuthOnLinksMeAndReturnUserOwnedLinks() throws Exception {
        // 1. Unauthenticated request to /api/v1/links/me must return 401 Unauthorized
        mockMvc.perform(get("/api/v1/links/me"))
                .andExpect(status().isUnauthorized());

        // 2. Register user and obtain token
        String registerPayload = """
                {
                    "email": "creator@youtube.com",
                    "password": "CreatorSecretKey2026!",
                    "fullName": "Tech Creator"
                }
                """;
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String token = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.accessToken");

        // 3. Create a link while authenticated
        String linkPayload = """
                {
                    "longUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                }
                """;

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkPayload))
                .andExpect(status().isCreated());

        // 4. Access /api/v1/links/me with JWT -> returns user's links
        mockMvc.perform(get("/api/v1/links/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].longUrl", is("https://www.youtube.com/watch?v=dQw4w9WgXcQ")));
    }

    @Test
    @DisplayName("Should rotate refresh tokens and revoke them on logout")
    void shouldRotateRefreshTokenAndRevokeOnLogout() throws Exception {
        String registerPayload = """
                {
                    "email": "agent007@mi6.gov.uk",
                    "password": "LicenceToKill007!",
                    "fullName": "James Bond"
                }
                """;
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshToken = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.refreshToken");

        // Refresh token -> returns new token pair
        String refreshPayload = String.format("{\"refreshToken\":\"%s\"}", refreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn();

        String newRefreshToken = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.refreshToken");

        // Old refresh token must be invalidated (one-time use rotation defense)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isUnauthorized());

        // Logout with new refresh token
        String logoutPayload = String.format("{\"refreshToken\":\"%s\"}", newRefreshToken);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutPayload))
                .andExpect(status().isNoContent());

        // Attempting to refresh after logout must fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutPayload))
                .andExpect(status().isUnauthorized());
    }
}
