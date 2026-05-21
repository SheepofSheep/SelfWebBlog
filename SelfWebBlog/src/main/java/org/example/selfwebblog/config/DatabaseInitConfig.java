package org.example.selfwebblog.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class DatabaseInitConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitConfig.class);

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    public CommandLineRunner initDatabase(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // 创建 blog_info 表（如果不存在）
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS blog_info (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    avatar_url TEXT,
                    nickname TEXT,
                    bio TEXT,
                    bg_url TEXT
                )
                """);
            log.info("blog_info 表检查/创建完成");

            // 兼容旧数据库：添加 bg_url 列
            tryAddColumn(jdbcTemplate, "blog_info", "bg_url", "TEXT");

            // 创建 post 表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS post (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    content TEXT,
                    image_url TEXT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            log.info("post 表检查/创建完成");

            // 创建 comment 表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS comment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    post_id INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    nickname TEXT NOT NULL,
                    avatar_url TEXT,
                    role TEXT NOT NULL,
                    pinned INTEGER DEFAULT 0,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            log.info("comment 表检查/创建完成");
            tryAddColumn(jdbcTemplate, "comment", "pinned", "INTEGER DEFAULT 0");

            // 创建 user 表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT,
                    email TEXT,
                    avatar_url TEXT,
                    role TEXT NOT NULL DEFAULT 'USER',
                    github_id TEXT UNIQUE,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            log.info("user 表检查/创建完成");

            // 创建 poster 表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS poster (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    image_url TEXT NOT NULL,
                    link_url TEXT,
                    title TEXT,
                    sort_order INTEGER DEFAULT 0,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            log.info("poster 表检查/创建完成");

            // 初始化默认管理员（密码写入合法 bcrypt hash）
            Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, "admin");
            if (adminCount == null || adminCount == 0) {
                String defaultHash = passwordEncoder.encode("admin123");
                jdbcTemplate.update(
                    "INSERT INTO user (id, username, password, email, avatar_url, role) VALUES (?, ?, ?, ?, ?, ?)",
                    1, "admin", defaultHash, "", "", "ADMIN"
                );
                log.info("默认管理员账号初始化完成");
            }

            // 初始化 blog_info 默认数据
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM blog_info WHERE id = 1", Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.update(
                    "INSERT INTO blog_info (id, avatar_url, nickname, bio, bg_url) VALUES (?, ?, ?, ?, ?)",
                    1,
                    "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix",
                    "博主",
                    "欢迎来到我的博客！",
                    ""
                );
                log.info("blog_info 默认数据初始化完成");
            }
        };
    }

    /**
     * 尝试添加列：仅当列不存在时才添加，其他错误则抛异常
     */
    private void tryAddColumn(JdbcTemplate jdbcTemplate, String table, String column, String type) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            log.info("{}.{} 列已添加", table, column);
        } catch (Exception e) {
            if (isDuplicateColumnError(e)) {
                log.debug("{}.{} 列已存在，跳过", table, column);
            } else {
                throw new RuntimeException("数据库迁移失败: " + table + "." + column, e);
            }
        }
    }

    private boolean isDuplicateColumnError(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SQLException) {
                String msg = cause.getMessage();
                if (msg != null && msg.contains("duplicate column name")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}
