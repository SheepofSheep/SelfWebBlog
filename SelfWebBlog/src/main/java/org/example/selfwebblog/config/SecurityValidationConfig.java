package org.example.selfwebblog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SecurityValidationConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityValidationConfig.class);

    private static final String DEV_JWT_SECRET = "this-is-a-dev-only-secret-key-change-in-production";
    private static final String DEV_ADMIN_PASSWORD = "admin123";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${auth.admin.password}")
    private String adminPassword;

    @Value("${auth.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${interaction.security.ip-encryption-key:}")
    private String ipEncryptionKey;

    @Value("${interaction.security.ip-hash-secret:}")
    private String ipHashSecret;

    @Value("${app.frontend-url:}")
    private String frontendUrl;

    @Value("${app.cors.allowed-origins:}")
    private String corsOrigins;

    @Bean
    @Profile("prod")
    public CommandLineRunner validateProdSecurity() {
        return args -> {
            boolean failed = false;

            if (DEV_JWT_SECRET.equals(jwtSecret)) {
                log.error("==========================================");
                log.error("❌ 生产环境安全校验失败！");
                log.error("JWT secret 使用了默认开发值，请设置 JWT_SECRET 环境变量。");
                log.error("示例: export JWT_SECRET=$(openssl rand -base64 64)");
                log.error("==========================================");
                failed = true;
            }

            if (jwtSecret == null || jwtSecret.length() < 32) {
                log.error("JWT_SECRET 长度必须至少为 32 个字符。");
                failed = true;
            }

            if (DEV_ADMIN_PASSWORD.equals(adminPassword)) {
                log.error("==========================================");
                log.error("❌ 生产环境安全校验失败！");
                log.error("管理员密码使用了默认值 'admin123'，请设置 BLOG_ADMIN_PASSWORD 环境变量。");
                log.error("==========================================");
                failed = true;
            }

            if (!secureCookie) {
                log.error("生产环境必须启用 Secure 会话 Cookie。");
                failed = true;
            }

            if (ipEncryptionKey == null || ipEncryptionKey.isBlank()
                    || ipHashSecret == null || ipHashSecret.length() < 24) {
                log.error("生产环境必须配置独立的 IP_ENCRYPTION_KEY 与 IP_HASH_SECRET。");
                failed = true;
            }

            if (frontendUrl == null || !frontendUrl.startsWith("https://")) {
                log.error("生产环境 FRONTEND_URL 必须使用 HTTPS。");
                failed = true;
            }

            if (corsOrigins == null || corsOrigins.contains("*") || !corsOrigins.contains(frontendUrl)) {
                log.error("CORS_ORIGINS 必须显式包含 FRONTEND_URL，且不能使用通配符。");
                failed = true;
            }

            if (failed) {
                throw new IllegalStateException(
                    "生产环境安全配置不完整，请设置 JWT_SECRET 和 BLOG_ADMIN_PASSWORD 环境变量后重新启动");
            }

            log.info("生产环境安全配置校验通过");
        };
    }
}
