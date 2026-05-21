package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.service.BlogInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    // 已知图片类型的 magic bytes（前几个字节）
    private static final Map<String, Set<String>> MAGIC_BYTES = Map.of(
        "jpg", Set.of("FFD8FF"),
        "jpeg", Set.of("FFD8FF"),
        "png", Set.of("89504E47"),
        "gif", Set.of("47494638"),
        "webp", Set.of("52494646") // RIFF....WEBP
    );

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${app.frontend-url:http://localhost:5174}")
    private String frontendUrl;

    private final BlogInfoService blogInfoService;

    public UploadController(BlogInfoService blogInfoService) {
        this.blogInfoService = blogInfoService;
    }

    private String getUploadDir() {
        return uploadPath.replace("\\", "/") + "/";
    }

    private String getAvatarDir() {
        return uploadPath.replace("\\", "/") + "/avatars/";
    }

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) {
            return Result.error("无权限操作");
        }
        return uploadFile(file, getUploadDir(), "/uploads/", request);
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) {
            return Result.error("无权限操作");
        }
        Result<String> result = uploadFile(file, getAvatarDir(), "/uploads/avatars/", request);
        if (result.getCode() == 200) {
            blogInfoService.updateAvatar(result.getData());
        }
        return result;
    }

    private Result<String> uploadFile(MultipartFile file, String dir, String urlPrefix, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return Result.error("文件名格式不正确");
            }

            // 1. 通过 magic bytes 校验真实文件类型
            String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            String detectedExt = detectImageType(file, suffix);
            if (detectedExt == null) {
                return Result.error("不支持的文件格式，仅支持 jpg、png、gif、webp");
            }
            // 以 magic bytes 检测结果为准
            suffix = "." + detectedExt;

            // 2. 创建目录
            File uploadDir = new File(dir);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String newFilename = UUID.randomUUID().toString() + suffix;
            File dest = new File(dir + newFilename);
            file.transferTo(dest);
            log.info("文件保存成功: {}", dest.getAbsolutePath());

            // 3. 用配置的前端 URL 拼接（不信任 Host 头）
            String fileUrl = frontendUrl.replaceAll("/$", "") + urlPrefix + newFilename;
            return Result.success(fileUrl);

        } catch (IOException e) {
            log.error("上传失败", e);
            return Result.error("上传失败，请稍后重试");
        }
    }

    /**
     * 读取文件头 magic bytes 识别真实图片类型
     */
    private String detectImageType(MultipartFile file, String suffixClaimed) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = in.read(header);
            if (read < 3) return null;

            String hex = HexFormat.of().withUpperCase().formatHex(header);
            // 取前 6/8 位 hex 进行匹配
            for (var entry : MAGIC_BYTES.entrySet()) {
                for (String magic : entry.getValue()) {
                    if (hex.startsWith(magic)) {
                        // webp 需要额外检查：RIFF 后面 4 字节是文件大小，再后 4 字节是 "WEBP"
                        if ("webp".equals(entry.getKey())) {
                            byte[] extended = new byte[12];
                            // 重新读一次完整 12 字节
                            return checkWebp(file) ? "webp" : null;
                        }
                        return entry.getKey();
                    }
                }
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean checkWebp(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[12];
            if (in.read(header) < 12) return false;
            // RIFF(4) + size(4) + WEBP(4)
            String riff = new String(header, 0, 4);
            String webp = new String(header, 8, 4);
            return "RIFF".equals(riff) && "WEBP".equals(webp);
        } catch (IOException e) {
            return false;
        }
    }
}
