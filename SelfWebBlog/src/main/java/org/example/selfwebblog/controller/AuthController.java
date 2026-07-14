package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.admin.audit.SecurityAuditService;
import org.example.selfwebblog.identity.captcha.CaptchaService;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.example.selfwebblog.security.JwtUtil;
import org.example.selfwebblog.security.ClientIpResolver;
import org.example.selfwebblog.security.LoginRateLimiter;
import org.example.selfwebblog.security.OAuthLoginTicketService;
import org.example.selfwebblog.security.OAuthStateService;
import org.example.selfwebblog.security.SessionCookieService;
import org.example.selfwebblog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String OAUTH_STATE_COOKIE = "OAUTH_STATE";

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ClientIpResolver clientIpResolver;
    private final LoginRateLimiter loginRateLimiter;
    private final OAuthLoginTicketService loginTicketService;
    private final SessionCookieService sessionCookieService;
    private final CaptchaService captchaService;
    private final SecurityAuditService auditService;
    private final VisitorIdentityService visitorIdentityService;
    private final OAuthStateService oauthStateService;
    private RestTemplate restTemplate;

    @Value("${github.oauth.proxy-host:}")
    private String proxyHost;

    @Value("${github.oauth.proxy-port:0}")
    private int proxyPort;

    @Value("${auth.admin.username:admin}")
    private String adminUsername;

    @Value("${auth.admin.password:admin123}")
    private String configuredPassword;

    @Value("${auth.admin.github-id:}")
    private String adminGithubId;

    @Value("${github.oauth.client-id:}")
    private String githubClientId;

    @Value("${github.oauth.client-secret:}")
    private String githubClientSecret;

    @Value("${github.oauth.redirect-uri:http://localhost:8080/auth/github/callback}")
    private String githubRedirectUri;

    @Value("${app.frontend-url:http://localhost:5174}")
    private String frontendUrl;

    @Value("${auth.cookie.secure:false}")
    private boolean secureCookies;

    private String adminPasswordHash;

    public AuthController(
            JwtUtil jwtUtil,
            UserService userService,
            ClientIpResolver clientIpResolver,
            LoginRateLimiter loginRateLimiter,
            OAuthLoginTicketService loginTicketService,
            SessionCookieService sessionCookieService,
            CaptchaService captchaService,
            SecurityAuditService auditService,
            VisitorIdentityService visitorIdentityService) {
        this(jwtUtil, userService, clientIpResolver, loginRateLimiter, loginTicketService,
                sessionCookieService, captchaService, auditService, visitorIdentityService,
                new OAuthStateService());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthController(
            JwtUtil jwtUtil,
            UserService userService,
            ClientIpResolver clientIpResolver,
            LoginRateLimiter loginRateLimiter,
            OAuthLoginTicketService loginTicketService,
            SessionCookieService sessionCookieService,
            CaptchaService captchaService,
            SecurityAuditService auditService,
            VisitorIdentityService visitorIdentityService,
            OAuthStateService oauthStateService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.clientIpResolver = clientIpResolver;
        this.loginRateLimiter = loginRateLimiter;
        this.loginTicketService = loginTicketService;
        this.sessionCookieService = sessionCookieService;
        this.captchaService = captchaService;
        this.auditService = auditService;
        this.visitorIdentityService = visitorIdentityService;
        this.oauthStateService = oauthStateService;
    }

    @PostConstruct
    public void init() {
        // 密码哈希
        if (configuredPassword.startsWith("$2a$") || configuredPassword.startsWith("$2b$") || configuredPassword.startsWith("$2y$")) {
            adminPasswordHash = configuredPassword;
        } else {
            adminPasswordHash = passwordEncoder.encode(configuredPassword);
        }

        // 构建 RestTemplate（自动检测系统代理 > 手动配置 > 直连）
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);

        System.setProperty("java.net.useSystemProxies", "true");
        Proxy detectedProxy = detectProxy("https://github.com");
        if (detectedProxy != null) {
            factory.setProxy(detectedProxy);
            log.info("GitHub OAuth 使用系统代理: {}", detectedProxy.address());
        } else if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
            log.info("GitHub OAuth 使用手动代理: {}:{}", proxyHost, proxyPort);
        } else {
            log.info("GitHub OAuth 直连模式（未检测到代理）");
        }
        this.restTemplate = new RestTemplate(factory);
    }

    private Proxy detectProxy(String url) {
        try {
            var proxies = ProxySelector.getDefault().select(new URI(url));
            if (proxies != null && !proxies.isEmpty()) {
                Proxy p = proxies.get(0);
                if (p.type() != Proxy.Type.DIRECT) {
                    return p;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private ResponseCookie oauthStateCookie(String value, Duration maxAge) {
        return ResponseCookie.from(OAUTH_STATE_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    // ==================== 注册 ====================

    @PostMapping("/register")
    public Result<?> register(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!tryAcquireRateLimit("register", request)) {
            return Result.rateLimited("注册请求过于频繁，请稍后再试", 60);
        }

        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");
        String avatarUrl = body.get("avatarUrl");

        if (!captchaService.verify(body.get("captchaId"), body.get("captchaAnswer"), "register")) {
            return Result.badRequest("验证码不正确或已过期");
        }

        if (isBlank(username) || isBlank(password)) {
            return Result.badRequest("用户名和密码不能为空");
        }
        if (username.length() < 2 || username.length() > 20) {
            return Result.badRequest("用户名长度需在2-20个字符之间");
        }
        if (password.length() < 6) {
            return Result.badRequest("密码长度不能少于6位");
        }

        try {
            User user = userService.register(username.trim(), password, email, avatarUrl, getClientIp(request));
            issueSession(user, response);
            UserInfo info = toUserInfo(user);
            recordAuthEvent(request, user.getId(), "REGISTER_SUCCESS", username);
            return Result.success(Map.of("user", info));
        } catch (RuntimeException e) {
            recordAuthEvent(request, null, "REGISTER_FAILURE", username);
            return Result.badRequest(e.getMessage());
        }
    }

    // ==================== 账号密码登录 ====================

    @PostMapping("/login")
    public Result<?> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!tryAcquireRateLimit("login", request)) {
            return Result.rateLimited("登录请求过于频繁，请稍后再试", 60);
        }

        String username = body.get("username");
        String password = body.get("password");

        if (isBlank(username) || isBlank(password)) {
            return Result.badRequest("用户名和密码不能为空");
        }

        // 管理员可通过此接口登录
        if (adminUsername.equals(username) && passwordEncoder.matches(password, adminPasswordHash)) {
            User adminUser = ensureAdminUser();
            issueSession(adminUser, response);
            UserInfo info = toUserInfo(adminUser);
            recordAuthEvent(request, adminUser.getId(), "LOGIN_SUCCESS", username);
            log.info("管理员登录成功");
            return Result.success(Map.of("user", info));
        }

        try {
            User user = userService.login(username.trim(), password);
            issueSession(user, response);
            UserInfo info = toUserInfo(user);
            Map<String, Object> data = Map.of("user", info);
            recordAuthEvent(request, user.getId(), "LOGIN_SUCCESS", username);
            log.info("用户登录成功：{}", username);
            return Result.success(data);
        } catch (RuntimeException e) {
            recordAuthEvent(request, null, "LOGIN_FAILURE", username);
            return Result.unauthorized(e.getMessage());
        }
    }

    private User ensureAdminUser() {
        User adminUser = userService.getByUsername(adminUsername);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setRole("ADMIN");
            adminUser.setTokenVersion(0);
            userService.save(adminUser);
        }
        return adminUser;
    }

    // ==================== 管理员登录 ====================

    @PostMapping("/admin")
    public Result<?> adminLogin(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!tryAcquireRateLimit("admin-login", request)) {
            return Result.rateLimited("登录请求过于频繁，请稍后再试", 60);
        }

        String username = body.get("username");
        String password = body.get("password");

        if (isBlank(username) || isBlank(password)) {
            return Result.badRequest("用户名或密码不能为空");
        }

        if (!adminUsername.equals(username) || !passwordEncoder.matches(password, adminPasswordHash)) {
            recordAuthEvent(request, null, "ADMIN_LOGIN_FAILURE", username);
            return Result.unauthorized("用户名或密码错误");
        }

        User adminUser = ensureAdminUser();
        issueSession(adminUser, response);
        UserInfo info = toUserInfo(adminUser);
        recordAuthEvent(request, adminUser.getId(), "ADMIN_LOGIN_SUCCESS", username);
        log.info("博主登录成功");
        return Result.success(Map.of("user", info));
    }

    // ==================== GitHub OAuth ====================

    @GetMapping("/github")
    public Result<?> githubLogin(HttpServletResponse response) {
        if (isBlank(githubClientId)) {
            return Result.serverError("GitHub OAuth 未配置");
        }
        String state = oauthStateService.issue();
        response.addHeader(HttpHeaders.SET_COOKIE, oauthStateCookie(state, Duration.ofMinutes(10)).toString());
        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + URLEncoder.encode(githubClientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(githubRedirectUri, StandardCharsets.UTF_8)
                + "&state=" + state
                + "&scope=user:email";
        return Result.success(url);
    }

    @GetMapping("/github/callback")
    public void githubCallback(@RequestParam("code") String code, @RequestParam("state") String state,
                               HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (isBlank(githubClientId) || isBlank(githubClientSecret)) {
            response.sendRedirect(frontendUrl + "/login?error=not_configured");
            return;
        }
        String browserState = readCookie(request, OAUTH_STATE_COOKIE);
        if (!oauthStateService.consume(state, browserState)) {
            response.addHeader(HttpHeaders.SET_COOKIE, oauthStateCookie("", Duration.ZERO).toString());
            response.sendRedirect(frontendUrl + "/login?error=invalid_state");
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, oauthStateCookie("", Duration.ZERO).toString());

        try {
            // 用 code 换取 access_token
            MultiValueMap<String, String> tokenBody = new LinkedMultiValueMap<>();
            tokenBody.add("client_id", githubClientId);
            tokenBody.add("client_secret", githubClientSecret);
            tokenBody.add("code", code);
            tokenBody.add("redirect_uri", githubRedirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenBody, headers);
            ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                    "https://github.com/login/oauth/access_token",
                    HttpMethod.POST, tokenRequest, Map.class);

            String accessToken = (String) tokenResponse.getBody().get("access_token");
            if (accessToken == null) {
                response.sendRedirect(frontendUrl + "/login?error=no_access_token");
                return;
            }

            // 用 access_token 获取用户信息
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);
            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET, userRequest, Map.class);

            Map<String, Object> githubUser = userResponse.getBody();
            String githubId = String.valueOf(githubUser.get("id"));
            String login = (String) githubUser.get("login");
            String avatarUrl = (String) githubUser.get("avatar_url");

            // 查找或创建用户，如果是博主 GitHub 账号则赋予 ADMIN 角色
            User user = userService.findOrCreateGithubUser(
                    githubId, login, avatarUrl, null, getClientIp(request));
            if (githubId.equals(adminGithubId) && !"ADMIN".equals(user.getRole())) {
                user.setRole("ADMIN");
                user.setTokenVersion(currentTokenVersion(user) + 1);
                userService.updateById(user);
                log.info("博主 GitHub 账号已自动提升为 ADMIN");
            }
            log.info("GitHub 用户登录成功：{} (role={})", login, user.getRole());

            String ticket = loginTicketService.issue(user);
            response.sendRedirect(frontendUrl + "/login?ticket=" + ticket);
        } catch (Exception e) {
            log.error("GitHub OAuth 回调失败", e);
            response.sendRedirect(frontendUrl + "/login?error=oauth_error");
        }
    }

    @PostMapping("/exchange")
    public Result<?> exchangeOAuthTicket(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        return loginTicketService.consume(body.get("ticket"))
                .map(userService::getById)
                .filter(Objects::nonNull)
                .<Result<?>>map(user -> {
                    issueSession(user, response);
                    return Result.success(Map.of("user", toUserInfo(user)));
                })
                .orElseGet(() -> Result.unauthorized("登录票据无效或已过期"));
    }

    @GetMapping("/csrf")
    public Result<Map<String, String>> csrf(HttpServletRequest request, HttpServletResponse response) {
        String token = sessionCookieService.ensureCsrfToken(request, response);
        return Result.success(Map.of("token", token));
    }

    // ==================== 获取当前用户 ====================

    @GetMapping("/me")
    public Result<?> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("未登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        UserInfo info = toUserInfo(user);
        return Result.success(info);
    }

    @GetMapping("/session")
    public Result<UserInfo> getSession(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.success(null);
        }
        User user = userService.getById(userId);
        return Result.success(user == null ? null : toUserInfo(user));
    }

    private UserInfo toUserInfo(User user) {
        return new UserInfo(
            user.getId(),
            user.getUsername(),
            user.getNickname() != null ? user.getNickname() : "",
            user.getAvatarUrl(),
            user.getRole(),
            user.getTitleName() != null ? user.getTitleName() : "",
            user.getTitleStyle() != null ? user.getTitleStyle() : "default"
        );
    }

    // ==================== 退出登录 ====================

    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Long userId = AuthHelper.getUserId(request);
        if (userId != null) {
            User user = userService.getById(userId);
            if (user != null) {
                user.setTokenVersion(currentTokenVersion(user) + 1);
                userService.updateById(user);
            }
        }
        sessionCookieService.clearSession(response);
        return Result.success("退出成功");
    }

    // ==================== 频率限制（滑动窗口） ====================

    private boolean tryAcquireRateLimit(String action, HttpServletRequest request) {
        return loginRateLimiter.tryAcquire(action, getClientIp(request));
    }

    private String getClientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }

    private void issueSession(User user, HttpServletResponse response) {
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), currentTokenVersion(user));
        sessionCookieService.issueSession(response, token);
    }

    private void recordAuthEvent(HttpServletRequest request, Long userId, String action, String username) {
        String ipHash = visitorIdentityService.hash(getClientIp(request));
        auditService.record(userId, action, "AUTH", "", username == null ? "" : username, ipHash);
    }

    private int currentTokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    // ==================== 工具方法 ====================

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ==================== 内部类 ====================

    private static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
        private String role;
        private String titleName;
        private String titleStyle;

        public UserInfo() {}

        public UserInfo(Long id, String username, String nickname, String avatarUrl, String role, String titleName, String titleStyle) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
            this.avatarUrl = avatarUrl;
            this.role = role;
            this.titleName = titleName;
            this.titleStyle = titleStyle;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getTitleName() { return titleName; }
        public void setTitleName(String titleName) { this.titleName = titleName; }
        public String getTitleStyle() { return titleStyle; }
        public void setTitleStyle(String titleStyle) { this.titleStyle = titleStyle; }
    }
}
