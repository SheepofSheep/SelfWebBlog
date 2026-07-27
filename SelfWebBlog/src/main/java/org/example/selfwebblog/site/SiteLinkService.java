package org.example.selfwebblog.site;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SiteLinkService {
    private static final String SELECT_COLUMNS = """
            id, label, url, icon_key, enabled, sort_order, create_time, update_time
            """;
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final JdbcTemplate jdbcTemplate;

    public SiteLinkService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SiteLink> listAll() {
        return jdbcTemplate.query("SELECT " + SELECT_COLUMNS + " FROM site_link ORDER BY sort_order, id",
                (result, row) -> map(result));
    }

    public List<SiteLink> listEnabled() {
        return jdbcTemplate.query("""
                        SELECT %s FROM site_link
                        WHERE enabled = 1
                        ORDER BY sort_order, id
                        """.formatted(SELECT_COLUMNS),
                (result, row) -> map(result));
    }

    @Transactional
    public SiteLink create(Request request) {
        Normalized normalized = normalize(request);
        Integer nextOrder = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM site_link", Integer.class);
        Long id = jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO site_link(label, url, icon_key, enabled, sort_order)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, normalized.label());
                statement.setString(2, normalized.url());
                statement.setString(3, normalized.iconKey());
                statement.setInt(4, normalized.enabled() ? 1 : 0);
                statement.setInt(5, nextOrder == null ? 0 : nextOrder);
                statement.executeUpdate();
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT last_insert_rowid()")) {
                if (!result.next()) throw new IllegalStateException("站点链接保存失败");
                return result.getLong(1);
            }
        });
        return require(id);
    }

    @Transactional
    public SiteLink update(Long id, Request request) {
        require(id);
        Normalized normalized = normalize(request);
        int updated = jdbcTemplate.update("""
                        UPDATE site_link
                        SET label = ?, url = ?, icon_key = ?, enabled = ?, update_time = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                normalized.label(), normalized.url(), normalized.iconKey(),
                normalized.enabled() ? 1 : 0, id);
        if (updated != 1) throw new IllegalArgumentException("站点链接不存在");
        return require(id);
    }

    @Transactional
    public List<SiteLink> reorder(List<Long> orderedIds) {
        List<SiteLink> existing = listAll();
        List<Long> ids = orderedIds == null ? List.of() : List.copyOf(orderedIds);
        Set<Long> unique = new HashSet<>(ids);
        Set<Long> expected = existing.stream().map(SiteLink::id).collect(java.util.stream.Collectors.toSet());
        if (ids.size() != existing.size() || unique.size() != ids.size() || !unique.equals(expected)) {
            throw new IllegalArgumentException("链接顺序必须包含全部现有链接且不能重复");
        }
        for (int index = 0; index < ids.size(); index++) {
            jdbcTemplate.update("""
                    UPDATE site_link SET sort_order = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?
                    """, index, ids.get(index));
        }
        return listAll();
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (jdbcTemplate.update("DELETE FROM site_link WHERE id = ?", id) != 1) {
            throw new IllegalArgumentException("站点链接不存在");
        }
        compactOrder();
    }

    private void compactOrder() {
        List<Long> ids = jdbcTemplate.query("SELECT id FROM site_link ORDER BY sort_order, id",
                (result, row) -> result.getLong(1));
        for (int index = 0; index < ids.size(); index++) {
            jdbcTemplate.update("UPDATE site_link SET sort_order = ? WHERE id = ?", index, ids.get(index));
        }
    }

    private SiteLink require(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("站点链接不存在");
        List<SiteLink> links = jdbcTemplate.query("""
                        SELECT %s FROM site_link WHERE id = ?
                        """.formatted(SELECT_COLUMNS),
                (result, row) -> map(result), id);
        if (links.isEmpty()) throw new IllegalArgumentException("站点链接不存在");
        return links.get(0);
    }

    private Normalized normalize(Request request) {
        if (request == null) throw new IllegalArgumentException("链接内容不能为空");
        String label = limited(request.label(), "链接名称", 30);
        if (label.isBlank()) throw new IllegalArgumentException("链接名称不能为空");
        String url = limited(request.url(), "链接地址", 500);
        if (!safeUrl(url)) throw new IllegalArgumentException("链接仅支持 HTTPS 地址或 mailto 邮箱");
        String iconKey = limited(request.iconKey(), "链接图标", 30);
        if (iconKey.isBlank()) iconKey = "link";
        return new Normalized(label, url, iconKey, request.enabled() == null || request.enabled());
    }

    private boolean safeUrl(String value) {
        if (value.startsWith("mailto:")) {
            return value.substring("mailto:".length()).matches(EMAIL_PATTERN);
        }
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String limited(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(label + "内容过长");
        return normalized;
    }

    private SiteLink map(java.sql.ResultSet result) throws java.sql.SQLException {
        return new SiteLink(
                result.getLong("id"),
                result.getString("label"),
                result.getString("url"),
                result.getString("icon_key"),
                result.getInt("enabled") == 1,
                result.getInt("sort_order"),
                parse(result.getString("create_time")),
                parse(result.getString("update_time")));
    }

    private LocalDateTime parse(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value.replace(' ', 'T'));
    }

    public record Request(String label, String url, String iconKey, Boolean enabled) {
    }

    private record Normalized(String label, String url, String iconKey, boolean enabled) {
    }
}
