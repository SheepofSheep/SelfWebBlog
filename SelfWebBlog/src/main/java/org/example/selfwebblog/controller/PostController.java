package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.content.tag.TagService;
import org.example.selfwebblog.analytics.EngagementMapper;
import org.example.selfwebblog.interaction.mapper.InteractionMapper;
import org.example.selfwebblog.interaction.model.Interaction;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@RestController
@RequestMapping({"/posts", "/api/posts"})
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final PostService postService;
    private final TagService tagService;
    private final InteractionMapper interactionMapper;
    private final EngagementMapper engagementMapper;

    public PostController(PostService postService) {
        this(postService, null, null, null);
    }

    @Autowired
    public PostController(PostService postService, TagService tagService,
                          InteractionMapper interactionMapper, EngagementMapper engagementMapper) {
        this.postService = postService;
        this.tagService = tagService;
        this.interactionMapper = interactionMapper;
        this.engagementMapper = engagementMapper;
    }

    @GetMapping
    public Result<Page<Post>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginationPolicy.PageRequest request = PaginationPolicy.require(page, size);
        return Result.success(postService.listByPage(request.page(), request.size()));
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
    @AdminOnly(action = "POST_SAVE")
    @Transactional
    public Result<String> saveOrUpdate(@Valid @RequestBody Post post, HttpServletRequest request) {
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
            syncTags(existing);
            log.info("更新文章 ID:{} status:{}", existing.getId(), existing.getStatus());
            return Result.success("修改成功");
        }
        post.setUpdateTime(LocalDateTime.now());
        postService.save(post);
        syncTags(post);
        log.info("创建文章 status:{}", post.getStatus());
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    @AdminOnly(action = "POST_DELETE")
    @Transactional
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        if (postService.getById(id) == null) return Result.notFound("删除失败，ID不存在: " + id);
        cleanupPostReferences(id);
        boolean removed = postService.removeById(id);
        log.info("删除文章 ID:{}", id);
        return removed ? Result.success("删除成功") : Result.notFound("删除失败，ID不存在: " + id);
    }

    @PostMapping("/batch-delete")
    @AdminOnly(action = "POST_BATCH_DELETE")
    @Transactional
    public Result<Integer> batchDelete(@RequestBody BatchIdsRequest body) {
        List<Long> ids = normalizedIds(body == null ? null : body.ids());
        for (Long id : ids) {
            if (postService.getById(id) == null) return Result.notFound("文章不存在: " + id);
        }
        ids.forEach(this::cleanupPostReferences);
        postService.removeByIds(ids);
        log.info("批量删除文章 count:{}", ids.size());
        return Result.success(ids.size());
    }

    @GetMapping("/drafts")
    @AdminOnly
    public Result<Page<Post>> listDrafts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(page, size);
        return Result.success(postService.listDrafts(pagination.page(), pagination.size()));
    }

    @GetMapping("/search")
    public Result<Page<Post>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginationPolicy.PageRequest request = PaginationPolicy.require(page, size);
        return Result.success(postService.search(
                keyword, category, tag, sort, request.page(), request.size()));
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

    private List<Long> normalizedIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("至少选择一篇文章");
        List<Long> normalized = new LinkedHashSet<>(ids).stream()
                .filter(id -> id != null && id > 0)
                .toList();
        if (normalized.isEmpty() || normalized.size() > 100) {
            throw new IllegalArgumentException("单次可处理 1 到 100 篇文章");
        }
        return normalized;
    }

    private void syncTags(Post post) {
        if (tagService != null) tagService.replacePostTags(post.getId(), post.getTags());
    }

    private void cleanupPostReferences(Long postId) {
        if (engagementMapper != null) {
            engagementMapper.deleteCommentLikesForPost(postId);
            engagementMapper.deletePostLikes(postId);
            engagementMapper.deletePostViews(postId);
        }
        if (interactionMapper != null) {
            interactionMapper.delete(new LambdaQueryWrapper<Interaction>()
                    .eq(Interaction::getTargetType, "POST")
                    .eq(Interaction::getTargetId, postId));
        }
        if (tagService != null) tagService.replacePostTags(postId, "");
    }

    public record BatchIdsRequest(List<Long> ids) {}
}
