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
}
