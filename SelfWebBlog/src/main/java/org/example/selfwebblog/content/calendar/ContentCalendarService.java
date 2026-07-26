package org.example.selfwebblog.content.calendar;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContentCalendarService {
    private final JdbcTemplate jdbcTemplate;

    public ContentCalendarService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public MonthCalendar month(int year, int month) {
        YearMonth requested = requireMonth(year, month);
        LocalDateTime start = requested.atDay(1).atStartOfDay();
        LocalDateTime end = requested.plusMonths(1).atDay(1).atStartOfDay();
        List<CalendarPost> posts = jdbcTemplate.query("""
                        SELECT id, title, summary, cover_url, category, published_time
                        FROM post
                        WHERE status = 'PUBLISHED'
                          AND date(published_time) >= ? AND date(published_time) < ?
                        ORDER BY datetime(published_time) ASC, id ASC
                        """,
                (result, row) -> mapPost(result), start.toLocalDate(), end.toLocalDate());

        Map<LocalDate, List<CalendarPost>> grouped = new LinkedHashMap<>();
        for (CalendarPost post : posts) {
            grouped.computeIfAbsent(post.publishedTime().toLocalDate(), ignored -> new ArrayList<>())
                    .add(post);
        }
        List<CalendarDay> days = grouped.entrySet().stream()
                .map(entry -> new CalendarDay(entry.getKey(), entry.getValue().size(), entry.getValue()))
                .toList();
        return new MonthCalendar(requested.toString(), posts.size(), days);
    }

    public static YearMonth requireMonth(int year, int month) {
        int maximum = LocalDate.now().getYear() + 2;
        if (year < 2000 || year > maximum || month < 1 || month > 12) {
            throw new IllegalArgumentException("月份参数无效");
        }
        return YearMonth.of(year, month);
    }

    private CalendarPost mapPost(ResultSet result) throws java.sql.SQLException {
        return new CalendarPost(
                result.getLong("id"),
                result.getString("title"),
                result.getString("summary"),
                result.getString("cover_url"),
                result.getString("category"),
                LocalDateTime.parse(result.getString("published_time").replace(' ', 'T')));
    }

    public record MonthCalendar(String month, int postCount, List<CalendarDay> days) {
    }

    public record CalendarDay(LocalDate date, int count, List<CalendarPost> posts) {
    }

    public record CalendarPost(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            String title,
            String summary,
            String coverUrl,
            String category,
            LocalDateTime publishedTime) {
    }
}
