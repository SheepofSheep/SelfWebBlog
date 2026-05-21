package org.example.selfwebblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.selfwebblog.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉 Spring 这是一个 Mapper
public interface PostMapper extends BaseMapper<Post> {
    // 继承了 BaseMapper 之后，增删改查的方法 MyBatis-Plus 都帮你写好了！
}