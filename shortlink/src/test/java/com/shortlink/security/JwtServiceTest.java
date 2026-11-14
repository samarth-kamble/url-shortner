package com.shortlink.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Standard 256-bit hex secret key
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        jwtService = new JwtService(secret, 60000); // 1 minute
    }

    @Test
    @DisplayName("Should successfully generate and validate a signed JWT access token")
    void shouldGenerateAndValidateToken() {
        Long userId = 123456789L;
        String email = "dev@shortlink.io";
        String role = "ROLE_USER";

        String token = jwtService.generateAccessToken(userId, email, role);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
        assertThat(jwtService.extractRole(token)).isEqualTo(role);
    }

    @Test
    @DisplayName("Should reject tampered or corrupted tokens")
    void shouldRejectTamperedToken() {
        String token = jwtService.generateAccessToken(123L, "test@test.com", "ROLE_USER");
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired tokens")
    void shouldRejectExpiredToken() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        JwtService shortLivedService = new JwtService(secret, -1000); // Expired 1 second ago

        String expiredToken = shortLivedService.generateAccessToken(123L, "expired@test.com", "ROLE_USER");

        assertThat(shortLivedService.validateToken(expiredToken)).isFalse();
    }
}

