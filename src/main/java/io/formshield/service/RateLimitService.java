package io.formshield.service;

import io.formshield.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Redis-backed rate limiter.
 * Enforces per-minute and per-day limits based on the user's plan.
 * Also enforces monthly submission limits.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;

    public void checkAndIncrement(User user) {
        checkMonthlyLimit(user);
        checkRateLimit(user, "min", Duration.ofMinutes(1), user.getPlan().getRequestsPerMinute());

        int dailyLimit = user.getPlan().getRequestsPerDay();
        if (dailyLimit > 0) { // -1 means unlimited
            checkRateLimit(user, "day", Duration.ofDays(1), dailyLimit);
        }
    }

    private void checkMonthlyLimit(User user) {
        // Reset counter if it's a new month
        LocalDate resetAt = user.getMonthResetAt();
        LocalDate now = LocalDate.now();
        if (now.getMonthValue() != resetAt.getMonthValue() || now.getYear() != resetAt.getYear()) {
            user.setSubmissionsThisMonth(0);
            user.setMonthResetAt(now.withDayOfMonth(1));
        }

        int limit = user.getPlan().getMonthlyLimit();
        if (user.getSubmissionsThisMonth() >= limit) {
            throw new RateLimitExceededException(
                    "Monthly submission limit of %d reached. Upgrade your plan for more.".formatted(limit),
                    -1
            );
        }
    }

    private void checkRateLimit(User user, String window, Duration ttl, int limit) {
        String key = "rl:user:%d:%s".formatted(user.getId(), window);
        Long current = redis.opsForValue().increment(key);

        if (current == null) {
            return;
        }

        if (current == 1) {
            redis.expire(key, ttl);
        }

        if (current > limit) {
            long retryAfter = redis.getExpire(key);
            throw new RateLimitExceededException(
                    "Rate limit exceeded: %d requests per %s".formatted(limit, window),
                    retryAfter > 0 ? retryAfter : ttl.getSeconds()
            );
        }
    }
}
