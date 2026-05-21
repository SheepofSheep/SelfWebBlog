package org.example.selfwebblog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.entity.Comment;

public interface CommentService extends IService<Comment> {
    Page<Comment> getCommentsByPostId(Long postId, int pageNum, int pageSize);
    Page<Comment> listAll(int pageNum, int pageSize);
}
