package org.example.selfwebblog.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitConfigTest {
    @Test
    void rotatesExistingAdminPasswordAndRevokesOldSessionsOnlyWhenCredentialsChange() throws Exception {
        SingleConnectionDataSource dataSource = database();
        try {
            DatabaseInitConfig config = new DatabaseInitConfig();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            config.seedDatabase(dataSource, "owner", "initial-test-password").run();
            jdbc.update("UPDATE user SET token_version = 4 WHERE username = 'owner'");

            config.seedDatabase(dataSource, "owner", "rotated-test-password").run();

            String hash = jdbc.queryForObject("SELECT password FROM user WHERE username = 'owner'", String.class);
            assertTrue(encoder.matches("rotated-test-password", hash));
            assertFalse(encoder.matches("initial-test-password", hash));
            assertEquals(5, jdbc.queryForObject("SELECT token_version FROM user WHERE username = 'owner'", Integer.class));
            config.seedDatabase(dataSource, "owner", "rotated-test-password").run();
            assertEquals(hash, jdbc.queryForObject("SELECT password FROM user WHERE username = 'owner'", String.class));
            assertEquals(5, jdbc.queryForObject("SELECT token_version FROM user WHERE username = 'owner'", Integer.class));
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void acceptsConfiguredBcryptHashesWithoutReencodingThem() throws Exception {
        SingleConnectionDataSource dataSource = database();
        try {
            DatabaseInitConfig config = new DatabaseInitConfig();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            String hash = new BCryptPasswordEncoder().encode("hash-config-test-password");
            config.seedDatabase(dataSource, "owner", "initial-test-password").run();
            config.seedDatabase(dataSource, "owner", hash).run();
            assertEquals(hash, jdbc.queryForObject("SELECT password FROM user WHERE username = 'owner'", String.class));
            assertEquals(1, jdbc.queryForObject("SELECT token_version FROM user WHERE username = 'owner'", Integer.class));
            config.seedDatabase(dataSource, "owner", hash).run();
            assertEquals(1, jdbc.queryForObject("SELECT token_version FROM user WHERE username = 'owner'", Integer.class));
        } finally {
            dataSource.destroy();
        }
    }

    private SingleConnectionDataSource database() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE user(id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT,
                  email TEXT, avatar_url TEXT, role TEXT, nickname TEXT, token_version INTEGER DEFAULT 0)
                """);
        jdbc.execute("CREATE TABLE blog_info(id INTEGER PRIMARY KEY, avatar_url TEXT, nickname TEXT, bio TEXT, bg_url TEXT)");
        return dataSource;
    }
}
