package com.cardtrading.integration;

import com.cardtrading.auth.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
            .withDatabaseName("cardtrading_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String extractRefreshTokenCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        if (setCookie == null) return null;
        return Arrays.stream(setCookie.split(";"))
                .map(String::trim)
                .filter(s -> s.startsWith("refresh_token="))
                .map(s -> s.substring("refresh_token=".length()))
                .findFirst()
                .orElse(null);
    }

    @Test
    @DisplayName("Full auth flow: register -> login -> access protected -> 401 without token")
    void fullAuthFlow() throws Exception {
        // 1. Register
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("integrationuser")
                .email("integration@example.com")
                .password("Password1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integrationuser"))
                .andExpect(jsonPath("$.role").value("USER"));

        // 2. Login - access token in body, refresh token in cookie
        LoginRequest loginRequest = LoginRequest.builder()
                .email("integration@example.com")
                .password("Password1")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), TokenResponse.class);

        // 3. Access protected endpoint with valid token
        mockMvc.perform(get("/api/v1/cards")
                        .header("Authorization", "Bearer " + tokenResponse.getAccessToken()))
                .andExpect(status().isNotFound());
        // Note: cards endpoint not implemented yet, but should not return 401

        // 4. Access protected endpoint WITHOUT token -> 401/403
        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Register duplicate email returns 422")
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("uniqueuser1")
                .email("duplicate@example.com")
                .password("Password1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = RegisterRequest.builder()
                .username("uniqueuser2")
                .email("duplicate@example.com")
                .password("Password1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    @DisplayName("Login with bad credentials returns 401")
    void shouldReject401OnBadCredentials() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("badcreduser")
                .email("badcred@example.com")
                .password("Password1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("badcred@example.com")
                .password("WrongPassword1")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Refresh token flow works via cookie")
    void shouldRefreshToken() throws Exception {
        // Register + Login
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("refreshuser")
                .email("refresh@example.com")
                .password("Password1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("refresh@example.com")
                .password("Password1")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = extractRefreshTokenCookie(loginResult);

        // Refresh using cookie
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Logout blacklists refresh token")
    void shouldBlacklistTokenOnLogout() throws Exception {
        // Register + Login
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("logoutuser")
                .email("logout@example.com")
                .password("Password1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email("logout@example.com")
                .password("Password1")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = extractRefreshTokenCookie(loginResult);

        // Logout via cookie
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"));

        // Try to refresh with blacklisted token -> 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rate limiting blocks after 5 failed login attempts")
    void shouldRateLimitAfter5FailedAttempts() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("ratelimituser")
                .email("ratelimit@example.com")
                .password("Password1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = LoginRequest.builder()
                .email("ratelimit@example.com")
                .password("WrongPassword1")
                .build();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should be rate limited
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isTooManyRequests());
    }
}
