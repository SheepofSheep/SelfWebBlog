package org.example.selfwebblog.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardTrendServiceTest {
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private DashboardTrendService service;

    @BeforeEach
    void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE post_view_daily (view_date TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE comment (status TEXT NOT NULL, create_time TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE post (status TEXT NOT NULL, create_time TEXT NOT NULL, published_time TEXT)");
        service = new DashboardTrendService(jdbc);
    }

    @AfterEach
    void tearDown() {
        dataSource.destroy();
    }

    @Test
    void fillsEveryDayAndAggregatesPublishedActivity() {
        jdbc.update("INSERT INTO post_view_daily(view_date) VALUES ('2026-07-26'), ('2026-07-26')");
        jdbc.update("""
                INSERT INTO comment(status, create_time) VALUES
                  ('PUBLISHED', '2026-07-26 09:00:00'),
                  ('PUBLISHED', '2026-07-26 10:00:00'),
                  ('HIDDEN', '2026-07-26 11:00:00')
                """);
        jdbc.update("""
                INSERT INTO post(status, create_time, published_time) VALUES
                  ('PUBLISHED', '2026-07-01 08:00:00', '2026-07-25 08:00:00'),
                  ('DRAFT', '2026-07-26 08:00:00', NULL)
                """);

        DashboardTrendService.DashboardTrend trend =
                service.load(14, LocalDate.of(2026, 7, 26));

        assertEquals(14, trend.days().size());
        assertEquals(LocalDate.of(2026, 7, 13), trend.days().get(0).date());
        assertEquals(0, trend.days().get(0).views());
        assertEquals(2, trend.days().get(13).views());
        assertEquals(2, trend.days().get(13).interactions());
        assertEquals(1, trend.days().get(12).posts());
        assertEquals(2, trend.totals().views());
        assertEquals(2, trend.totals().interactions());
        assertEquals(1, trend.totals().posts());
    }

    @Test
    void supportsOnlyTheFourteenAndThirtyDayRanges() {
        assertEquals(30, service.load(30, LocalDate.of(2026, 7, 26)).days().size());
        assertThrows(IllegalArgumentException.class,
                () -> service.load(7, LocalDate.of(2026, 7, 26)));
    }
}
