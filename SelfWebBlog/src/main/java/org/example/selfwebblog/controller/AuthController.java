package org.example.selfwebblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.security.JwtUtil;
import org.example.selfwebblog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestTemplate restTemplate;

    // 每 IP 每分钟最多 5 次登录/注册（滑动窗口）
    private final ConcurrentHashMap<String, long[]> loginAttempts = new ConcurrentHashMap<>();

    // OAuth state 防 CSRF，10 分钟过期自动淘汰
    private final Cache<String, String> oauthStates = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10000)
            .build();

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

    private String adminPasswordHash;

    public AuthController(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
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

    // ==================== 注册 ====================

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!tryAcquireRateLimit(request)) {
            return Result.rateLimited("请求过于频繁，请稍后再试");
        }

        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");
        String avatarUrl = body.get("avatarUrl");

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
            String token = jwtUtil.generateToken(user.getId(), user.getRole());
            UserInfo info = toUserInfo(user);
            Map<String, Object> data = Map.of("token", token, "user", info);
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    // ==================== 账号密码登录 ====================

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!tryAcquireRateLimit(request)) {
            return Result.rateLimited("请求过于频繁，请稍后再试");
        }

        String username = body.get("username");
        String password = body.get("password");

        if (isBlank(username) || isBlank(password)) {
            return Result.badRequest("用户名和密码不能为空");
        }

        // 管理员可通过此接口登录
        if (adminUsername.equals(username) && passwordEncoder.matches(password, adminPasswordHash)) {
            User adminUser = ensureAdminUser();
            String token = jwtUtil.generateToken(adminUser.getId(), "ADMIN");
            UserInfo info = toUserInfo(adminUser);
            log.info("管理员登录成功");
            return Result.success(Map.of("token", token, "user", info));
        }

        try {
            User user = userService.login(username.trim(), password);
            String token = jwtUtil.generateToken(user.getId(), user.getRole());
            UserInfo info = toUserInfo(user);
            Map<String, Object> data = Map.of("token", token, "user", info);
            log.info("用户登录成功：{}", username);
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.unauthorized(e.getMessage());
        }
    }

    private User ensureAdminUser() {
        User adminUser = userService.getByUsername("admin");
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setRole("ADMIN");
            userService.save(adminUser);
        }
        return adminUser;
    }

    // ==================== 管理员登录 ====================

    @PostMapping("/admin")
    public Result<?> adminLogin(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!tryAcquireRateLimit(request)) {
            return Result.rateLimited("请求过于频繁，请稍后再试");
        }

        String username = body.get("username");
        String password = body.get("password");

        if (isBlank(username) || isBlank(password)) {
            return Result.badRequest("用户名或密码不能为空");
        }

        if (!adminUsername.equals(username) || !passwordEncoder.matches(password, adminPasswordHash)) {
            return Result.unauthorized("用户名或密码错误");
        }

        User adminUser = ensureAdminUser();
        String token = jwtUtil.generateToken(adminUser.getId(), "ADMIN");
        UserInfo info = toUserInfo(adminUser);
        log.info("博主登录成功");
        return Result.success(Map.of("token", token, "user", info));
    }

    // ==================== GitHub OAuth ====================

    @GetMapping("/github")
    public Result<?> githubLogin() {
        if (isBlank(githubClientId)) {
            return Result.serverError("GitHub OAuth 未配置");
        }
        String state = UUID.randomUUID().toString();
        oauthStates.put(state, "");
        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + URLEncoder.encode(githubClientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(githubRedirectUri, StandardCharsets.UTF_8)
                + "&state=" + state
                + "&scope=user:email";
        return Result.success(url);
    }

    @GetMapping("/github/callback")
    public void githubCallback(@RequestParam("code") String code, @RequestParam("state") String state,
                               HttpServletResponse response) throws Exception {
        if (isBlank(githubClientId) || isBlank(githubClientSecret)) {
            response.sendRedirect(frontendUrl + "/#/login?error=not_configured");
            return;
        }
        if (oauthStates.getIfPresent(state) == null) {
            response.sendRedirect(frontendUrl + "/#/login?error=invalid_state");
            return;
        }
        oauthStates.invalidate(state);

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
                response.sendRedirect(frontendUrl + "/#/login?error=no_access_token");
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
            User user = userService.findOrCreateGithubUser(githubId, login, avatarUrl, null, "");
            if (githubId.equals(adminGithubId) && !"ADMIN".equals(user.getRole())) {
                user.setRole("ADMIN");
                userService.updateById(user);
                log.info("博主 GitHub 账号已自动提升为 ADMIN");
            }
            String token = jwtUtil.generateToken(user.getId(), user.getRole());
            log.info("GitHub 用户登录成功：{} (role={})", login, user.getRole());

            // 用 Jackson 安全序列化 JSON
            UserInfo info = toUserInfo(user);
            String userJson = URLEncoder.encode(objectMapper.writeValueAsString(info), StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + "/#/login?token=" + token + "&user=" + userJson);
        } catch (Exception e) {
            log.error("GitHub OAuth 回调失败", e);
            response.sendRedirect(frontendUrl + "/#/login?error=oauth_error");
        }
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
    public Result<?> logout() {
        return Result.success("退出成功");
    }

    // ==================== 频率限制（滑动窗口） ====================

    private static final int MAX_LOGIN_PER_MINUTE = 5;
    private static final long WINDOW_MS = 60_000;

    private boolean tryAcquireRateLimit(HttpServletRequest request) {
        String ip = getClientIp(request);
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;

        long[] slots = loginAttempts.computeIfAbsent(ip, k -> new long[MAX_LOGIN_PER_MINUTE]);
        synchronized (slots) {
            int count = 0;
            int freeIdx = -1;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] > cutoff) {
                    count++;
                } else if (freeIdx == -1) {
                    freeIdx = i;
                }
            }
            if (count >= MAX_LOGIN_PER_MINUTE) {
                return false;
            }
            slots[freeIdx != -1 ? freeIdx : 0] = now;
            return true;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
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
