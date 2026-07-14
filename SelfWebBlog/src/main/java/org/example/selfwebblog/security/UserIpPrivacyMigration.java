package org.example.selfwebblog.security;

import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class UserIpPrivacyMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;
    private final VisitorIdentityService visitorIdentityService;

    public UserIpPrivacyMigration(JdbcTemplate jdbcTemplate, VisitorIdentityService visitorIdentityService) {
        this.jdbcTemplate = jdbcTemplate;
        this.visitorIdentityService = visitorIdentityService;
    }

    @Override
    public void run(String... args) {
        var legacyRows = jdbcTemplate.query("""
                SELECT id, ip_address FROM user
                WHERE ip_address IS NOT NULL AND TRIM(ip_address) <> ''
                """, (result, row) -> new LegacyIp(result.getLong("id"), result.getString("ip_address")));
        for (LegacyIp row : legacyRows) {
            jdbcTemplate.update("""
                    UPDATE user
                    SET registration_ip_hash = ?, ip_address = ''
                    WHERE id = ?
                    """, visitorIdentityService.hash(row.ipAddress().trim()), row.id());
        }
    }

    private record LegacyIp(long id, String ipAddress) {
    }
}
