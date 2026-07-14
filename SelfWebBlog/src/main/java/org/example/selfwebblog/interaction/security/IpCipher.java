package org.example.selfwebblog.interaction.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class IpCipher {
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final byte VERSION = 1;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public IpCipher(@Value("${interaction.security.ip-encryption-key}") String encodedKey) {
        byte[] rawKey;
        try {
            rawKey = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("IP_ENCRYPTION_KEY must be valid Base64", exception);
        }
        if (rawKey.length != 32) {
            throw new IllegalStateException("IP_ENCRYPTION_KEY must decode to 32 bytes");
        }
        this.key = new SecretKeySpec(rawKey, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer payload = ByteBuffer.allocate(1 + nonce.length + encrypted.length);
            payload.put(VERSION).put(nonce).put(encrypted);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt IP metadata", exception);
        }
    }

    public String decrypt(String encodedPayload) {
        try {
            ByteBuffer payload = ByteBuffer.wrap(Base64.getUrlDecoder().decode(encodedPayload));
            if (payload.get() != VERSION) throw new IllegalArgumentException("Unsupported IP payload version");
            byte[] nonce = new byte[NONCE_BYTES];
            payload.get(nonce);
            byte[] encrypted = new byte[payload.remaining()];
            payload.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt IP metadata", exception);
        }
    }
}
