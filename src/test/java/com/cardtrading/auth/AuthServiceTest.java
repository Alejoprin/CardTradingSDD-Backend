package com.cardtrading.auth;

import com.cardtrading.auth.dto.*;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.auth.service.AuthService;
import com.cardtrading.event.producer.EventPublisher;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.UnauthorizedException;
import com.cardtrading.shared.security.JwtService;
import com.cardtrading.shared.security.LoginRateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private LoginRateLimitService loginRateLimitService;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterSuccessfully() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("newuser")
                    .email("new@example.com")
                    .password("Password1")
                    .build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(passwordEncoder.encode("Password1")).thenReturn("encodedPwd");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(UUID.randomUUID());
                u.setCreatedAt(LocalDateTime.now());
                return u;
            });

            UserResponse response = authService.register(request);

            assertThat(response.getUsername()).isEqualTo("newuser");
            assertThat(response.getEmail()).isEqualTo("new@example.com");
            assertThat(response.getRole()).isEqualTo("USER");
            verify(eventPublisher).publish(eq("trading.user.registered"), anyString(), any());
        }

        @Test
        @DisplayName("should throw when email already exists")
        void shouldThrowOnDuplicateEmail() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("newuser")
                    .email("existing@example.com")
                    .password("Password1")
                    .build();

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Email already in use");
        }

        @Test
        @DisplayName("should throw when username already exists")
        void shouldThrowOnDuplicateUsername() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("existinguser")
                    .email("new@example.com")
                    .password("Password1")
                    .build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Username already taken");
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("Password1")
                    .build();

            doNothing().when(loginRateLimitService).checkRateLimit("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password1", "encodedPassword")).thenReturn(true);
            when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");

            AuthService.LoginResult result = authService.login(request);

            assertThat(result.tokenResponse().getAccessToken()).isEqualTo("access-token");
            assertThat(result.tokenResponse().getTokenType()).isEqualTo("Bearer");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            verify(loginRateLimitService).resetAttempts("test@example.com");
        }

        @Test
        @DisplayName("should throw on bad password and record failed attempt")
        void shouldThrowOnBadPassword() {
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("WrongPassword1")
                    .build();

            doNothing().when(loginRateLimitService).checkRateLimit("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPassword1", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");

            verify(loginRateLimitService).recordFailedAttempt("test@example.com");
        }

        @Test
        @DisplayName("should throw when user is banned")
        void shouldThrowWhenBanned() {
            testUser.setBanned(true);
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("Password1")
                    .build();

            doNothing().when(loginRateLimitService).checkRateLimit("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("Password1", "encodedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Account has been suspended");
        }

        @Test
        @DisplayName("should throw on non-existent email and record failed attempt")
        void shouldThrowOnNonExistentEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("nonexistent@example.com")
                    .password("Password1")
                    .build();

            doNothing().when(loginRateLimitService).checkRateLimit("nonexistent@example.com");
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");

            verify(loginRateLimitService).recordFailedAttempt("nonexistent@example.com");
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshSuccessfully() {
            when(jwtService.isTokenValid("valid-refresh-token")).thenReturn(true);
            when(jwtService.extractTokenType("valid-refresh-token")).thenReturn("refresh");
            when(redisTemplate.hasKey("token:blacklist:valid-refresh-token")).thenReturn(false);
            when(jwtService.extractUserId("valid-refresh-token")).thenReturn(testUser.getId());
            when(jwtService.extractEmail("valid-refresh-token")).thenReturn("test@example.com");
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser.getId(), "test@example.com", "USER"))
                    .thenReturn("new-access-token");

            TokenResponse response = authService.refreshToken("valid-refresh-token");

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("should throw when refresh token is expired")
        void shouldThrowOnExpiredToken() {
            when(jwtService.isTokenValid("expired-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("expired-token"))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("initiatePasswordReset()")
    class InitiatePasswordReset {

        @Test
        @DisplayName("should store reset token in Redis for existing user")
        void shouldStoreResetToken() {
            PasswordResetRequest request = PasswordResetRequest.builder()
                    .email("test@example.com")
                    .build();

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            authService.initiatePasswordReset(request);

            verify(valueOperations).set(startsWith("password:reset:"), eq(testUser.getId().toString()), any());
        }

        @Test
        @DisplayName("should not throw for non-existent email (no enumeration)")
        void shouldNotThrowForNonExistentEmail() {
            PasswordResetRequest request = PasswordResetRequest.builder()
                    .email("nonexistent@example.com")
                    .build();

            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // Should not throw - prevents email enumeration
            authService.initiatePasswordReset(request);
        }
    }

    @Nested
    @DisplayName("confirmPasswordReset()")
    class ConfirmPasswordReset {

        @Test
        @DisplayName("should reset password with valid token")
        void shouldResetPasswordSuccessfully() {
            String resetToken = UUID.randomUUID().toString();
            PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                    .token(resetToken)
                    .newPassword("NewPassword1")
                    .build();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password:reset:" + resetToken)).thenReturn(testUser.getId().toString());
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("NewPassword1")).thenReturn("newEncodedPwd");
            when(redisTemplate.delete("password:reset:" + resetToken)).thenReturn(true);

            authService.confirmPasswordReset(request);

            verify(userRepository).save(testUser);
            assertThat(testUser.getPassword()).isEqualTo("newEncodedPwd");
        }

        @Test
        @DisplayName("should throw on expired/invalid token")
        void shouldThrowOnInvalidToken() {
            PasswordResetConfirmRequest request = PasswordResetConfirmRequest.builder()
                    .token("invalid-token")
                    .newPassword("NewPassword1")
                    .build();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password:reset:invalid-token")).thenReturn(null);

            assertThatThrownBy(() -> authService.confirmPasswordReset(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Reset token is invalid or has expired");
        }
    }
}
