package org.example.selfwebblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("blog_info")
public class BlogInfo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String avatarUrl;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String bio;

    private String bgUrl;

    private LocalDate siteStartDate;

    private String currentStatus;

    private LocalDateTime statusUpdatedTime;

    private String aboutMarkdown;

    private String socialLinks;
}
