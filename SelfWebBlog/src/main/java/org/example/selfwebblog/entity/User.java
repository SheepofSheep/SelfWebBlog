package org.example.selfwebblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;
    @JsonIgnore
    private String password;
    private String email;
    private String avatarUrl;
    private String role;
    private String githubId;
    private String ipAddress;
    private String titleName;
    private String titleStyle;
    private LocalDateTime createTime;
}
