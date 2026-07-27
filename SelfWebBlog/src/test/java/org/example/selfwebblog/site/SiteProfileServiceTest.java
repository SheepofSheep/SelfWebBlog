package org.example.selfwebblog.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SiteProfileServiceTest {
    private SingleConnectionDataSource dataSource;

    @AfterEach
    void tearDown() {
        if (dataSource != null) dataSource.destroy();
    }

    @Test
    void publicProfileIncludesOnlyEnabledLinksInStableOrder() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE blog_info (
                  id INTEGER PRIMARY KEY,
                  nickname TEXT, bio TEXT, avatar_url TEXT, bg_url TEXT,
                  site_start_date TEXT, current_status TEXT, status_updated_time TEXT,
                  about_markdown TEXT, social_links TEXT
                )
                """);
        jdbc.execute("""
                CREATE TABLE site_link (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  label TEXT NOT NULL, url TEXT NOT NULL, icon_key TEXT NOT NULL,
                  enabled INTEGER NOT NULL, sort_order INTEGER NOT NULL,
                  create_time TEXT DEFAULT CURRENT_TIMESTAMP,
                  update_time TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbc.update("""
                INSERT INTO blog_info(id, nickname, bio, avatar_url, bg_url, social_links)
                VALUES (1, '博主', '', '', '', '{}')
                """);
        jdbc.update("""
                INSERT INTO site_link(label, url, icon_key, enabled, sort_order)
                VALUES ('隐藏', 'https://example.test/hidden', 'link', 0, 0),
                       ('第二', 'https://example.test/second', 'link', 1, 2),
                       ('第一', 'https://example.test/first', 'link', 1, 1)
                """);

        SiteLinkService links = new SiteLinkService(jdbc);
        SiteProfileService service = new SiteProfileService(jdbc, new ObjectMapper(), links);

        assertEquals(java.util.List.of("第一", "第二"),
                service.get().links().stream().map(SiteLink::label).toList());
    }
}
