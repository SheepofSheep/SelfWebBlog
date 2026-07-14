package org.example.selfwebblog.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuestTrustServiceTest {

    private final GuestTrustService service = new GuestTrustService();

    @Test
    void firstGuestMessageIsPending() {
        assertEquals("PENDING", service.statusFor(false, "你好", false));
    }

    @Test
    void approvedGuestCanPublishPlainTextButLinksAndRateAnomaliesRemainPending() {
        assertEquals("PUBLISHED", service.statusFor(true, "再次来访", false));
        assertEquals("PENDING", service.statusFor(true, "https://example.com", false));
        assertEquals("PENDING", service.statusFor(true, "发送太快", true));
    }
}
