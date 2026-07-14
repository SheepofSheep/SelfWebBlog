package org.example.selfwebblog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.selfwebblog.entity.Comment;
import org.example.selfwebblog.mapper.CommentMapper;
import org.example.selfwebblog.service.CommentService;
import org.springframework.stereotype.Service;

/**
 * 评论服务实现类
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    
    @Override
    public Page<Comment> getCommentsByPostId(Long postId, int pageNum, int pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, "PUBLISHED")
                .orderByDesc(Comment::getPinned)
                .orderByDesc(Comment::getCreateTime);
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Page<Comment> listAll(int pageNum, int pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<Comment>()
                .orderByDesc(Comment::getCreateTime);
        return baseMapper.selectPage(page, queryWrapper);
    }
}
