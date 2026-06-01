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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/posts")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

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
    public Result<Post> getById(@PathVariable Long id, HttpServletRequest request) {
        Post post = postService.getById(id);
        if (post == null) {
            return Result.notFound("文章不存在");
        }
        if (isDraft(post) && !AuthHelper.isAdmin(request)) {
            return Result.notFound("文章不存在");
        }
        return Result.success(post);
    }

    @PostMapping
    public Result<String> saveOrUpdate(@Valid @RequestBody Post post, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限操作");

        String normalizedStatus = normalizeStatus(post.getStatus());
        if (!isValidStatus(normalizedStatus)) {
            return Result.badRequest("文章状态不合法");
        }
        post.setStatus(normalizedStatus);

        if (post.getId() != null) {
            Post existing = postService.getById(post.getId());
            if (existing == null) {
                return Result.notFound("文章不存在");
            }
            mergePost(existing, post);
            existing.setUpdateTime(LocalDateTime.now());
            postService.updateById(existing);
            log.info("更新文章 ID:{} status:{}", existing.getId(), existing.getStatus());
            return Result.success("修改成功");
        }
        post.setUpdateTime(LocalDateTime.now());
        postService.save(post);
        log.info("创建文章 status:{}", post.getStatus());
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限操作");
        boolean removed = postService.removeById(id);
        log.info("删除文章 ID:{}", id);
        return removed ? Result.success("删除成功") : Result.notFound("删除失败，ID不存在: " + id);
    }

    @GetMapping("/drafts")
    public Result<Page<Post>> listDrafts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限操作");
        return Result.success(postService.listDrafts(page, size));
    }

    @GetMapping("/search")
    public Result<Page<Post>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(postService.search(keyword, category, tag, sort, page, size));
    }

    @GetMapping("/categories")
    public Result<java.util.List<String>> listCategories() {
        return Result.success(postService.allCategories());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_PUBLISHED;
        }
        return status.trim().toUpperCase();
    }

    private boolean isValidStatus(String status) {
        return STATUS_DRAFT.equals(status) || STATUS_PUBLISHED.equals(status);
    }

    private boolean isDraft(Post post) {
        return post != null && STATUS_DRAFT.equalsIgnoreCase(post.getStatus());
    }

    private void mergePost(Post existing, Post incoming) {
        existing.setTitle(incoming.getTitle());
        existing.setContent(incoming.getContent());
        if (incoming.getSummary() != null) existing.setSummary(incoming.getSummary());
        if (incoming.getCoverUrl() != null) existing.setCoverUrl(incoming.getCoverUrl());
        if (incoming.getCategory() != null) existing.setCategory(incoming.getCategory());
        if (incoming.getTags() != null) existing.setTags(incoming.getTags());
        if (incoming.getImageUrl() != null) existing.setImageUrl(incoming.getImageUrl());
        existing.setStatus(incoming.getStatus());
    }
}
