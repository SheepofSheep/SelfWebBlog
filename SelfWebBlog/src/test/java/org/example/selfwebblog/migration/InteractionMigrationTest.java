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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void preservesLegacyCommentsAndCreatesEngagementConstraints() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("interaction.db");
        Flyway.configure().dataSource(jdbcUrl, null, null).target("3").load().migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO post (id, title, status) VALUES (7, 'Legacy post', 'PUBLISHED')
                    """);
            statement.executeUpdate("""
                    INSERT INTO comment (
                        id, post_id, content, nickname, role, pinned, title_name, title_style, user_id
                    ) VALUES (9, 7, 'Legacy comment', 'Gabriel', 'ADMIN', 1, '', 'default', 1)
                    """);
        }

        Flyway.configure().dataSource(jdbcUrl, null, null).load().migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            try (ResultSet row = statement.executeQuery("""
                    SELECT post_id, target_type, target_id, status, like_count
                    FROM comment WHERE id = 9
                    """)) {
                assertTrue(row.next());
                assertEquals(7L, row.getLong("post_id"));
                assertEquals("POST", row.getString("target_type"));
                assertEquals(7L, row.getLong("target_id"));
                assertEquals("PUBLISHED", row.getString("status"));
                assertEquals(0, row.getInt("like_count"));
            }

            statement.executeUpdate("""
                    INSERT INTO comment (
                        id, post_id, target_type, target_id, content, nickname, role, status
                    ) VALUES (10, NULL, 'GUESTBOOK', 1, 'Hello', 'Visitor', 'GUEST', 'PENDING')
                    """);

            assertTrue(hasUniqueIndex(statement, "interaction_like"));
            assertTrue(hasUniqueIndex(statement, "post_view_daily"));
            assertTrue(hasColumn(statement, "comment", "pin_order"));
            assertTrue(hasColumn(statement, "comment", "pinned_at"));
            assertTrue(hasIndex(statement, "comment", "idx_interaction_pin_order"));
        }
    }

    @Test
    void versionTwelveCompactsPopulatedPinOrdersBeforeAddingTheUniqueInvariant() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("pin-order.db");
        Flyway.configure().dataSource(jdbcUrl, null, null).target("11").load().migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO post (id, title, status) VALUES (7, 'Post', 'PUBLISHED')");
            statement.executeUpdate("""
                    INSERT INTO comment (
                        id, post_id, target_type, target_id, content, nickname, role,
                        pinned, pin_order, pinned_at, status, create_time
                    ) VALUES
                        (41, 7, 'POST', 7, 'one', 'A', 'USER', 1, 9, '2026-01-02', 'PUBLISHED', '2026-01-02'),
                        (42, 7, 'POST', 7, 'two', 'B', 'USER', 1, 9, '2026-01-01', 'PUBLISHED', '2026-01-01')
                    """);
        }

        Flyway.configure().dataSource(jdbcUrl, null, null).load().migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("""
                    SELECT id, pin_order FROM comment WHERE pinned = 1 ORDER BY pin_order
                    """)) {
                assertTrue(rows.next());
                assertEquals(42L, rows.getLong("id"));
                assertEquals(0, rows.getInt("pin_order"));
                assertTrue(rows.next());
                assertEquals(41L, rows.getLong("id"));
                assertEquals(1, rows.getInt("pin_order"));
            }
            assertTrue(hasNamedUniqueIndex(statement, "comment", "uq_comment_target_pin_order"));
            assertThrows(Exception.class, () -> statement.executeUpdate("""
                    INSERT INTO comment (
                        id, post_id, target_type, target_id, content, nickname, role,
                        pinned, pin_order, status
                    ) VALUES (43, 7, 'POST', 7, 'duplicate', 'C', 'USER', 1, 1, 'PUBLISHED')
                    """));
            assertThrows(Exception.class, () -> statement.executeUpdate("""
                    INSERT INTO comment (
                        id, post_id, target_type, target_id, content, nickname, role,
                        pinned, pin_order, status
                    ) VALUES (44, 7, 'POST', 7, 'missing order', 'D', 'USER', 1, NULL, 'PUBLISHED')
                    """));
            assertTrue(hasTrigger(statement, "trg_comment_pinned_order_insert"));
            assertTrue(hasTrigger(statement, "trg_comment_pinned_order_update"));
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

    private boolean hasColumn(Statement statement, String table, String name) throws Exception {
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info('" + table + "')")) {
            while (columns.next()) {
                if (name.equals(columns.getString("name"))) return true;
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

    private boolean hasNamedUniqueIndex(Statement statement, String table, String name) throws Exception {
        try (ResultSet indexes = statement.executeQuery("PRAGMA index_list('" + table + "')")) {
            while (indexes.next()) {
                if (name.equals(indexes.getString("name")) && indexes.getInt("unique") == 1) return true;
            }
        }
        return false;
    }

    private boolean hasTrigger(Statement statement, String name) throws Exception {
        try (ResultSet triggers = statement.executeQuery(
                "SELECT name FROM sqlite_master WHERE type = 'trigger' AND name = '" + name + "'")) {
            return triggers.next();
        }
    }
}
