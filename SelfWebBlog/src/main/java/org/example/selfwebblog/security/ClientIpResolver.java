package org.example.selfwebblog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientIpResolver {
    private final Set<String> trustedProxies;

    public ClientIpResolver() {
        this("127.0.0.1,::1,0:0:0:0:0:0:0:1");
    }

    @Autowired
    public ClientIpResolver(@Value("${security.trusted-proxies:127.0.0.1,::1}") String proxies) {
        this.trustedProxies = Arrays.stream(proxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddress)) {
            return remoteAddress;
        }

        String realIp = validAddress(firstValue(request.getHeader("X-Real-IP")));
        if (realIp != null) {
            return realIp;
        }

        String forwardedFor = validAddress(firstValue(request.getHeader("X-Forwarded-For")));
        return forwardedFor != null ? forwardedFor : remoteAddress;
    }

    private String validAddress(String address) {
        if (address == null || address.length() > 45) return null;
        if (address.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) {
            for (String part : address.split("\\.")) {
                if (Integer.parseInt(part) > 255) return null;
            }
            return address;
        }
        return address.matches("[0-9A-Fa-f:]+") && address.contains(":") ? address : null;
    }

    private String firstValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.split(",", 2)[0].trim();
    }
}
