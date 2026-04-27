package com.cardtrading.auth;

import com.cardtrading.auth.controller.AuthController;
import com.cardtrading.auth.dto.*;
import com.cardtrading.auth.service.AuthService;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.GlobalExceptionHandler;
import com.cardtrading.shared.exception.TooManyRequestsException;
import com.cardtrading.shared.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("201 - successful registration")
        void shouldReturn201OnSuccess() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password("Password1")
                    .build();

            UserResponse response = UserResponse.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .email("test@example.com")
                    .role("USER")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("400 - validation failure (short username)")
        void shouldReturn400OnValidationFailure() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("ab")
                    .email("bad-email")
                    .password("short")
                    .build();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.details").exists());
        }

        @Test
        @DisplayName("409 - duplicate email")
        void shouldReturn409OnDuplicateEmail() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .username("testuser")
                    .email("existing@example.com")
                    .password("Password1")
                    .build();

            when(authService.register(any())).thenThrow(new BusinessRuleException("Email already in use"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("Email already in use"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("200 - successful login")
        void shouldReturn200OnSuccess() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("Password1")
                    .build();

            TokenResponse response = TokenResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600));
        }

        @Test
        @DisplayName("401 - bad credentials")
        void shouldReturn401OnBadCredentials() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("WrongPassword1")
                    .build();

            when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid email or password"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("429 - rate limit exceeded")
        void shouldReturn429OnRateLimit() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("Password1")
                    .build();

            when(authService.login(any())).thenThrow(
                    new TooManyRequestsException("Too many login attempts. Try again in 14 minutes."));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.message").value("Too many login attempts. Try again in 14 minutes."));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("200 - successful token refresh")
        void shouldReturn200OnSuccess() throws Exception {
            RefreshRequest request = RefreshRequest.builder()
                    .refreshToken("valid-refresh-token")
                    .build();

            TokenResponse response = TokenResponse.builder()
                    .accessToken("new-access-token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .build();

            when(authService.refreshToken(any(RefreshRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("401 - invalid refresh token")
        void shouldReturn401OnInvalidToken() throws Exception {
            RefreshRequest request = RefreshRequest.builder()
                    .refreshToken("invalid-token")
                    .build();

            when(authService.refreshToken(any()))
                    .thenThrow(new UnauthorizedException("Refresh token is invalid or expired"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutEndpoint {

        @Test
        @DisplayName("200 - successful logout")
        void shouldReturn200OnSuccess() throws Exception {
            RefreshRequest request = RefreshRequest.builder()
                    .refreshToken("some-refresh-token")
                    .build();

            doNothing().when(authService).logout(any(RefreshRequest.class));

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully logged out"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/password/reset")
    class PasswordResetEndpoint {

        @Test
        @DisplayName("200 - always returns success (no email enumeration)")
        void shouldAlwaysReturn200() throws Exception {
            PasswordResetRequest request = PasswordResetRequest.builder()
                    .email("any@example.com")
                    .build();

            doNothing().when(authService).initiatePasswordReset(any());

            mockMvc.perform(post("/api/v1/auth/password/reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            "If an account with that email exists, a reset link has been sent."));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/password/reset/confirm")
    class PasswordResetConfirmEndpoint {

        @Test
        @DisplayName("200 - successful password reset")
        void shouldReturn200OnSuccess() throws Exception {
            PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                    .token(UUID.randomUUID().toString())
                    .newPassword("NewPassword1")
                    .build();

            doNothing().when(authService).confirmPasswordReset(any());

            mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password successfully reset."));
        }

        @Test
        @DisplayName("400 - invalid/expired reset token")
        void shouldReturn400OnInvalidToken() throws Exception {
            PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                    .token("invalid-token")
                    .newPassword("NewPassword1")
                    .build();

            doThrow(new IllegalArgumentException("Reset token is invalid or has expired"))
                    .when(authService).confirmPasswordReset(any());

            mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Reset token is invalid or has expired"));
        }
    }
}
