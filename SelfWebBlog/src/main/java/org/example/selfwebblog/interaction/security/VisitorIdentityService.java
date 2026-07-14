package org.example.selfwebblog.interaction.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class VisitorIdentityService {
    private final byte[] secret;

    public VisitorIdentityService(@Value("${interaction.security.ip-hash-secret}") String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException("IP_HASH_SECRET must contain at least 16 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash visitor identity", exception);
        }
    }
}
