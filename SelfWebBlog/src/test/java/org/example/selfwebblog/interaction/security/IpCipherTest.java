package org.example.selfwebblog.interaction.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class IpCipherTest {

    @Test
    void encryptsWithAuthenticatedEncryptionAndCanDecrypt() {
        String key = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        IpCipher cipher = new IpCipher(key);

        String encrypted = cipher.encrypt("203.0.113.42");

        assertFalse(encrypted.contains("203.0.113.42"));
        assertEquals("203.0.113.42", cipher.decrypt(encrypted));
    }
}
