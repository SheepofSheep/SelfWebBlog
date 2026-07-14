package org.example.selfwebblog.analytics;

import jakarta.servlet.http.HttpServletRequest;
import org.example.selfwebblog.controller.AuthHelper;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.interaction.LikeService;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.example.selfwebblog.security.ClientIpResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/engagement")
public class PostEngagementController {
    private final LikeService likeService;
    private final PostViewService postViewService;
    private final ClientIpResolver clientIpResolver;
    private final VisitorIdentityService visitorIdentityService;

    public PostEngagementController(
            LikeService likeService,
            PostViewService postViewService,
            ClientIpResolver clientIpResolver,
            VisitorIdentityService visitorIdentityService) {
        this.likeService = likeService;
        this.postViewService = postViewService;
        this.clientIpResolver = clientIpResolver;
        this.visitorIdentityService = visitorIdentityService;
    }

    @GetMapping("/{targetType}/{targetId}/like")
    public Result<LikeService.LikeState> likeState(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            HttpServletRequest request) {
        return Result.success(likeService.state(targetType, targetId, AuthHelper.getUserId(request)));
    }

    @PutMapping("/{targetType}/{targetId}/like")
    public Result<LikeService.LikeState> setLiked(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestBody Map<String, Boolean> body,
            HttpServletRequest request) {
        Long userId = AuthHelper.getUserId(request);
        if (userId == null) return Result.unauthorized("点赞需要登录");
        return Result.success(likeService.setLiked(
                targetType, targetId, userId, Boolean.TRUE.equals(body.get("liked"))));
    }

    @PostMapping("/posts/{postId}/view")
    public Result<Map<String, Boolean>> recordView(
            @PathVariable Long postId,
            HttpServletRequest request) {
        String source = clientIpResolver.resolve(request) + "|" +
                String.valueOf(request.getHeader("User-Agent"));
        boolean counted = postViewService.record(
                postId, visitorIdentityService.hash(source), LocalDate.now());
        return Result.success(Map.of("counted", counted));
    }
}
