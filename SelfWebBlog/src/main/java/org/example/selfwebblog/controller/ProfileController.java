package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.selfwebblog.controller.AuthHelper;
import org.example.selfwebblog.entity.BlogInfo;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.service.PostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final BlogInfoService blogInfoService;
    private final PostService postService;

    public ProfileController(BlogInfoService blogInfoService, PostService postService) {
        this.blogInfoService = blogInfoService;
        this.postService = postService;
    }

    // 获取个人主页数据，支持可选分页
    @GetMapping
    public Result<Map<String, Object>> getProfile(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Map<String, Object> data = new HashMap<>();

        BlogInfo blogInfo = blogInfoService.getBlogInfo();
        data.put("blogInfo", blogInfo);

        if (page != null && size != null) {
            Page<Post> postPage = postService.listByPage(page, size);
            data.put("posts", postPage.getRecords());
            long total = postPage.getTotal() > 0 ? postPage.getTotal() : postService.count();
            data.put("total", total);
            data.put("pages", Math.max(1, (int) Math.ceil((double) total / size)));
        } else {
            data.put("posts", postService.list());
        }

        return Result.success(data);
    }

    // 更新个人信息（统一接口）
    @PostMapping("/update")
    public Result<Void> updateProfile(@Valid @RequestBody BlogInfo blogInfo, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限操作");
        blogInfo.setId(1);
        blogInfoService.updateById(blogInfo);
        return Result.success();
    }

    // 更新背景图
    @PostMapping("/background")
    public Result<Void> updateBackground(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限操作");
        String bgUrl = body.get("bgUrl");
        blogInfoService.updateBackground(bgUrl);
        return Result.success();
    }

}
