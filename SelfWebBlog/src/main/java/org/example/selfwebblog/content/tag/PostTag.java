package org.example.selfwebblog.content.tag;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("post_tag")
public class PostTag {
    private Long postId;
    private Long tagId;
    private LocalDateTime createTime;
}
