package org.example.selfwebblog.controller;

import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.service.upload.StoredUpload;
import org.example.selfwebblog.service.upload.UploadStorageService;
import org.example.selfwebblog.service.upload.UploadTarget;
import org.example.selfwebblog.upload.asset.ImageAssetResponse;
import org.example.selfwebblog.upload.asset.ImageAssetService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping({"/upload", "/api/upload"})
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final BlogInfoService blogInfoService;
    private final UploadStorageService uploadStorageService;
    private final ImageAssetService imageAssetService;

    public UploadController(
        BlogInfoService blogInfoService,
        UploadStorageService uploadStorageService,
        ImageAssetService imageAssetService
    ) {
        this.blogInfoService = blogInfoService;
        this.uploadStorageService = uploadStorageService;
        this.imageAssetService = imageAssetService;
    }

    @PostMapping("/image")
    @AdminOnly(action = "ASSET_UPLOAD")
    public Result<ImageAssetResponse> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            return Result.success(imageAssetService.storeArticleImage(file, AuthHelper.getUserId(request)));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (IOException e) {
            log.error("文章图片上传失败", e);
            return Result.serverError("上传失败，请稍后重试");
        }
    }

    @GetMapping("/assets")
    @AdminOnly
    public Result<List<ImageAssetResponse>> listAssets(HttpServletRequest request) {
        return Result.success(imageAssetService.listArticleAssets(AuthHelper.getUserId(request)));
    }

    @PostMapping("/avatar")
    @AdminOnly(action = "AVATAR_UPLOAD")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Result<String> result = uploadFile(file, UploadTarget.AVATAR);
        if (result.getCode() == 200) {
            String avatarUrl = result.getData();
            blogInfoService.updateAvatar(avatarUrl);
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
