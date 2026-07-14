package org.example.selfwebblog.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ActionRateLimiterTest {
    @Test
    void isolatesActionsAndReturnsRetryDelay() {
        ActionRateLimiter limiter = new ActionRateLimiter();

        assertThat(limiter.acquire("LIKE", "user-1", 1, Duration.ofMinutes(1)).allowed()).isTrue();
        ActionRateLimiter.Decision blocked = limiter.acquire("LIKE", "user-1", 1, Duration.ofMinutes(1));

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isPositive();
        assertThat(limiter.acquire("VIEW", "user-1", 1, Duration.ofMinutes(1)).allowed()).isTrue();
    }
}
