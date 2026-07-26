package org.example.selfwebblog.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void createsNormalizedTagRelationsAndImageAssets() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("content.db");
        Flyway.configure().dataSource(jdbcUrl, null, null).load().migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO post (id, title, content) VALUES (7, 'Post', 'Body')");
            statement.executeUpdate("INSERT INTO tag (id, name, slug) VALUES (3, 'Vue', 'vue')");
            statement.executeUpdate("INSERT INTO post_tag (post_id, tag_id) VALUES (7, 3)");

            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM post_tag"));
            assertTrue(hasUniqueIndex(statement, "post_tag"));

            statement.executeUpdate("""
                    INSERT INTO image_asset (
                        content_hash, purpose, original_path, original_mime,
                        width, height, byte_size, uploader_id, reference_status
                    ) VALUES ('abc', 'ARTICLE', '/uploads/a.png', 'image/png', 10, 10, 100, 1, 'ACTIVE')
                    """);
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM image_asset"));
        }
    }

    @Test
    void upgradesVersionNineDataWithoutChangingContentCounts() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("upgrade-v9.db");
        Flyway.configure().dataSource(jdbcUrl, null, null).target("9").load().migrate();
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO post (id, title, content, status) VALUES (91, '旧文章', '正文', 'PUBLISHED')");
            statement.executeUpdate("INSERT INTO tag (id, name, slug) VALUES (92, '旧标签', 'legacy')");
            statement.executeUpdate("INSERT INTO post_tag (post_id, tag_id) VALUES (91, 92)");
            statement.executeUpdate("""
                    INSERT INTO comment (id, post_id, target_type, target_id, content, nickname, role, status)
                    VALUES (93, 91, 'POST', 91, '旧评论', '访客', 'USER', 'PUBLISHED')
                    """);
        }

        Flyway.configure().dataSource(jdbcUrl, null, null).load().migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM post"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM tag"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM post_tag"));
            assertEquals(1, scalar(statement, "SELECT COUNT(*) FROM comment"));
            try (ResultSet post = statement.executeQuery("""
                    SELECT background_mode, background_value, background_focus_x, background_focus_y
                    FROM post WHERE id = 91
                    """)) {
                assertTrue(post.next());
                assertEquals("COVER", post.getString("background_mode"));
                assertEquals("", post.getString("background_value"));
                assertEquals(50, post.getInt("background_focus_x"));
                assertEquals(50, post.getInt("background_focus_y"));
            }
            assertTrue(hasIndex(statement, "post", "idx_post_status_create_time"));
            assertTrue(hasIndex(statement, "comment", "idx_comment_status_create_time"));
            assertTrue(hasColumn(statement, "post", "archived_time"));
            assertTrue(hasIndex(statement, "post", "idx_post_status_archived_time"));
            assertTrue(hasColumn(statement, "post", "published_time"));
            assertTrue(hasIndex(statement, "post", "idx_post_status_published_time"));
            assertEquals(1, scalar(statement,
                    "SELECT COUNT(*) FROM post WHERE id = 91 AND published_time = create_time"));
            assertEquals(0, scalar(statement, "SELECT COUNT(*) FROM site_link"));
        }
    }

    @Test
    void migratesKnownSocialLinksIntoOrderedSiteLinks() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("site-links-v10.db");
        Flyway.configure().dataSource(jdbcUrl, null, null).target("10").load().migrate();
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO blog_info (id, nickname, social_links)
                    VALUES (1, '站点', '{"github":"https://github.com/example","email":"hello@example.com"}')
                    """);
        }

        Flyway.configure().dataSource(jdbcUrl, null, null).load().migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            assertEquals(2, scalar(statement, "SELECT COUNT(*) FROM site_link"));
            try (ResultSet links = statement.executeQuery("""
                    SELECT label, url, sort_order FROM site_link ORDER BY sort_order
                    """)) {
                assertTrue(links.next());
                assertEquals("GitHub", links.getString("label"));
                assertEquals("https://github.com/example", links.getString("url"));
                assertEquals(0, links.getInt("sort_order"));
                assertTrue(links.next());
                assertEquals("邮箱", links.getString("label"));
                assertEquals("mailto:hello@example.com", links.getString("url"));
                assertEquals(1, links.getInt("sort_order"));
            }
        }
    }

    private int scalar(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private boolean hasUniqueIndex(Statement statement, String table) throws Exception {
        try (ResultSet indexes = statement.executeQuery("PRAGMA index_list('" + table + "')")) {
            while (indexes.next()) {
                if (indexes.getInt("unique") == 1) return true;
            }
        }
        return false;
    }

    private boolean hasIndex(Statement statement, String table, String name) throws Exception {
        try (ResultSet indexes = statement.executeQuery("PRAGMA index_list('" + table + "')")) {
            while (indexes.next()) {
                if (name.equals(indexes.getString("name"))) return true;
            }
        }
        return false;
    }

    private boolean hasColumn(Statement statement, String table, String name) throws Exception {
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info('" + table + "')")) {
            while (columns.next()) {
                if (name.equals(columns.getString("name"))) return true;
            }
        }
        return false;
    }
}
