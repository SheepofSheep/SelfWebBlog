package org.example.selfwebblog.interaction.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interaction_like")
public class InteractionLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private Long targetId;
    private Long userId;
    private LocalDateTime createTime;
}
