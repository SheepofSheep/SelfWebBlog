package org.example.selfwebblog.interaction.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Interaction {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long postId;
    private String targetType;
    private Long targetId;
    private Long rootId;
    private Long replyToId;
    private Long replyToUserId;
    private String content;
    private Long userId;
    private String nickname;
    private String guestName;
    private String avatarUrl;
    private String role;
    private Integer pinned;
    private String titleName;
    private String titleStyle;
    private String status;
    private String ipCiphertext;
    private String ipHash;
    private String ipRegion;
    private Integer likeCount;
    private LocalDateTime editedTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
