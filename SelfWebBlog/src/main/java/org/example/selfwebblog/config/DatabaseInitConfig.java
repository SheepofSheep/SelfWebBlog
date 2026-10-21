package org.example.selfwebblog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseInitConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitConfig.class);

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    public CommandLineRunner seedDatabase(
        DataSource dataSource,
        @Value("${auth.admin.username:admin}") String adminUsername,
        @Value("${auth.admin.password:admin123}") String adminPassword
    ) {
        return args -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            new TransactionTemplate(new DataSourceTransactionManager(dataSource)).executeWithoutResult(status -> {
                ensureAdminUser(jdbcTemplate, adminUsername, adminPassword);
                ensureBlogInfo(jdbcTemplate);
            });
        };
    }

    private void ensureAdminUser(JdbcTemplate jdbcTemplate, String adminUsername, String adminPassword) {
        var credentials = jdbcTemplate.query(
                "SELECT password, role FROM user WHERE username = ?",
                (result, row) -> new AdminCredentials(result.getString("password"), result.getString("role")),
                adminUsername);

        if (credentials.isEmpty()) {
            jdbcTemplate.update(
                """
                    INSERT INTO user (username, password, email, avatar_url, role, nickname)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                adminUsername,
                encodePassword(adminPassword),
                "",
                "",
                "ADMIN",
                "博主"
            );
            log.info("默认管理员账号初始化完成");
            return;
        }

        AdminCredentials existing = credentials.get(0);
        boolean passwordChanged = !matchesConfiguredPassword(adminPassword, existing.password());
        if (passwordChanged || !"ADMIN".equals(existing.role())) {
            jdbcTemplate.update("""
                    UPDATE user SET password = ?, role = 'ADMIN',
                      token_version = COALESCE(token_version, 0) + 1
                    WHERE username = ?
                    """, passwordChanged ? encodePassword(adminPassword) : existing.password(), adminUsername);
            log.info("管理员凭证已同步，旧会话已失效");
        }
    }

    private void ensureBlogInfo(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM blog_info WHERE id = 1",
            Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO blog_info (id, avatar_url, nickname, bio, bg_url) VALUES (?, ?, ?, ?, ?)",
                1,
                "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix",
                "博主",
                "OvO",
                ""
            );
            log.info("blog_info 默认数据初始化完成");
        }
    }

    private String encodePassword(String configuredPassword) {
        if (configuredPassword == null || configuredPassword.isBlank()) {
            return "";
        }
        if (configuredPassword.startsWith("$2a$")
            || configuredPassword.startsWith("$2b$")
            || configuredPassword.startsWith("$2y$")) {
            return configuredPassword;
        }
        return passwordEncoder.encode(configuredPassword);
    }

    private boolean matchesConfiguredPassword(String configuredPassword, String storedPassword) {
        if (configuredPassword == null || configuredPassword.isBlank()) {
            return "".equals(storedPassword);
        }
        if (configuredPassword.startsWith("$2a$") || configuredPassword.startsWith("$2b$")
                || configuredPassword.startsWith("$2y$")) {
            return configuredPassword.equals(storedPassword);
        }
        return storedPassword != null && passwordEncoder.matches(configuredPassword, storedPassword);
    }

    private record AdminCredentials(String password, String role) {}
}
