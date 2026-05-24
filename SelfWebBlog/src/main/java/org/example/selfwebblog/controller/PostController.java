package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.service.impl.PostServiceImpl;
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

import java.time.LocalDateTime;

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

        if (post.getStatus() == null || post.getStatus().isBlank()) {
            post.setStatus("PUBLISHED");
        }

        if (post.getId() != null) {
            post.setUpdateTime(LocalDateTime.now());
            postService.updateById(post);
            log.info("更新文章 ID:{} status:{}", post.getId(), post.getStatus());
            return Result.success("修改成功");
        }
        post.setUpdateTime(LocalDateTime.now());
        postService.save(post);
        log.info("创建文章 status:{}", post.getStatus());
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限操作");
        boolean removed = postService.removeById(id);
        log.info("删除文章 ID:{}", id);
        return removed ? Result.success("删除成功") : Result.error("删除失败，ID不存在: " + id);
    }

    @GetMapping("/drafts")
    public Result<Page<Post>> listDrafts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限操作");
        PostServiceImpl impl = (PostServiceImpl) postService;
        return Result.success(impl.listDrafts(page, size));
    }

    @GetMapping("/search")
    public Result<Page<Post>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PostServiceImpl impl = (PostServiceImpl) postService;
        return Result.success(impl.search(keyword, category, tag, sort, page, size));
    }

    @GetMapping("/categories")
    public Result<java.util.List<String>> listCategories() {
        PostServiceImpl impl = (PostServiceImpl) postService;
        return Result.success(impl.allCategories());
    }
}
