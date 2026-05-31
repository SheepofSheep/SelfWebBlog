package org.example.selfwebblog.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CommentRateLimiterTest {

    @Test
    void blocksAfterConfiguredLimitWithinWindow() {
        CommentRateLimiter limiter = new CommentRateLimiter(3, Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire(10L, "127.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire(10L, "127.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire(10L, "127.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire(10L, "127.0.0.1")).isFalse();
    }

    @Test
    void tracksDifferentUsersSeparately() {
        CommentRateLimiter limiter = new CommentRateLimiter(1, Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire(10L, "127.0.0.1")).isTrue();
        assertThat(limiter.tryAcquire(10L, "127.0.0.1")).isFalse();
        assertThat(limiter.tryAcquire(11L, "127.0.0.1")).isTrue();
    }
}
