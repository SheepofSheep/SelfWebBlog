package org.example.selfwebblog.admin.audit;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityAuditService {
    private final SecurityEventMapper mapper;

    public SecurityAuditService(SecurityEventMapper mapper) {
        this.mapper = mapper;
    }

    public void record(
            Long actorUserId,
            String action,
            String targetType,
            String targetId,
            String detail,
            String ipHash) {
        mapper.insert(new SecurityEventMapper.EventWrite(
                actorUserId,
                safe(action, 64),
                safe(targetType, 48),
                safe(targetId, 128),
                safe(detail, 500),
                safe(ipHash, 128)));
    }

    public List<SecurityEvent> recent(int limit) {
        return mapper.listRecent(Math.max(1, Math.min(limit, 100)));
    }

    private String safe(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
