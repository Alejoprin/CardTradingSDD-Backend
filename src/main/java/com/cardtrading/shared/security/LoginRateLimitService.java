package com.cardtrading.shared.security;

import com.cardtrading.shared.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "ratelimit:login:";

    private final StringRedisTemplate redisTemplate;

    public void checkRateLimit(String email) {
        String key = KEY_PREFIX + email.toLowerCase();
        String value = redisTemplate.opsForValue().get(key);
        int attempts = value != null ? Integer.parseInt(value) : 0;

        if (attempts >= MAX_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(key);
            long minutesLeft = ttl != null && ttl > 0 ? (ttl / 60) + 1 : 15;
            throw new TooManyRequestsException(
                    "Too many login attempts. Try again in " + minutesLeft + " minutes.");
        }
    }

    public void recordFailedAttempt(String email) {
        String key = KEY_PREFIX + email.toLowerCase();
        Long newCount = redisTemplate.opsForValue().increment(key);
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(key, WINDOW);
        }
    }

    public void resetAttempts(String email) {
        String key = KEY_PREFIX + email.toLowerCase();
        redisTemplate.delete(key);
    }
}
