package org.example.selfwebblog.interaction.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;

public record AdminInteractionResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String targetType,
        @JsonSerialize(using = ToStringSerializer.class) Long targetId,
        String sourceTitle,
        @JsonSerialize(using = ToStringSerializer.class) Long rootId,
        @JsonSerialize(using = ToStringSerializer.class) Long replyToId,
        String replyToNickname,
        String content,
        @JsonSerialize(using = ToStringSerializer.class) Long userId,
        String nickname,
        String avatarUrl,
        String role,
        String titleName,
        String titleStyle,
        String status,
        String ipRegion,
        int likeCount,
        boolean pinned,
        Integer pinOrder,
        LocalDateTime pinnedAt,
        LocalDateTime createTime,
        int replyCount,
        List<AdminInteractionResponse> replies) {
}
