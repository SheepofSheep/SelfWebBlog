package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.UserService;
import org.example.selfwebblog.service.upload.StoredUpload;
import org.example.selfwebblog.service.upload.UploadStorageService;
import org.example.selfwebblog.service.upload.UploadTarget;
import org.example.selfwebblog.security.ActionRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.time.Duration;

@RestController
@RequestMapping({"/user", "/api/user"})
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UploadStorageService uploadStorageService;
    private final ActionRateLimiter rateLimiter;

    public UserController(UserService userService, UploadStorageService uploadStorageService,
                          ActionRateLimiter rateLimiter) {
        this.userService = userService;
        this.uploadStorageService = uploadStorageService;
        this.rateLimiter = rateLimiter;
    }

    // ==================== 更新个人资料 ====================

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = AuthHelper.getUserId(request);
        if (userId == null) return Result.unauthorized("请先登录");

        User user = userService.getById(userId);
        if (user == null) return Result.notFound("用户不存在");

        if (body.containsKey("nickname")) {
            String nickname = body.get("nickname").trim();
            if (nickname.length() > 30) return Result.badRequest("昵称不能超过30个字符");
            user.setNickname(nickname);
        }
        if (body.containsKey("avatarUrl")) {
            user.setAvatarUrl(body.get("avatarUrl").trim());
        }

        userService.updateById(user);
        log.info("用户 {} 更新了个人资料", user.getUsername());

        return Result.success(Map.of(
            "id", String.valueOf(user.getId()),
            "nickname", user.getNickname() != null ? user.getNickname() : "",
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
        ));
    }

    // ==================== 上传头像 ====================

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = AuthHelper.getUserId(request);
        if (userId == null) return Result.unauthorized("请先登录");
        ActionRateLimiter.Decision decision = rateLimiter.acquire(
                "avatar-upload", "user:" + userId, 10, Duration.ofHours(1));
        if (!decision.allowed()) {
            return Result.rateLimited("头像上传太频繁，请稍后再试", decision.retryAfterSeconds());
        }

        User currentUser = userService.getById(userId);
        if (currentUser == null) return Result.notFound("用户不存在");
        String previousAvatar = currentUser.getAvatarUrl();

        try {
            StoredUpload upload = uploadStorageService.storeImage(file, UploadTarget.AVATAR);
            String fileUrl = upload.url();
            log.info("用户 {} 上传头像: {}", userId, fileUrl);

            // 更新用户头像
            currentUser.setAvatarUrl(fileUrl);
            userService.updateById(currentUser);
            uploadStorageService.deleteManagedAvatar(previousAvatar);
            log.info("用户 {} 头像已更新: {}", currentUser.getUsername(), fileUrl);

            return Result.success(fileUrl);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (IOException e) {
            log.error("用户头像上传失败", e);
            return Result.serverError("上传失败，请稍后重试");
        }
    }
}
