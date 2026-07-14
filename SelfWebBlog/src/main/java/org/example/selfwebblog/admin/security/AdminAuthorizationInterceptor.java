package org.example.selfwebblog.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.selfwebblog.admin.audit.SecurityAuditService;
import org.example.selfwebblog.controller.AuthHelper;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.example.selfwebblog.security.ClientIpResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {
    private static final String AUDIT_ACTION = AdminAuthorizationInterceptor.class.getName() + ".action";

    private final ObjectMapper objectMapper;
    private final SecurityAuditService auditService;
    private final ClientIpResolver ipResolver;
    private final VisitorIdentityService visitorIdentityService;

    public AdminAuthorizationInterceptor(
            ObjectMapper objectMapper,
            SecurityAuditService auditService,
            ClientIpResolver ipResolver,
            VisitorIdentityService visitorIdentityService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.ipResolver = ipResolver;
        this.visitorIdentityService = visitorIdentityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        AdminOnly policy = findPolicy(handler);
        if (policy == null) return true;
        if (!AuthHelper.isAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Result<Void> result = Result.forbidden("当前账号没有管理员权限");
            result.setTraceId((String) request.getAttribute(org.example.selfwebblog.security.RequestTraceFilter.ATTRIBUTE));
            objectMapper.writeValue(response.getWriter(), result);
            return false;
        }
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            request.setAttribute(AUDIT_ACTION, policy.action());
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        Object action = request.getAttribute(AUDIT_ACTION);
        if (action == null || exception != null || response.getStatus() >= 400) return;
        String ipHash = visitorIdentityService.hash(ipResolver.resolve(request));
        auditService.record(
                AuthHelper.getUserId(request),
                String.valueOf(action),
                "HTTP",
                request.getRequestURI(),
                request.getMethod(),
                ipHash);
    }

    private AdminOnly findPolicy(Object handler) {
        if (!(handler instanceof HandlerMethod method)) return null;
        AdminOnly methodPolicy = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), AdminOnly.class);
        return methodPolicy != null
                ? methodPolicy
                : AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), AdminOnly.class);
    }
}
