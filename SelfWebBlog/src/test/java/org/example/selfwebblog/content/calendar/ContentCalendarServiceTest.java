package org.example.selfwebblog.content.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentCalendarServiceTest {

    @Test
    void groupsPostsByFirstPublicationTimeInsteadOfDraftCreationTime() {
        SingleConnectionDataSource dataSource =
                new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("""
                    CREATE TABLE post (
                      id INTEGER PRIMARY KEY, title TEXT, summary TEXT, cover_url TEXT,
                      category TEXT, status TEXT, create_time TEXT, published_time TEXT
                    )
                    """);
            jdbc.update("""
                    INSERT INTO post(
                      id, title, summary, cover_url, category, status, create_time, published_time
                    ) VALUES
                      (1, '晚些发布', '', '', '', 'PUBLISHED',
                       '2026-06-01 08:00:00', '2026-07-20 09:30:00'),
                      (2, '八月发布', '', '', '', 'PUBLISHED',
                       '2026-07-05 08:00:00', '2026-08-01 09:30:00')
                    """);

            ContentCalendarService.MonthCalendar result =
                    new ContentCalendarService(jdbc).month(2026, 7);

            assertEquals(1, result.postCount());
            assertEquals(LocalDate.of(2026, 7, 20), result.days().get(0).date());
            assertEquals("晚些发布", result.days().get(0).posts().get(0).title());
            assertEquals(LocalDate.of(2026, 7, 20),
                    result.days().get(0).posts().get(0).publishedTime().toLocalDate());
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void validatesCalendarRange() {
        assertEquals(YearMonth.of(2026, 7), ContentCalendarService.requireMonth(2026, 7));
        assertThrows(IllegalArgumentException.class,
                () -> ContentCalendarService.requireMonth(2026, 13));
        assertThrows(IllegalArgumentException.class,
                () -> ContentCalendarService.requireMonth(LocalDate.now().getYear() + 3, 1));
    }
}
