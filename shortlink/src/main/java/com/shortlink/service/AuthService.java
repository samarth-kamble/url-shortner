package com.shortlink.service;

import com.shortlink.api.dto.AuthResponse;
import com.shortlink.api.dto.LoginRequest;
import com.shortlink.api.dto.RegisterRequest;
import com.shortlink.api.dto.UserProfileResponse;
import com.shortlink.api.exception.EmailAlreadyExistsException;
import com.shortlink.api.exception.ResourceNotFoundException;
import com.shortlink.domain.User;
import com.shortlink.domain.UserRepository;
import com.shortlink.idgen.SnowflakeIdGenerator;
import com.shortlink.security.JwtService;
import com.shortlink.security.RefreshTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service orchestrating user registration, authentication, token issuance, and profile retrieval.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            SnowflakeIdGenerator snowflakeIdGenerator,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Registers a new user, hashes password with BCrypt, assigns a 64-bit Snowflake ID, and issues tokens.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered: " + normalizedEmail);
        }

        Long userId = snowflakeIdGenerator.nextId();
        String hashedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .id(userId)
                .email(normalizedEmail)
                .passwordHash(hashedPassword)
                .fullName(request.fullName().trim())
                .role("ROLE_USER")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        user = userRepository.save(user);

        return issueTokens(user);
    }

    /**
     * Authenticates user with email and password and issues new tokens.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokens(user);
    }

    /**
     * Rotates refresh token and generates a new access token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        Long userId = refreshTokenService.validateAndGetUserId(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Invalidate old refresh token (token rotation defense)
        refreshTokenService.revokeRefreshToken(refreshToken);

        return issueTokens(user);
    }

    /**
     * Revokes the refresh token from Redis.
     */
    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    /**
     * Retrieves the profile of an authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toProfileResponse(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                toProfileResponse(user)
        );
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

