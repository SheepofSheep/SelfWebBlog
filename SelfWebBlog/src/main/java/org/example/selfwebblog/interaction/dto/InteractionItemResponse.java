package org.example.selfwebblog.interaction.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class InteractionItemResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String targetType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long rootId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyToId;
    private String content;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String titleName;
    private String titleStyle;
    private String ipRegion;
    private String status;
    private int likeCount;
    private boolean liked;
    private boolean pinned;
    private LocalDateTime editedTime;
    private LocalDateTime createTime;
    private List<InteractionItemResponse> replies = new ArrayList<>();
}
