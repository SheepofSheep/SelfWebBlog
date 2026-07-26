package org.example.selfwebblog.interaction.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record AdminInteractionResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String targetType,
        @JsonSerialize(using = ToStringSerializer.class) Long targetId,
        @JsonSerialize(using = ToStringSerializer.class) Long rootId,
        @JsonSerialize(using = ToStringSerializer.class) Long replyToId,
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
        LocalDateTime createTime) {
}
