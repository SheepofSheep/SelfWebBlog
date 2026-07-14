package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.service.UserService;
import org.example.selfwebblog.service.upload.StoredUpload;
import org.example.selfwebblog.service.upload.UploadStorageService;
import org.example.selfwebblog.service.upload.UploadTarget;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

@RestController
@RequestMapping({"/upload", "/api/upload"})
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final BlogInfoService blogInfoService;
    private final UserService userService;
    private final UploadStorageService uploadStorageService;
    private final String adminUsername;

    public UploadController(
        BlogInfoService blogInfoService,
        UserService userService,
        UploadStorageService uploadStorageService,
        @Value("${auth.admin.username:admin}") String adminUsername
    ) {
        this.blogInfoService = blogInfoService;
        this.userService = userService;
        this.uploadStorageService = uploadStorageService;
        this.adminUsername = adminUsername;
    }

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) {
            return Result.forbidden("无权限操作");
        }
        return uploadFile(file, UploadTarget.ARTICLE_IMAGE);
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) {
            return Result.forbidden("无权限操作");
        }
        Result<String> result = uploadFile(file, UploadTarget.AVATAR);
        if (result.getCode() == 200) {
            String avatarUrl = result.getData();
            blogInfoService.updateAvatar(avatarUrl);
            // 同步更新 admin 用户的头像，保持 /auth/me 和评论区一致
            User adminUser = userService.getByUsername(adminUsername);
            if (adminUser != null) {
                adminUser.setAvatarUrl(avatarUrl);
                userService.updateById(adminUser);
            }
        }
        return result;
    }

    private Result<String> uploadFile(MultipartFile file, UploadTarget target) {
        try {
            StoredUpload upload = uploadStorageService.storeImage(file, target);
            log.info("文件保存成功: {}", upload.url());
            return Result.success(upload.url());
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (IOException e) {
            log.error("上传失败", e);
            return Result.serverError("上传失败，请稍后重试");
        }
    }
}
