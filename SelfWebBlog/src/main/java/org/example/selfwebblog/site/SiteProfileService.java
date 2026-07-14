package org.example.selfwebblog.site;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SiteProfileService {
    private static final Set<String> SOCIAL_KEYS = Set.of("github", "bilibili", "email", "website");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SiteProfileService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public SiteProfile get() {
        return jdbcTemplate.queryForObject("""
                SELECT nickname, bio, avatar_url, bg_url, site_start_date,
                       current_status, status_updated_time, about_markdown, social_links
                FROM blog_info
                WHERE id = 1
                """, (result, row) -> new SiteProfile(
                value(result.getString("nickname")),
                value(result.getString("bio")),
                value(result.getString("avatar_url")),
                value(result.getString("bg_url")),
                parseDate(result.getString("site_start_date")),
                value(result.getString("current_status")),
                parseDateTime(result.getString("status_updated_time")),
                value(result.getString("about_markdown")),
                parseSocialLinks(result.getString("social_links"))));
    }

    @Transactional
    public SiteProfile update(SiteSettingsRequest request) {
        String nickname = required(request.nickname(), "昵称", 40);
        String bio = limited(request.bio(), "简介", 240);
        String currentStatus = limited(request.currentStatus(), "当前近况", 300);
        String aboutMarkdown = limited(request.aboutMarkdown(), "作者正文", 20_000);
        LocalDate startDate = request.siteStartDate() == null ? LocalDate.now() : request.siteStartDate();
        if (startDate.isAfter(LocalDate.now())) throw new IllegalArgumentException("建站日期不能晚于今天");

        Map<String, String> socialLinks = normalizeSocialLinks(request.socialLinks());
        LocalDateTime statusUpdated = LocalDateTime.now();
        jdbcTemplate.update("""
                UPDATE blog_info
                SET nickname = ?, bio = ?, avatar_url = ?, bg_url = ?, site_start_date = ?,
                    current_status = ?, status_updated_time = ?, about_markdown = ?, social_links = ?
                WHERE id = 1
                """,
                nickname,
                bio,
                limited(request.avatarUrl(), "头像地址", 1000),
                limited(request.bgUrl(), "背景地址", 1000),
                startDate,
                currentStatus,
                statusUpdated,
                aboutMarkdown,
                writeSocialLinks(socialLinks));
        return get();
    }

    private Map<String, String> normalizeSocialLinks(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        if (source == null) return result;
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (!SOCIAL_KEYS.contains(entry.getKey())) continue;
            String value = limited(entry.getValue(), "社交链接", 500);
            if (!value.isBlank() && !isSafeSocialValue(entry.getKey(), value)) {
                throw new IllegalArgumentException("社交链接仅支持 HTTPS 地址或邮箱");
            }
            if (!value.isBlank()) result.put(entry.getKey(), value);
        }
        return result;
    }

    private boolean isSafeSocialValue(String key, String value) {
        if ("email".equals(key)) return value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
        return value.startsWith("https://");
    }

    private Map<String, String> parseSocialLinks(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String writeSocialLinks(Map<String, String> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("社交链接格式无效", exception);
        }
    }

    private String required(String value, String label, int maxLength) {
        String normalized = limited(value, label, maxLength);
        if (normalized.isBlank()) throw new IllegalArgumentException(label + "不能为空");
        return normalized;
    }

    private String limited(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(label + "内容过长");
        return normalized;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.substring(0, 10));
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value.replace(' ', 'T'));
    }

    public record SiteProfile(
            String nickname,
            String bio,
            String avatarUrl,
            String bgUrl,
            LocalDate siteStartDate,
            String currentStatus,
            LocalDateTime statusUpdatedTime,
            String aboutMarkdown,
            Map<String, String> socialLinks) {
    }

    public record SiteSettingsRequest(
            String nickname,
            String bio,
            String avatarUrl,
            String bgUrl,
            LocalDate siteStartDate,
            String currentStatus,
            String aboutMarkdown,
            Map<String, String> socialLinks) {
    }
}
