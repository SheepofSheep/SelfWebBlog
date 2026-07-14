package org.example.selfwebblog.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;
    private final Cache<String, long[]> attempts = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    public boolean tryAcquire(String clientIp) {
        return tryAcquire("login", clientIp);
    }

    public boolean tryAcquire(String action, String clientIp) {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;
        String key = action + ":" + clientIp;
        long[] slots = attempts.asMap().computeIfAbsent(key, ignored -> new long[MAX_ATTEMPTS]);

        synchronized (slots) {
            int active = 0;
            int freeIndex = -1;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] > cutoff) {
                    active++;
                } else if (freeIndex == -1) {
                    freeIndex = i;
                }
            }
            if (active >= MAX_ATTEMPTS) {
                return false;
            }
            slots[freeIndex] = now;
            return true;
        }
    }
}
