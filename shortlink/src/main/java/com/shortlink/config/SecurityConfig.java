package com.shortlink.config;

import com.shortlink.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {
                        "type": "about:blank",
                        "title": "Unauthorized",
                        "status": 401,
                        "detail": "Full authentication is required to access this resource",
                        "instance": "%s"
                    }
                    """.formatted(request.getRequestURI()));
        };
    }

    @Bean
    public org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {
                        "type": "about:blank",
                        "title": "Forbidden",
                        "status": 403,
                        "detail": "You do not have permission to access this resource",
                        "instance": "%s"
                    }
                    """.formatted(request.getRequestURI()));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. High-Performance Redirects: 100% public & unauthenticated
                        .requestMatchers(HttpMethod.GET, "/{shortCode:[a-zA-Z0-9_-]+}").permitAll()
                        // 2. Health & Utility Endpoints
                        .requestMatchers("/api/v1/ping", "/actuator/**", "/error").permitAll()
                        // 3. Authentication endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 4. Public Link Endpoints (Anonymous creation supported)
                        .requestMatchers(HttpMethod.POST, "/api/v1/links").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/links/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/links/{shortCode}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/links/{shortCode}/analytics").permitAll()
                        // 5. Protected User routes
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
