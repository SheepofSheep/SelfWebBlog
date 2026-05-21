package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/posts")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Result<Page<Post>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(postService.listByPage(page, size));
    }

    @GetMapping("/{id}")
    public Result<Post> getById(@PathVariable Long id) {
        return Result.success(postService.getById(id));
    }

    @PostMapping
    public Result<String> saveOrUpdate(@Valid @RequestBody Post post, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限操作");
        if (post.getId() != null) {
            postService.updateById(post);
            return Result.success("修改成功");
        }
        postService.save(post);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限操作");
        boolean removed = postService.removeById(id);
        log.info("删除文章 ID:{}", id);
        return removed ? Result.success("删除成功") : Result.error("删除失败，ID不存在: " + id);
    }
}