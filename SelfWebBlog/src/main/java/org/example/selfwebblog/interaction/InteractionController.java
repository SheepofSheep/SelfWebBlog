package org.example.selfwebblog.interaction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.controller.AuthHelper;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.interaction.dto.InteractionCreateRequest;
import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.example.selfwebblog.interaction.dto.InteractionReplyRequest;
import org.example.selfwebblog.interaction.dto.InteractionThreadResponse;
import org.example.selfwebblog.interaction.model.Interaction;
import org.example.selfwebblog.security.ClientIpResolver;
import org.example.selfwebblog.service.CommentRateLimiter;
import org.example.selfwebblog.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {
    private final InteractionService interactionService;
    private final UserService userService;
    private final ClientIpResolver clientIpResolver;
    private final CommentRateLimiter rateLimiter;

    public InteractionController(
            InteractionService interactionService,
            UserService userService,
            ClientIpResolver clientIpResolver,
            CommentRateLimiter rateLimiter) {
        this.interactionService = interactionService;
        this.userService = userService;
        this.clientIpResolver = clientIpResolver;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/{targetType}/{targetId}")
    public Result<InteractionThreadResponse> getThread(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(page, size);
        return Result.success(interactionService.getThread(
                targetType,
                targetId,
                pagination.page(),
                pagination.size(),
                AuthHelper.getUserId(request),
                false));
    }

    @PostMapping
    public Result<InteractionItemResponse> create(
            @Valid @RequestBody InteractionCreateRequest body,
            HttpServletRequest request) {
        Long userId = AuthHelper.getUserId(request);
        if ("POST".equalsIgnoreCase(body.getTargetType()) && userId == null) {
            return Result.unauthorized("文章评论需要登录");
        }
        User actor = userId == null ? null : userService.getById(userId);
        if (userId != null && actor == null) return Result.unauthorized("登录状态已失效");

        String ipAddress = clientIpResolver.resolve(request);
        String action = "GUESTBOOK".equalsIgnoreCase(body.getTargetType()) ? "guestbook" : "comment";
        if (!rateLimiter.tryAcquire(action, userId, ipAddress)) {
            return Result.rateLimited("发送太频繁了，请稍后再试", 60);
        }
        return Result.success(interactionService.create(body, actor, ipAddress, false));
    }

    @PostMapping("/{id}/replies")
    public Result<InteractionItemResponse> reply(
            @PathVariable Long id,
            @Valid @RequestBody InteractionReplyRequest body,
            HttpServletRequest request) {
        Long userId = AuthHelper.getUserId(request);
        if (userId == null) return Result.unauthorized("回复需要登录");
        User actor = userService.getById(userId);
        if (actor == null) return Result.unauthorized("登录状态已失效");
        Interaction parent = interactionService.requireInteraction(id);

        InteractionCreateRequest create = new InteractionCreateRequest();
        create.setTargetType(parent.getTargetType());
        create.setTargetId(parent.getTargetId());
        create.setReplyToId(id);
        create.setContent(body.getContent());
        String ipAddress = clientIpResolver.resolve(request);
        if (!rateLimiter.tryAcquire("reply", userId, ipAddress)) {
            return Result.rateLimited("回复太频繁了，请稍后再试", 60);
        }
        return Result.success(interactionService.create(create, actor, ipAddress, false));
    }
}
