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
}
