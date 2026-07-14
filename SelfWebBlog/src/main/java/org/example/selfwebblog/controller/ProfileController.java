package org.example.selfwebblog.controller;

import org.example.selfwebblog.admin.security.AdminOnly;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.entity.BlogInfo;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/profile", "/api/profile"})
public class ProfileController {

    private final BlogInfoService blogInfoService;
    private final PostService postService;
    private final UserService userService;
    private final String adminUsername;

    public ProfileController(
            BlogInfoService blogInfoService,
            PostService postService,
            UserService userService,
            @Value("${auth.admin.username:admin}") String adminUsername) {
        this.blogInfoService = blogInfoService;
        this.postService = postService;
        this.userService = userService;
        this.adminUsername = adminUsername;
    }

    // 获取个人主页数据，支持可选分页
    @GetMapping
    public Result<Map<String, Object>> getProfile(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Map<String, Object> data = new HashMap<>();

        BlogInfo blogInfo = blogInfoService.getBlogInfo();
        data.put("blogInfo", blogInfo);

        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(
                page == null ? 1 : page,
                size == null ? 20 : size);
        Page<Post> postPage = postService.listByPage(pagination.page(), pagination.size());
        data.put("posts", postPage.getRecords());
        long total = postPage.getTotal() > 0 ? postPage.getTotal() : postService.count();
        data.put("total", total);
        data.put("pages", Math.max(1, (int) Math.ceil((double) total / pagination.size())));

        return Result.success(data);
    }

    // 更新个人信息（只更新非空字段，避免清空未传入的字段）
    @PostMapping("/update")
    @AdminOnly(action = "SITE_PROFILE_UPDATE")
    public Result<Void> updateProfile(@RequestBody Map<String, Object> body, HttpServletRequest request) {

        BlogInfo existing = blogInfoService.getBlogInfo();

        if (body.containsKey("nickname")) {
            String nickname = (String) body.get("nickname");
            if (nickname != null && !nickname.trim().isEmpty()) {
                existing.setNickname(nickname.trim());
            }
        }
        if (body.containsKey("bio")) {
            existing.setBio((String) body.get("bio"));
        }
        if (body.containsKey("avatarUrl")) {
            String avatarUrl = (String) body.get("avatarUrl");
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                existing.setAvatarUrl(avatarUrl.trim());
            }
        }
        if (body.containsKey("bgUrl")) {
            existing.setBgUrl((String) body.get("bgUrl"));
        }

        blogInfoService.updateById(existing);

        // 同步更新 admin 用户记录，确保 /auth/me 返回最新头像和昵称
        User adminUser = userService.getByUsername(adminUsername);
        if (adminUser != null) {
            if (body.containsKey("nickname") && existing.getNickname() != null) {
                adminUser.setNickname(existing.getNickname());
            }
            if (body.containsKey("avatarUrl") && existing.getAvatarUrl() != null) {
                adminUser.setAvatarUrl(existing.getAvatarUrl());
            }
            userService.updateById(adminUser);
        }

        return Result.success();
    }

    // 更新背景图
    @PostMapping("/background")
    @AdminOnly(action = "SITE_BACKGROUND_UPDATE")
    public Result<Void> updateBackground(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String bgUrl = body.get("bgUrl");
        blogInfoService.updateBackground(bgUrl);
        return Result.success();
    }

}
