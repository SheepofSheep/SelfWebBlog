package org.example.selfwebblog.controller;

import org.example.selfwebblog.admin.security.AdminOnly;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.entity.Comment;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.CommentRateLimiter;
import org.example.selfwebblog.service.CommentService;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.service.UserService;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.interaction.InteractionService;
import org.example.selfwebblog.interaction.dto.InteractionCreateRequest;
import org.example.selfwebblog.security.ClientIpResolver;
import org.example.selfwebblog.entity.BlogInfo;
import org.example.selfwebblog.entity.Post;
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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/comments", "/api/comments"})
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);
    private static final int MAX_COMMENT_LENGTH = 1000;

    private final CommentService commentService;
    private final UserService userService;
    private final PostService postService;
    private final CommentRateLimiter commentRateLimiter;
    private final InteractionService interactionService;
    private final ClientIpResolver clientIpResolver;

    private final BlogInfoService blogInfoService;

    public CommentController(CommentService commentService, UserService userService, BlogInfoService blogInfoService, PostService postService, CommentRateLimiter commentRateLimiter) {
        this(commentService, userService, blogInfoService, postService, commentRateLimiter, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CommentController(CommentService commentService, UserService userService, BlogInfoService blogInfoService,
                             PostService postService, CommentRateLimiter commentRateLimiter,
                             InteractionService interactionService, ClientIpResolver clientIpResolver) {
        this.commentService = commentService;
        this.userService = userService;
        this.blogInfoService = blogInfoService;
        this.postService = postService;
        this.commentRateLimiter = commentRateLimiter;
        this.interactionService = interactionService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/post/{postId}")
    public Result<Page<Comment>> getCommentsByPostId(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(pageNum, pageSize);
        Page<Comment> comments = commentService.getCommentsByPostId(
                postId, pagination.page(), pagination.size());
        // 博主评论同步当前头像
        BlogInfo blogInfo = blogInfoService.getById(1);
        String adminAvatar = blogInfo != null ? blogInfo.getAvatarUrl() : null;
        Map<Long, User> usersById = userService.listByIds(
                        comments.getRecords().stream()
                                .filter(comment -> !"ADMIN".equals(comment.getRole()))
                                .map(Comment::getUserId)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 评论展示信息使用当前资料，避免逐条查询数据库。
        for (Comment c : comments.getRecords()) {
            if ("ADMIN".equals(c.getRole())) {
                if (adminAvatar != null && !adminAvatar.isEmpty()) {
                    c.setAvatarUrl(adminAvatar);
                }
            } else if (c.getUserId() != null) {
                User commentUser = usersById.get(c.getUserId());
                if (commentUser != null) {
                    String name = (commentUser.getNickname() != null && !commentUser.getNickname().isBlank())
                            ? commentUser.getNickname() : commentUser.getUsername();
                    c.setNickname(name);
                    c.setAvatarUrl(commentUser.getAvatarUrl() != null ? commentUser.getAvatarUrl() : "");
                }
            }
        }
        return Result.success(comments);
    }

    @PostMapping
    public Result<String> addComment(@Valid @RequestBody Comment comment, HttpServletRequest request) {
        Long userId = AuthHelper.getUserId(request);
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.notFound("用户不存在");
        }

        Post post = postService.getById(comment.getPostId());
        if (post == null || "DRAFT".equalsIgnoreCase(post.getStatus())) {
            return Result.notFound("文章不存在或未发布");
        }
        if (comment.getContent() != null && comment.getContent().trim().length() > MAX_COMMENT_LENGTH) {
            return Result.badRequest("评论不能超过1000个字符");
        }
        String clientIp = clientIpResolver == null ? request.getRemoteAddr() : clientIpResolver.resolve(request);
        if (!commentRateLimiter.tryAcquire(userId, clientIp)) {
            return Result.rateLimited("评论太频繁了，请稍后再试");
        }

        if (interactionService != null) {
            InteractionCreateRequest create = new InteractionCreateRequest();
            create.setTargetType("POST");
            create.setTargetId(comment.getPostId());
            create.setContent(comment.getContent());
            interactionService.create(create, user, clientIp, false);
            log.info("评论成功：{}", user.getUsername());
            return Result.success("评论成功");
        }

        String displayName = (user.getNickname() != null && !user.getNickname().isBlank())
                ? user.getNickname() : user.getUsername();
        comment.setUserId(user.getId());
        comment.setNickname(displayName);
        comment.setAvatarUrl(user.getAvatarUrl());
        comment.setRole(user.getRole());
        comment.setTitleName(user.getTitleName());
        comment.setTitleStyle(user.getTitleStyle());
        comment.setTargetType("POST");
        comment.setTargetId(comment.getPostId());
        comment.setStatus("PUBLISHED");
        comment.setLikeCount(0);
        commentService.save(comment);
        log.info("评论成功：{}", user.getUsername());
        return Result.success("评论成功");
    }

    @GetMapping("/all")
    @AdminOnly
    public Result<Page<Comment>> listAll(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(pageNum, pageSize);
        return Result.success(commentService.listAll(pagination.page(), pagination.size()));
    }

    @PutMapping("/{id}/pin")
    @AdminOnly(action = "LEGACY_COMMENT_PIN")
    public Result<String> togglePin(@PathVariable Long id, HttpServletRequest request) {
        if (interactionService != null) {
            var updated = interactionService.requireInteraction(id);
            interactionService.setPinned(id, !Integer.valueOf(1).equals(updated.getPinned()));
            return Result.success(Integer.valueOf(1).equals(updated.getPinned()) ? "已取消置顶" : "已置顶");
        }
        Comment comment = commentService.getById(id);
        if (comment == null) return Result.notFound("评论不存在");
        comment.setPinned(comment.getPinned() != null && comment.getPinned() == 1 ? 0 : 1);
        commentService.updateById(comment);
        return Result.success(comment.getPinned() == 1 ? "已置顶" : "已取消置顶");
    }

    @DeleteMapping("/{id}")
    @AdminOnly(action = "LEGACY_COMMENT_DELETE")
    public Result<String> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        if (interactionService != null) {
            interactionService.softDelete(id);
            log.info("软删除评论成功：ID={}", id);
            return Result.success("删除成功");
        }
        boolean removed = commentService.removeById(id);
        if (removed) {
            log.info("删除评论成功：ID={}", id);
            return Result.success("删除成功");
        }
        return Result.notFound("评论不存在");
    }
}
