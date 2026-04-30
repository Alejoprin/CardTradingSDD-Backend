package com.cardtrading.auth.service;

import com.cardtrading.auth.dto.*;
import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import com.cardtrading.event.model.UserRegisteredEvent;
import com.cardtrading.event.producer.EventPublisher;
import com.cardtrading.shared.exception.BusinessRuleException;
import com.cardtrading.shared.exception.UnauthorizedException;
import com.cardtrading.shared.security.JwtService;
import com.cardtrading.shared.security.LoginRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String RESET_TOKEN_PREFIX = "password:reset:";
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimitService loginRateLimitService;
    private final EventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);

        eventPublisher.publish("trading.user.registered", user.getId().toString(),
                UserRegisteredEvent.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .timestamp(LocalDateTime.now())
                        .build());

        log.info("User registered: userId={} username={}", user.getId(), user.getUsername());

        return toUserResponse(user);
    }

    public record LoginResult(TokenResponse tokenResponse, String refreshToken) {}

    public LoginResult login(LoginRequest request) {
        loginRateLimitService.checkRateLimit(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> {
                    loginRateLimitService.recordFailedAttempt(request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimitService.recordFailedAttempt(request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.isBanned()) {
            throw new UnauthorizedException("Account has been suspended");
        }

        loginRateLimitService.resetAttempts(request.getEmail());

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        log.info("User logged in: userId={}", user.getId());

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();

        return new LoginResult(tokenResponse, refreshToken);
    }

    public TokenResponse refreshToken(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        String tokenType = jwtService.extractTokenType(token);
        if (!"refresh".equals(tokenType)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        String blacklistKey = BLACKLIST_PREFIX + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        UUID userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired"));

        if (user.isBanned()) {
            throw new UnauthorizedException("Account has been suspended");
        }

        String newAccessToken = jwtService.generateAccessToken(userId, email, user.getRole().name());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();
    }

    public void logout(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofDays(7));
        log.info("Refresh token blacklisted");
    }

    public void initiatePasswordReset(PasswordResetRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase())
                .ifPresent(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    String key = RESET_TOKEN_PREFIX + resetToken;
                    redisTemplate.opsForValue().set(key, user.getId().toString(), RESET_TOKEN_TTL);
                    log.info("Password reset token generated for userId={}", user.getId());
                    // Email sending is handled by the notification consumer
                });
        // Always return success to prevent email enumeration
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        String key = RESET_TOKEN_PREFIX + request.getToken();
        String userIdStr = redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            throw new IllegalArgumentException("Reset token is invalid or has expired");
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Reset token is invalid or has expired"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisTemplate.delete(key);
        log.info("Password reset completed for userId={}", userId);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
