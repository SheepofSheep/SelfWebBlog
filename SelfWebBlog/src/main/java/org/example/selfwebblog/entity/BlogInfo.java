package org.example.selfwebblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
}
