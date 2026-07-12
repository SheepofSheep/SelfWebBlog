package org.example.selfwebblog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!isLoopback(remoteAddress)) {
            return remoteAddress;
        }

        String realIp = firstValue(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        String forwardedFor = firstValue(request.getHeader("X-Forwarded-For"));
        return forwardedFor != null ? forwardedFor : remoteAddress;
    }

    private boolean isLoopback(String address) {
        return "127.0.0.1".equals(address) || "::1".equals(address) || "0:0:0:0:0:0:0:1".equals(address);
    }

    private String firstValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.split(",", 2)[0].trim();
    }
}
