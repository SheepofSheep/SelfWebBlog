package org.example.selfwebblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.selfwebblog.entity.Comment;

/**
 * 评论Mapper接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    // 继承BaseMapper，提供基本的CRUD操作
}
