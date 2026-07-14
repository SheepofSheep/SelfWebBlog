package org.example.selfwebblog.interaction.dto;

import java.time.LocalDateTime;

public record AdminInteractionResponse(
        Long id,
        String targetType,
        Long targetId,
        Long rootId,
        Long replyToId,
        String content,
        Long userId,
        String nickname,
        String role,
        String status,
        String ipRegion,
        int likeCount,
        boolean pinned,
        LocalDateTime createTime) {
}
