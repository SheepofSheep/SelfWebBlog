package org.example.selfwebblog.identity.captcha;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaServiceTest {

    @Test
    void challengeCanOnlyBeConsumedOnce() {
        MutableTicker ticker = new MutableTicker();
        CaptchaService service = new CaptchaService(ticker, Duration.ofMinutes(5), () -> "A7K9");
        CaptchaResponse challenge = service.create("guestbook");

        assertTrue(challenge.imageDataUrl().startsWith("data:image/png;base64,"));
        assertTrue(service.verify(challenge.challengeId(), "a7k9", "guestbook"));
        assertFalse(service.verify(challenge.challengeId(), "A7K9", "guestbook"));
    }

    @Test
    void wrongPurposeWrongAnswerAndExpiredChallengesFail() {
        MutableTicker ticker = new MutableTicker();
        CaptchaService service = new CaptchaService(ticker, Duration.ofMinutes(5), () -> "K2M8");

        CaptchaResponse wrong = service.create("guestbook");
        assertFalse(service.verify(wrong.challengeId(), "wrong", "guestbook"));

        CaptchaResponse purpose = service.create("guestbook");
        assertFalse(service.verify(purpose.challengeId(), "K2M8", "register"));

        CaptchaResponse expired = service.create("guestbook");
        ticker.advance(Duration.ofMinutes(6));
        assertFalse(service.verify(expired.challengeId(), "K2M8", "guestbook"));
    }

    private static final class MutableTicker extends Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
