package org.example.selfwebblog.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "change-me-in-production-please-use-a-long-random-string";
    private long expiration = 604800; // 7 days in seconds
}
