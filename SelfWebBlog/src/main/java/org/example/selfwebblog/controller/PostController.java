package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.content.tag.TagService;
import org.example.selfwebblog.content.post.PostContentPolicy;
import org.example.selfwebblog.content.post.PostStatusPolicy;
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
import org.springframework.web.bind.annotation.PutMapping;
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
        if (!PostStatusPolicy.isPublic(post.getStatus()) && !AuthHelper.isAdmin(request)) {
            return Result.notFound("文章不存在");
        }
        return Result.success(post);
    }

    @PostMapping
    @AdminOnly(action = "POST_SAVE")
    @Transactional
    public Result<String> saveOrUpdate(@Valid @RequestBody Post post, HttpServletRequest request) {
        String normalizedStatus = normalizeStatus(post.getStatus());
        if (!PostStatusPolicy.isValid(normalizedStatus)) {
            return Result.badRequest("文章状态不合法");
        }
        post.setStatus(normalizedStatus);
        post.setTitle(value(post.getTitle()));
        post.setContent(value(post.getContent()));
        PostContentPolicy.requirePublishable(normalizedStatus, post.getContent());
        normalizePresentation(post);

        if (post.getId() != null) {
            Post existing = postService.getById(post.getId());
            if (existing == null) {
                return Result.notFound("文章不存在");
            }
            if (!PostStatusPolicy.canTransition(existing.getStatus(), normalizedStatus)) {
                return Result.badRequest("文章状态不能这样变更");
            }
            String previousStatus = PostStatusPolicy.normalize(existing.getStatus());
            LocalDateTime now = LocalDateTime.now();
            mergePost(existing, post);
            ensurePublicationTime(existing, previousStatus, normalizedStatus, now);
            existing.setUpdateTime(now);
            postService.updateById(existing);
            syncTags(existing);
            log.info("更新文章 ID:{} status:{}", existing.getId(), existing.getStatus());
            return Result.success("修改成功");
        }
        if (!PostStatusPolicy.canCreate(normalizedStatus)) {
            return Result.badRequest("新文章不能直接归档");
        }
        LocalDateTime now = LocalDateTime.now();
        ensurePublicationTime(post, "", normalizedStatus, now);
        post.setUpdateTime(now);
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

    @GetMapping("/admin")
    @AdminOnly
    public Result<Page<Post>> listForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String normalizedStatus = PostStatusPolicy.normalize(status);
        if (!normalizedStatus.isBlank() && !PostStatusPolicy.isValid(normalizedStatus)) {
            return Result.badRequest("文章状态不合法");
        }
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(page, size);
        return Result.success(postService.listForAdmin(
                normalizedStatus, keyword, pagination.page(), pagination.size()));
    }

    @PutMapping("/{id}/status")
    @AdminOnly(action = "POST_STATUS_UPDATE")
    @Transactional
    public Result<Post> updateStatus(@PathVariable Long id, @RequestBody StatusChangeRequest body) {
        Post existing = postService.getById(id);
        if (existing == null) return Result.notFound("文章不存在");
        String target = PostStatusPolicy.normalize(body == null ? null : body.status());
        if (!PostStatusPolicy.canTransition(existing.getStatus(), target)) {
            return Result.badRequest("文章状态不能这样变更");
        }
        applyStatus(existing, target);
        postService.updateById(existing);
        return Result.success(existing);
    }

    @PutMapping("/batch/status")
    @AdminOnly(action = "POST_BATCH_STATUS_UPDATE")
    @Transactional
    public Result<Integer> batchUpdateStatus(@RequestBody BatchStatusRequest body) {
        List<Long> ids = normalizedIds(body == null ? null : body.ids());
        String target = PostStatusPolicy.normalize(body == null ? null : body.status());
        List<Post> posts = ids.stream().map(postService::getById).toList();
        for (int index = 0; index < posts.size(); index++) {
            Post post = posts.get(index);
            if (post == null) return Result.notFound("文章不存在: " + ids.get(index));
            if (!PostStatusPolicy.canTransition(post.getStatus(), target)) {
                return Result.badRequest("所选文章包含不能变更为该状态的内容");
            }
        }
        for (Post post : posts) {
            applyStatus(post, target);
            postService.updateById(post);
        }
        return Result.success(posts.size());
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
            return PostStatusPolicy.PUBLISHED;
        }
        return PostStatusPolicy.normalize(status);
    }

    private void mergePost(Post existing, Post incoming) {
        existing.setTitle(incoming.getTitle());
        existing.setContent(incoming.getContent());
        if (incoming.getSummary() != null) existing.setSummary(incoming.getSummary());
        if (incoming.getCoverUrl() != null) existing.setCoverUrl(incoming.getCoverUrl());
        if (incoming.getCategory() != null) existing.setCategory(incoming.getCategory());
        if (incoming.getTags() != null) existing.setTags(incoming.getTags());
        if (incoming.getImageUrl() != null) existing.setImageUrl(incoming.getImageUrl());
        if (incoming.getBackgroundMode() != null) existing.setBackgroundMode(incoming.getBackgroundMode());
        if (incoming.getBackgroundValue() != null) existing.setBackgroundValue(incoming.getBackgroundValue());
        if (incoming.getBackgroundFocusX() != null) existing.setBackgroundFocusX(incoming.getBackgroundFocusX());
        if (incoming.getBackgroundFocusY() != null) existing.setBackgroundFocusY(incoming.getBackgroundFocusY());
        existing.setStatus(incoming.getStatus());
        if (PostStatusPolicy.ARCHIVED.equals(incoming.getStatus()) && existing.getArchivedTime() == null) {
            existing.setArchivedTime(LocalDateTime.now());
        } else if (!PostStatusPolicy.ARCHIVED.equals(incoming.getStatus())) {
            existing.setArchivedTime(null);
        }
    }

    private void applyStatus(Post post, String status) {
        PostContentPolicy.requirePublishable(status, post.getContent());
        String previousStatus = PostStatusPolicy.normalize(post.getStatus());
        LocalDateTime now = LocalDateTime.now();
        post.setStatus(status);
        ensurePublicationTime(post, previousStatus, status, now);
        if (PostStatusPolicy.ARCHIVED.equals(status)) {
            if (post.getArchivedTime() == null) post.setArchivedTime(now);
        } else {
            post.setArchivedTime(null);
        }
        post.setUpdateTime(now);
    }

    private void ensurePublicationTime(
            Post post, String previousStatus, String targetStatus, LocalDateTime now) {
        if (post.getPublishedTime() != null
                || (!PostStatusPolicy.PUBLISHED.equals(targetStatus)
                && !PostStatusPolicy.ARCHIVED.equals(targetStatus))) {
            return;
        }
        boolean firstPublication = PostStatusPolicy.DRAFT.equals(previousStatus)
                && PostStatusPolicy.PUBLISHED.equals(targetStatus);
        post.setPublishedTime(firstPublication || post.getCreateTime() == null
                ? now
                : post.getCreateTime());
    }

    private void normalizePresentation(Post post) {
        String mode = value(post.getBackgroundMode()).trim().toUpperCase();
        if (mode.isBlank()) mode = "COVER";
        if (!List.of("COVER", "CUSTOM_IMAGE", "GRADIENT", "SOLID", "SITE_DEFAULT", "NONE").contains(mode)) {
            throw new IllegalArgumentException("文章背景模式不合法");
        }
        String backgroundValue = value(post.getBackgroundValue()).trim();
        if (!isValidBackgroundValue(mode, backgroundValue)) {
            throw new IllegalArgumentException("文章背景配置不合法");
        }
        post.setBackgroundMode(mode);
        post.setBackgroundValue(backgroundValue);
        post.setBackgroundFocusX(clamp(post.getBackgroundFocusX(), 50));
        post.setBackgroundFocusY(clamp(post.getBackgroundFocusY(), 50));
    }

    private boolean isValidBackgroundValue(String mode, String backgroundValue) {
        return switch (mode) {
            case "CUSTOM_IMAGE" -> backgroundValue.matches("(?i)^(?:https?://|/uploads/|/api/media/)[^\\s<>]+$");
            case "GRADIENT" -> backgroundValue.matches("^(?:tide|mist|dusk|leaf|ember)$");
            case "SOLID" -> backgroundValue.matches("^#[0-9a-fA-F]{6}$");
            default -> backgroundValue.isBlank();
        };
    }

    private int clamp(Integer value, int fallback) {
        if (value == null) return fallback;
        return Math.max(0, Math.min(100, value));
    }

    private String value(String value) {
        return value == null ? "" : value;
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

    public record StatusChangeRequest(String status) {}

    public record BatchStatusRequest(List<Long> ids, String status) {}
}
