package org.example.selfwebblog.interaction;

import org.example.selfwebblog.analytics.EngagementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class LikeService {
    private final EngagementMapper mapper;

    public LikeService(EngagementMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public LikeState setLiked(String rawTargetType, Long targetId, Long userId, boolean liked) {
        String targetType = normalizeTargetType(rawTargetType);
        if (targetId == null || targetId <= 0 || userId == null) {
            throw new IllegalArgumentException("点赞目标无效");
        }
        if (mapper.readLikeCount(targetType, targetId) == null) {
            throw new IllegalArgumentException("点赞目标不存在");
        }
        if (liked) {
            if (mapper.insertLike(targetType, targetId, userId) == 1) {
                mapper.incrementLikeCount(targetType, targetId);
            }
        } else if (mapper.deleteLike(targetType, targetId, userId) == 1) {
            mapper.decrementLikeCount(targetType, targetId);
        }
        Integer count = mapper.readLikeCount(targetType, targetId);
        return new LikeState(liked, count == null ? 0 : Math.max(0, count));
    }

    public LikeState state(String rawTargetType, Long targetId, Long userId) {
        String targetType = normalizeTargetType(rawTargetType);
        Integer count = mapper.readLikeCount(targetType, targetId);
        boolean liked = userId != null && mapper.hasLike(targetType, targetId, userId) > 0;
        return new LikeState(liked, count == null ? 0 : Math.max(0, count));
    }

    private String normalizeTargetType(String targetType) {
        String normalized = targetType == null ? "" : targetType.toUpperCase(Locale.ROOT);
        if (!Set.of("POST", "COMMENT").contains(normalized)) {
            throw new IllegalArgumentException("不支持的点赞目标");
        }
        return normalized;
    }

    public record LikeState(boolean liked, int count) {
    }
}
