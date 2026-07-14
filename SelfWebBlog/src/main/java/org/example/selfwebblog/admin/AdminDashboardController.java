package org.example.selfwebblog.admin;

import org.example.selfwebblog.admin.audit.SecurityAuditService;
import org.example.selfwebblog.admin.audit.SecurityEvent;
import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.entity.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@AdminOnly
public class AdminDashboardController {
    private final JdbcTemplate jdbcTemplate;
    private final SecurityAuditService auditService;

    public AdminDashboardController(JdbcTemplate jdbcTemplate, SecurityAuditService auditService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
    }

    @GetMapping("/overview")
    public Result<Overview> overview() {
        return Result.success(jdbcTemplate.queryForObject("""
                SELECT
                  (SELECT COUNT(*) FROM post WHERE status = 'PUBLISHED') AS published_count,
                  (SELECT COUNT(*) FROM post WHERE status = 'DRAFT') AS draft_count,
                  (SELECT COUNT(*) FROM comment WHERE status = 'PENDING') AS pending_count,
                  (SELECT COUNT(*) FROM user) AS user_count,
                  (SELECT COUNT(*) FROM image_asset) AS asset_count,
                  (SELECT COUNT(*) FROM image_asset WHERE reference_status = 'ORPHANED') AS orphaned_count,
                  MAX(
                    COALESCE((SELECT MAX(COALESCE(update_time, create_time)) FROM post), ''),
                    COALESCE((SELECT MAX(create_time) FROM comment), '')
                  ) AS latest_activity
                """, (result, row) -> new Overview(
                result.getLong("published_count"),
                result.getLong("draft_count"),
                result.getLong("pending_count"),
                result.getLong("user_count"),
                result.getLong("asset_count"),
                result.getLong("orphaned_count"),
                parse(result.getString("latest_activity")),
                "UP")));
    }

    @GetMapping("/security-events")
    public Result<List<SecurityEvent>> securityEvents(
            @RequestParam(defaultValue = "30") int limit) {
        return Result.success(auditService.recent(limit));
    }

    private LocalDateTime parse(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value.replace(' ', 'T'));
    }

    public record Overview(
            long publishedCount,
            long draftCount,
            long pendingCount,
            long userCount,
            long assetCount,
            long orphanedCount,
            LocalDateTime latestActivity,
            String databaseStatus) {
    }
}
