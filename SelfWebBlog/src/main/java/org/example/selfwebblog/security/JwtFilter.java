package org.example.selfwebblog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final SessionCookieService cookieService;

    public JwtFilter(JwtUtil jwtUtil, UserService userService, SessionCookieService cookieService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.cookieService = cookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = cookieService.readSessionToken(request);
        if (token != null && !token.isBlank()) {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserId(token);
                Integer tokenVersion = jwtUtil.getTokenVersion(token);
                User user = userService.getById(userId);
                int currentVersion = user == null || user.getTokenVersion() == null
                        ? 0
                        : user.getTokenVersion();
                if (user != null && tokenVersion != null && tokenVersion == currentVersion) {
                    request.setAttribute("userId", userId);
                    request.setAttribute("role", user.getRole());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
