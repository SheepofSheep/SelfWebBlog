package org.example.selfwebblog.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class SessionCookieService {
    public static final String SESSION_COOKIE = "BLOG_SESSION";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";
    public static final String CSRF_ATTRIBUTE = "csrfToken";

    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean secure;
    private final Duration sessionDuration;

    public SessionCookieService(
            @Value("${auth.cookie.secure:false}") boolean secure,
            @Value("${jwt.expiration:604800}") long expirationSeconds) {
        this.secure = secure;
        this.sessionDuration = Duration.ofSeconds(Math.max(60, expirationSeconds));
    }

    public void issueSession(HttpServletResponse response, String token) {
        addCookie(response, ResponseCookie.from(SESSION_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(sessionDuration)
                .build());
        rotateCsrfToken(response);
    }

    public void clearSession(HttpServletResponse response) {
        addCookie(response, expiredCookie(SESSION_COOKIE, true));
        addCookie(response, expiredCookie(CSRF_COOKIE, false));
    }

    public String ensureCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        String existing = readCookie(request, CSRF_COOKIE);
        String token = existing == null || existing.isBlank() ? newToken() : existing;
        if (!token.equals(existing)) writeCsrfCookie(response, token);
        request.setAttribute(CSRF_ATTRIBUTE, token);
        return token;
    }

    public void rotateCsrfToken(HttpServletResponse response) {
        writeCsrfCookie(response, newToken());
    }

    public boolean hasValidCsrf(HttpServletRequest request) {
        String cookie = readCookie(request, CSRF_COOKIE);
        String header = request.getHeader(CSRF_HEADER);
        if (cookie == null || header == null) return false;
        return MessageDigest.isEqual(
                cookie.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                header.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String readSessionToken(HttpServletRequest request) {
        return readCookie(request, SESSION_COOKIE);
    }

    private void writeCsrfCookie(HttpServletResponse response, String token) {
        addCookie(response, ResponseCookie.from(CSRF_COOKIE, token)
                .httpOnly(false)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(sessionDuration)
                .build());
    }

    private ResponseCookie expiredCookie(String name, boolean httpOnly) {
        return ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
