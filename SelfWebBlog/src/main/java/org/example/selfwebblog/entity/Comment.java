package org.example.selfwebblog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @NotNull(message = "文章ID不能为空")
    private Long postId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private String nickname;
    private String avatarUrl;
    private String role;
    private String titleName;
    private String titleStyle;

    private Integer pinned;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
