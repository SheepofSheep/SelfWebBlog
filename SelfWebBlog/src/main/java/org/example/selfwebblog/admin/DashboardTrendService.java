package org.example.selfwebblog.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardTrendService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final JdbcTemplate jdbcTemplate;

    public DashboardTrendService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardTrend load(int range) {
        return load(range, LocalDate.now(SHANGHAI));
    }

    public DashboardTrend load(int range, LocalDate endDate) {
        if (range != 14 && range != 30) throw new IllegalArgumentException("趋势范围仅支持 14 或 30 天");
        if (endDate == null) throw new IllegalArgumentException("趋势结束日期不能为空");
        LocalDate startDate = endDate.minusDays(range - 1L);
        Map<LocalDate, Long> views = aggregate("""
                SELECT view_date AS day, COUNT(*) AS amount
                FROM post_view_daily
                WHERE view_date >= ? AND view_date <= ?
                GROUP BY view_date
                """, startDate, endDate);
        Map<LocalDate, Long> interactions = aggregate("""
                SELECT date(create_time) AS day, COUNT(*) AS amount
                FROM comment
                WHERE status = 'PUBLISHED' AND date(create_time) >= ? AND date(create_time) <= ?
                GROUP BY date(create_time)
                """, startDate, endDate);
        Map<LocalDate, Long> posts = aggregate("""
                SELECT date(published_time) AS day, COUNT(*) AS amount
                FROM post
                WHERE status = 'PUBLISHED' AND date(published_time) >= ? AND date(published_time) <= ?
                GROUP BY date(published_time)
                """, startDate, endDate);

        List<TrendDay> days = new ArrayList<>(range);
        long totalViews = 0;
        long totalInteractions = 0;
        long totalPosts = 0;
        for (int offset = 0; offset < range; offset++) {
            LocalDate date = startDate.plusDays(offset);
            long dayViews = views.getOrDefault(date, 0L);
            long dayInteractions = interactions.getOrDefault(date, 0L);
            long dayPosts = posts.getOrDefault(date, 0L);
            days.add(new TrendDay(date, dayViews, dayInteractions, dayPosts));
            totalViews += dayViews;
            totalInteractions += dayInteractions;
            totalPosts += dayPosts;
        }
        return new DashboardTrend(
                range,
                startDate,
                endDate,
                List.copyOf(days),
                new TrendTotals(totalViews, totalInteractions, totalPosts));
    }

    private Map<LocalDate, Long> aggregate(String sql, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql,
                (row, index) -> Map.entry(
                        LocalDate.parse(row.getString("day")), row.getLong("amount")),
                startDate.toString(), endDate.toString())
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    public record DashboardTrend(
            int range,
            LocalDate startDate,
            LocalDate endDate,
            List<TrendDay> days,
            TrendTotals totals) {
    }

    public record TrendDay(LocalDate date, long views, long interactions, long posts) {
    }

    public record TrendTotals(long views, long interactions, long posts) {
    }
}
