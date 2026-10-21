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
                .eq(Comment::getTargetType, "POST")
                .eq(Comment::getTargetId, postId)
                .eq(Comment::getStatus, "PUBLISHED")
                .and(visible -> visible.isNull(Comment::getRootId).or().exists("""
                        SELECT 1 FROM comment root
                        WHERE root.id = comment.root_id AND root.root_id IS NULL
                          AND root.status = 'PUBLISHED' AND root.target_type = 'POST'
                          AND root.post_id = {0} AND root.target_id = {0}
                        """, postId))
                .orderByDesc(Comment::getPinned)
                .orderByDesc(Comment::getCreateTime)
                .orderByDesc(Comment::getId);
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
