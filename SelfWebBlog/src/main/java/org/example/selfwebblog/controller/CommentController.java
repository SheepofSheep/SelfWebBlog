package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.selfwebblog.entity.Comment;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.CommentService;
import org.example.selfwebblog.service.UserService;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.entity.BlogInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;
    private final UserService userService;

    private final BlogInfoService blogInfoService;

    public CommentController(CommentService commentService, UserService userService, BlogInfoService blogInfoService) {
        this.commentService = commentService;
        this.userService = userService;
        this.blogInfoService = blogInfoService;
    }

    @GetMapping("/post/{postId}")
    public Result<Page<Comment>> getCommentsByPostId(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Comment> comments = commentService.getCommentsByPostId(postId, pageNum, pageSize);
        // 管理员评论始终同步博主当前头像
        BlogInfo blogInfo = blogInfoService.getById(1);
        String adminAvatar = blogInfo != null ? blogInfo.getAvatarUrl() : null;
        if (adminAvatar != null && !adminAvatar.isEmpty()) {
            for (Comment c : comments.getRecords()) {
                if ("ADMIN".equals(c.getRole())) {
                    c.setAvatarUrl(adminAvatar);
                }
            }
        }
        return Result.success(comments);
    }

    @PostMapping
    public Result<String> addComment(@Valid @RequestBody Comment comment, HttpServletRequest request) {
        Long userId = AuthHelper.getUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        comment.setNickname(user.getUsername());
        comment.setAvatarUrl(user.getAvatarUrl());
        comment.setRole(user.getRole());
        comment.setTitleName(user.getTitleName());
        comment.setTitleStyle(user.getTitleStyle());
        commentService.save(comment);
        log.info("评论成功：{}", user.getUsername());
        return Result.success("评论成功");
    }

    @GetMapping("/all")
    public Result<Page<Comment>> listAll(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限");
        return Result.success(commentService.listAll(pageNum, pageSize));
    }

    @PutMapping("/{id}/pin")
    public Result<String> togglePin(@PathVariable Long id, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限");
        Comment comment = commentService.getById(id);
        if (comment == null) return Result.error("评论不存在");
        comment.setPinned(comment.getPinned() != null && comment.getPinned() == 1 ? 0 : 1);
        commentService.updateById(comment);
        return Result.success(comment.getPinned() == 1 ? "已置顶" : "已取消置顶");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) {
            return Result.error("无权限删除评论");
        }
        boolean removed = commentService.removeById(id);
        if (removed) {
            log.info("删除评论成功：ID={}", id);
            return Result.success("删除成功");
        }
        return Result.error("评论不存在");
    }
}
