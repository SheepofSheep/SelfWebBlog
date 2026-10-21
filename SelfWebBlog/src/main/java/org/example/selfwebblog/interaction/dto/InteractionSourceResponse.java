package org.example.selfwebblog.interaction.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionSourceResponse {
    private String targetType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;
    private String sourceTitle;
    private String sourceStatus;
    private long rootCount;
    private long replyCount;
    private long pendingCount;
    private LocalDateTime latestActivity;
}
