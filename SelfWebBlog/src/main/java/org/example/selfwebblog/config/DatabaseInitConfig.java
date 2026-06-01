package org.example.selfwebblog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
            ensureAdminUser(jdbcTemplate, adminUsername, adminPassword);
            ensureBlogInfo(jdbcTemplate);
        };
    }

    private void ensureAdminUser(JdbcTemplate jdbcTemplate, String adminUsername, String adminPassword) {
        Integer adminCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user WHERE username = ?",
            Integer.class,
            adminUsername
        );

        if (adminCount == null || adminCount == 0) {
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

        jdbcTemplate.update("UPDATE user SET role = 'ADMIN' WHERE username = ?", adminUsername);
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
}
