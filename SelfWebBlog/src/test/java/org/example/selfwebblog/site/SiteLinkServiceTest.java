package org.example.selfwebblog.site;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteLinkServiceTest {
    private SingleConnectionDataSource dataSource;
    private SiteLinkService service;

    @BeforeEach
    void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE site_link (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  label TEXT NOT NULL,
                  url TEXT NOT NULL,
                  icon_key TEXT NOT NULL DEFAULT 'link',
                  enabled INTEGER NOT NULL DEFAULT 1,
                  sort_order INTEGER NOT NULL DEFAULT 0,
                  create_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  update_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        service = new SiteLinkService(jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        dataSource.destroy();
    }

    @Test
    void createsTrimmedArbitraryLinksAtTheEndOfTheOrder() {
        SiteLink first = service.create(new SiteLinkService.Request(
                "  文档  ", "https://example.test/docs", "book", true));
        SiteLink second = service.create(new SiteLinkService.Request(
                "邮箱", "mailto:hello@example.test", "mail", true));

        assertEquals("文档", first.label());
        assertEquals(0, first.sortOrder());
        assertEquals(1, second.sortOrder());
        assertEquals(List.of(first.id(), second.id()),
                service.listAll().stream().map(SiteLink::id).toList());
    }

    @Test
    void rejectsUnsafeOrMalformedLinkValues() {
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new SiteLinkService.Request("坏链接", "javascript:alert(1)", "link", true)));
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new SiteLinkService.Request("邮箱", "mailto:not-an-email", "mail", true)));
        assertThrows(IllegalArgumentException.class, () -> service.create(
                new SiteLinkService.Request(" ", "https://example.test", "link", true)));
    }

    @Test
    void updatesVisibilityAndProjectsOnlyEnabledLinksPublicly() {
        SiteLink visible = service.create(new SiteLinkService.Request(
                "文档", "https://example.test/docs", "book", true));
        SiteLink hidden = service.create(new SiteLinkService.Request(
                "旧链接", "https://example.test/old", "link", false));

        SiteLink updated = service.update(hidden.id(), new SiteLinkService.Request(
                "新链接", "https://example.test/new", "sparkles", true));

        assertEquals("新链接", updated.label());
        assertTrue(updated.enabled());
        assertEquals(List.of(visible.id(), hidden.id()),
                service.listEnabled().stream().map(SiteLink::id).toList());

        SiteLink disabled = service.update(visible.id(), new SiteLinkService.Request(
                visible.label(), visible.url(), visible.iconKey(), false));
        assertFalse(disabled.enabled());
        assertEquals(List.of(hidden.id()),
                service.listEnabled().stream().map(SiteLink::id).toList());
    }

    @Test
    void reordersAllLinksAndCompactsOrderAfterDeletion() {
        SiteLink first = service.create(new SiteLinkService.Request(
                "一", "https://example.test/1", "link", true));
        SiteLink second = service.create(new SiteLinkService.Request(
                "二", "https://example.test/2", "link", true));
        SiteLink third = service.create(new SiteLinkService.Request(
                "三", "https://example.test/3", "link", true));

        List<SiteLink> reordered = service.reorder(List.of(third.id(), first.id(), second.id()));
        assertEquals(List.of(third.id(), first.id(), second.id()),
                reordered.stream().map(SiteLink::id).toList());

        service.delete(first.id());
        List<SiteLink> remaining = service.listAll();
        assertEquals(List.of(third.id(), second.id()), remaining.stream().map(SiteLink::id).toList());
        assertEquals(List.of(0, 1), remaining.stream().map(SiteLink::sortOrder).toList());
    }

    @Test
    void rejectsPartialDuplicateOrUnknownReorderRequests() {
        SiteLink first = service.create(new SiteLinkService.Request(
                "一", "https://example.test/1", "link", true));
        SiteLink second = service.create(new SiteLinkService.Request(
                "二", "https://example.test/2", "link", true));

        assertThrows(IllegalArgumentException.class, () -> service.reorder(List.of(first.id())));
        assertThrows(IllegalArgumentException.class,
                () -> service.reorder(List.of(first.id(), first.id())));
        assertThrows(IllegalArgumentException.class,
                () -> service.reorder(List.of(first.id(), second.id() + 100)));
    }
}
