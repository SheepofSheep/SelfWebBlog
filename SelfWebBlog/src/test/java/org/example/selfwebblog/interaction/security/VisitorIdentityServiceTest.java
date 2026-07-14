package org.example.selfwebblog.interaction.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VisitorIdentityServiceTest {

    @Test
    void hashesVisitorIdentityWithASecret() {
        VisitorIdentityService service = new VisitorIdentityService("first-secret-for-tests");
        VisitorIdentityService anotherSecret = new VisitorIdentityService("second-secret-for-tests");

        assertEquals(service.hash("203.0.113.42"), service.hash("203.0.113.42"));
        assertNotEquals(service.hash("203.0.113.42"), service.hash("203.0.113.43"));
        assertNotEquals(service.hash("203.0.113.42"), anotherSecret.hash("203.0.113.42"));
    }
}
