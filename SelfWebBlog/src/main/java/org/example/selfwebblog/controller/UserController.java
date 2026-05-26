package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final Map<String, Set<String>> MAGIC_BYTES = Map.of(
        "jpg", Set.of("FFD8FF"),
        "jpeg", Set.of("FFD8FF"),
        "png", Set.of("89504E47"),
        "gif", Set.of("47494638"),
        "webp", Set.of("52494646")
    );

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${app.frontend-url:http://localhost:5174}")
    private String frontendUrl;

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ==================== 更新个人资料 ====================

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = AuthHelper.getUserId(request);
        if (userId == null) return Result.error("请先登录");

        User user = userService.getById(userId);
        if (user == null) return Result.error("用户不存在");

        if (body.containsKey("nickname")) {
            String nickname = body.get("nickname").trim();
            if (nickname.length() > 30) return Result.error("昵称不能超过30个字符");
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
        if (userId == null) return Result.error("请先登录");

        if (file == null || file.isEmpty()) return Result.error("请选择文件");

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return Result.error("无效的文件名");
            }

            String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            String detectedExt = detectImageType(file);
            if (detectedExt == null) return Result.error("仅支持 jpg、png、gif、webp 格式");
            suffix = "." + detectedExt;

            String avatarDir = uploadPath.replace("\\", "/") + "/avatars/";
            File dir = new File(avatarDir);
            if (!dir.exists()) dir.mkdirs();

            String newFilename = UUID.randomUUID().toString() + suffix;
            File dest = new File(avatarDir + newFilename);
            file.transferTo(dest);
            log.info("用户 {} 上传头像: {}", userId, dest.getAbsolutePath());

            String fileUrl = "/uploads/avatars/" + newFilename;
            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("用户头像上传失败", e);
            return Result.error("上传失败，请稍后重试");
        }
    }

    private String detectImageType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = in.read(header);
            if (read < 3) return null;

            String hex = HexFormat.of().withUpperCase().formatHex(header);
            for (var entry : MAGIC_BYTES.entrySet()) {
                for (String magic : entry.getValue()) {
                    if (hex.startsWith(magic)) {
                        if ("webp".equals(entry.getKey())) {
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
            String riff = new String(header, 0, 4);
            String webp = new String(header, 8, 4);
            return "RIFF".equals(riff) && "WEBP".equals(webp);
        } catch (IOException e) {
            return false;
        }
    }
}
