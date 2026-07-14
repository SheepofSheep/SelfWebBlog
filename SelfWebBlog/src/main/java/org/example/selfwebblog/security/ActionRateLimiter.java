package org.example.selfwebblog.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class ActionRateLimiter {
    private final Cache<String, Window> windows = CacheBuilder.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(2, TimeUnit.HOURS)
            .build();

    public Decision acquire(String action, String identity, int limit, Duration duration) {
        if (limit < 1 || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("限流策略无效");
        }
        long now = System.currentTimeMillis();
        long windowMillis = duration.toMillis();
        String key = action + ":" + identity;
        Window window = windows.asMap().computeIfAbsent(key, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.startedAt >= windowMillis) {
                window.startedAt = now;
                window.count = 0;
            }
            long retryAfter = Math.max(1, (windowMillis - (now - window.startedAt) + 999) / 1000);
            if (window.count >= limit) return new Decision(false, retryAfter);
            window.count++;
            return new Decision(true, 0);
        }
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private static final class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
