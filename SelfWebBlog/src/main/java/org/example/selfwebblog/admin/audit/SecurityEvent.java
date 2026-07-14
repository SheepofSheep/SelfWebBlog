package org.example.selfwebblog.admin.audit;

import java.time.LocalDateTime;

public record SecurityEvent(
        Long id,
        Long actorUserId,
        String action,
        String targetType,
        String targetId,
        String detail,
        String ipHash,
        LocalDateTime createTime) {
}
