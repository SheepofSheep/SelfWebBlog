package org.example.selfwebblog.interaction;

import org.example.selfwebblog.analytics.EngagementMapper;
import org.example.selfwebblog.content.post.PostStatusPolicy;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class LikeService {
    private final EngagementMapper mapper;
    private final PostService postService;

    public LikeService(EngagementMapper mapper, PostService postService) {
        this.mapper = mapper;
        this.postService = postService;
    }

    @Transactional
    public LikeState setLiked(String rawTargetType, Long targetId, Long userId, boolean liked) {
        String targetType = normalizeTargetType(rawTargetType);
        if (targetId == null || targetId <= 0 || userId == null) {
            throw new IllegalArgumentException("点赞目标无效");
        }
        mapper.acquireWriteLock();
        requirePublishedPost(targetType, targetId);
        if (mapper.readLikeCount(targetType, targetId) == null) {
            throw new IllegalArgumentException("点赞目标不存在");
        }
        if (liked) {
            if (mapper.insertLike(targetType, targetId, userId) == 1) {
                if (mapper.incrementLikeCount(targetType, targetId) != 1) {
                    throw new IllegalArgumentException("点赞目标不存在");
                }
            }
        } else if (mapper.deleteLike(targetType, targetId, userId) == 1) {
            if (mapper.decrementLikeCount(targetType, targetId) != 1) {
                throw new IllegalArgumentException("点赞目标不存在");
            }
        }
        Integer count = mapper.readLikeCount(targetType, targetId);
        if (count == null) throw new IllegalArgumentException("点赞目标不存在");
        return new LikeState(liked, Math.max(0, count));
    }

    @Transactional(readOnly = true)
    public LikeState state(String rawTargetType, Long targetId, Long userId) {
        String targetType = normalizeTargetType(rawTargetType);
        if (targetId == null || targetId <= 0) throw new IllegalArgumentException("点赞目标无效");
        requirePublishedPost(targetType, targetId);
        Integer count = mapper.readLikeCount(targetType, targetId);
        if (count == null) throw new IllegalArgumentException("点赞目标不存在");
        boolean liked = userId != null && mapper.hasLike(targetType, targetId, userId) > 0;
        return new LikeState(liked, Math.max(0, count));
    }

    private String normalizeTargetType(String targetType) {
        String normalized = targetType == null ? "" : targetType.toUpperCase(Locale.ROOT);
        if (!Set.of("POST", "COMMENT").contains(normalized)) {
            throw new IllegalArgumentException("不支持的点赞目标");
        }
        return normalized;
    }

    private void requirePublishedPost(String targetType, Long targetId) {
        if (!"POST".equals(targetType)) return;
        if (targetId == null || targetId <= 0) throw new IllegalArgumentException("点赞目标无效");
        Post post = postService.getById(targetId);
        if (post == null || !PostStatusPolicy.isPublic(post.getStatus())) {
            throw new IllegalArgumentException("点赞目标不存在");
        }
    }

    public record LikeState(boolean liked, int count) {
    }
}
