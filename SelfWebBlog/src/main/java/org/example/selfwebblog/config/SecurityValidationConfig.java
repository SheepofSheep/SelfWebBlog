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

            if (DEV_ADMIN_PASSWORD.equals(adminPassword)) {
                log.error("==========================================");
                log.error("❌ 生产环境安全校验失败！");
                log.error("管理员密码使用了默认值 'admin123'，请设置 BLOG_ADMIN_PASSWORD 环境变量。");
                log.error("==========================================");
                failed = true;
            }

            if (failed) {
                throw new IllegalStateException(
                    "生产环境安全配置不完整，请设置 JWT_SECRET 和 BLOG_ADMIN_PASSWORD 环境变量后重新启动");
            }

            log.info("✅ 生产环境安全配置校验通过");
        };
    }
}
