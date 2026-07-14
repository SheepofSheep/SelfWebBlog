package org.example.selfwebblog.site;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class SiteSummaryService {
    private final JdbcTemplate jdbcTemplate;

    public SiteSummaryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SiteSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                  (SELECT COUNT(*) FROM post WHERE status = 'PUBLISHED') AS post_count,
                  (SELECT COUNT(*) FROM comment WHERE status = 'PUBLISHED') AS interaction_count,
                  (SELECT COALESCE(SUM(view_count), 0) FROM post WHERE status = 'PUBLISHED') AS view_count,
                  (SELECT site_start_date FROM blog_info WHERE id = 1) AS started_at,
                  (SELECT current_status FROM blog_info WHERE id = 1) AS current_status,
                  (SELECT status_updated_time FROM blog_info WHERE id = 1) AS status_updated_time,
                  MAX(
                    COALESCE((SELECT MAX(COALESCE(update_time, create_time)) FROM post WHERE status = 'PUBLISHED'), ''),
                    COALESCE((SELECT MAX(create_time) FROM comment WHERE status = 'PUBLISHED'), '')
                  ) AS latest_activity
                """, (result, row) -> {
            LocalDate startedAt = parseDate(result.getString("started_at"));
            LocalDateTime latestActivity = parse(result.getString("latest_activity"));
            return new SiteSummary(
                    runningDays(startedAt, LocalDate.now()),
                    result.getLong("post_count"),
                    result.getLong("interaction_count"),
                    result.getLong("view_count"),
                    startedAt,
                    latestActivity,
                    result.getString("current_status"),
                    parse(result.getString("status_updated_time")));
        });
    }

    public static long runningDays(LocalDate startedAt, LocalDate today) {
        if (startedAt == null || today == null || startedAt.isAfter(today)) return 0;
        return ChronoUnit.DAYS.between(startedAt, today) + 1;
    }

    private static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDateTime.parse(value.replace(' ', 'T'));
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.substring(0, 10));
    }

    public record SiteSummary(
            long runningDays,
            long postCount,
            long interactionCount,
            long viewCount,
            LocalDate startedAt,
            LocalDateTime latestActivity,
            String currentStatus,
            LocalDateTime statusUpdatedTime) {
    }
}
