package org.example.selfwebblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.selfwebblog.entity.Result;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class CsrfFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final SessionCookieService cookieService;
    private final ObjectMapper objectMapper;

    public CsrfFilter(SessionCookieService cookieService, ObjectMapper objectMapper) {
        this.cookieService = cookieService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        cookieService.ensureCsrfToken(request, response);
        if (!SAFE_METHODS.contains(request.getMethod()) && !cookieService.hasValidCsrf(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Result<Void> result = Result.forbidden("请求校验已过期，请刷新页面后重试");
            result.setTraceId((String) request.getAttribute(RequestTraceFilter.ATTRIBUTE));
            objectMapper.writeValue(response.getWriter(), result);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
