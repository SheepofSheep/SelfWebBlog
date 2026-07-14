package org.example.selfwebblog.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CommentRateLimiter {

    private static final int DEFAULT_MAX_COMMENTS = 3;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);

    private final int maxComments;
    private final Cache<String, AtomicInteger> attempts;

    public CommentRateLimiter() {
        this(DEFAULT_MAX_COMMENTS, DEFAULT_WINDOW);
    }

    public CommentRateLimiter(int maxComments, Duration window) {
        this.maxComments = maxComments;
        this.attempts = CacheBuilder.newBuilder()
                .expireAfterWrite(window.toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    public boolean tryAcquire(Long userId, String ipAddress) {
        return tryAcquire("comment", userId, ipAddress);
    }

    public boolean tryAcquire(String action, Long userId, String ipAddress) {
        String identity = userId != null ? "user:" + userId : "ip:" + normalizeIp(ipAddress);
        String key = action + ":" + identity;
        try {
            AtomicInteger counter = attempts.get(key, AtomicInteger::new);
            return counter.incrementAndGet() <= maxComments;
        } catch (ExecutionException e) {
            return false;
        }
    }

    private String normalizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }
        return ipAddress.trim();
    }
}
