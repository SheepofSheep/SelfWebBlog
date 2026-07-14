package org.example.selfwebblog.interaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.identity.captcha.CaptchaService;
import org.example.selfwebblog.interaction.dto.InteractionCreateRequest;
import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.example.selfwebblog.interaction.dto.InteractionThreadResponse;
import org.example.selfwebblog.interaction.mapper.InteractionLikeMapper;
import org.example.selfwebblog.interaction.mapper.InteractionMapper;
import org.example.selfwebblog.interaction.model.Interaction;
import org.example.selfwebblog.interaction.model.InteractionLike;
import org.example.selfwebblog.interaction.security.IpCipher;
import org.example.selfwebblog.interaction.security.IpRegionResolver;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.example.selfwebblog.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InteractionService {
    private final InteractionMapper interactionMapper;
    private final InteractionLikeMapper likeMapper;
    private final PostService postService;
    private final CaptchaService captchaService;
    private final GuestTrustService guestTrustService;
    private final IpCipher ipCipher;
    private final VisitorIdentityService visitorIdentityService;
    private final IpRegionResolver ipRegionResolver;

    public InteractionService(
            InteractionMapper interactionMapper,
            InteractionLikeMapper likeMapper,
            PostService postService,
            CaptchaService captchaService,
            GuestTrustService guestTrustService,
            IpCipher ipCipher,
            VisitorIdentityService visitorIdentityService,
            IpRegionResolver ipRegionResolver) {
        this.interactionMapper = interactionMapper;
        this.likeMapper = likeMapper;
        this.postService = postService;
        this.captchaService = captchaService;
        this.guestTrustService = guestTrustService;
        this.ipCipher = ipCipher;
        this.visitorIdentityService = visitorIdentityService;
        this.ipRegionResolver = ipRegionResolver;
    }

    @Transactional
    public InteractionItemResponse create(
            InteractionCreateRequest request,
            User actor,
            String ipAddress,
            boolean rateSuspicious) {
        String targetType = normalizeTargetType(request.getTargetType());
        long targetId = request.getTargetId();
        validateTarget(targetType, targetId, actor);

        Interaction parent = null;
        if (request.getReplyToId() != null) {
            parent = requireInteraction(request.getReplyToId());
            requireSameTarget(parent, targetType, targetId);
        }

        String normalizedIp = ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress.trim();
        String ipHash = visitorIdentityService.hash(normalizedIp);
        Interaction interaction = new Interaction();
        interaction.setPostId("POST".equals(targetType) ? targetId : null);
        interaction.setTargetType(targetType);
        interaction.setTargetId(targetId);
        interaction.setContent(request.getContent().trim());
        interaction.setPinned(0);
        interaction.setLikeCount(0);
        interaction.setIpCiphertext(ipCipher.encrypt(normalizedIp));
        interaction.setIpHash(ipHash);
        interaction.setIpRegion(ipRegionResolver.resolve(normalizedIp));

        if (parent != null) {
            interaction.setReplyToId(parent.getId());
            interaction.setReplyToUserId(parent.getUserId());
            interaction.setRootId(resolveRootId(parent));
        }

        if (actor != null) {
            applyActor(interaction, actor);
            interaction.setStatus("PUBLISHED");
        } else {
            applyGuest(interaction, request, ipHash, rateSuspicious);
        }

        interactionMapper.insert(interaction);
        return toResponse(interaction, false);
    }

    public InteractionThreadResponse getThread(
            String rawTargetType,
            Long targetId,
            int page,
            int size,
            Long viewerId,
            boolean includeModeration) {
        String targetType = normalizeTargetType(rawTargetType);
        LambdaQueryWrapper<Interaction> rootsQuery = targetQuery(targetType, targetId, includeModeration)
                .isNull(Interaction::getRootId)
                .orderByDesc(Interaction::getPinned)
                .orderByDesc(Interaction::getCreateTime);
        Page<Interaction> roots = interactionMapper.selectPage(new Page<>(page, size), rootsQuery);
        if (roots.getRecords().isEmpty()) {
            return new InteractionThreadResponse(List.of(), roots.getTotal(), page, size);
        }

        List<Long> rootIds = roots.getRecords().stream().map(Interaction::getId).toList();
        LambdaQueryWrapper<Interaction> repliesQuery = new LambdaQueryWrapper<Interaction>()
                .in(Interaction::getRootId, rootIds)
                .orderByAsc(Interaction::getCreateTime);
        if (!includeModeration) repliesQuery.eq(Interaction::getStatus, "PUBLISHED");
        List<Interaction> replies = interactionMapper.selectList(repliesQuery);

        Set<Long> likedIds = likedCommentIds(viewerId, roots.getRecords(), replies);
        Map<Long, List<InteractionItemResponse>> repliesByRoot = new HashMap<>();
        for (Interaction reply : replies) {
            repliesByRoot.computeIfAbsent(reply.getRootId(), ignored -> new ArrayList<>())
                    .add(toResponse(reply, likedIds.contains(reply.getId())));
        }
        List<InteractionItemResponse> items = roots.getRecords().stream().map(root -> {
            InteractionItemResponse response = toResponse(root, likedIds.contains(root.getId()));
            response.setReplies(repliesByRoot.getOrDefault(root.getId(), List.of()));
            return response;
        }).toList();
        return new InteractionThreadResponse(items, roots.getTotal(), page, size);
    }

    public Page<Interaction> listForModeration(String status, int page, int size) {
        LambdaQueryWrapper<Interaction> query = new LambdaQueryWrapper<Interaction>()
                .orderByDesc(Interaction::getCreateTime);
        if (status != null && !status.isBlank()) query.eq(Interaction::getStatus, status.toUpperCase(Locale.ROOT));
        return interactionMapper.selectPage(new Page<>(page, size), query);
    }

    @Transactional
    public InteractionItemResponse moderate(Long id, String nextStatus) {
        Interaction interaction = requireInteraction(id);
        String normalized = nextStatus == null ? "" : nextStatus.toUpperCase(Locale.ROOT);
        if (!InteractionPolicy.canTransition(interaction.getStatus(), normalized)) {
            throw new IllegalArgumentException("不允许的审核状态变更");
        }
        interaction.setStatus(normalized);
        interactionMapper.updateById(interaction);
        return toResponse(interaction, false);
    }

    public Interaction requireInteraction(Long id) {
        Interaction interaction = interactionMapper.selectById(id);
        if (interaction == null) throw new IllegalArgumentException("评论不存在");
        return interaction;
    }

    static long resolveRootId(Interaction parent) {
        return parent.getRootId() == null ? parent.getId() : parent.getRootId();
    }

    static void requireSameTarget(Interaction parent, String targetType, Long targetId) {
        if (!Objects.equals(parent.getTargetType(), targetType)
                || !Objects.equals(parent.getTargetId(), targetId)) {
            throw new IllegalArgumentException("不能跨内容回复");
        }
    }

    private void validateTarget(String targetType, Long targetId, User actor) {
        if (targetId == null || targetId <= 0) throw new IllegalArgumentException("目标不存在");
        if ("POST".equals(targetType)) {
            if (actor == null) throw new IllegalArgumentException("文章评论需要登录");
            Post post = postService.getById(targetId);
            if (post == null || "DRAFT".equalsIgnoreCase(post.getStatus())) {
                throw new IllegalArgumentException("文章不存在或未发布");
            }
        } else if (targetId != 1L) {
            throw new IllegalArgumentException("留言板目标无效");
        }
    }

    private void applyActor(Interaction interaction, User actor) {
        interaction.setUserId(actor.getId());
        interaction.setNickname(actor.getNickname() == null || actor.getNickname().isBlank()
                ? actor.getUsername() : actor.getNickname());
        interaction.setAvatarUrl(actor.getAvatarUrl());
        interaction.setRole(actor.getRole());
        interaction.setTitleName(actor.getTitleName());
        interaction.setTitleStyle(actor.getTitleStyle());
    }

    private void applyGuest(
            Interaction interaction,
            InteractionCreateRequest request,
            String ipHash,
            boolean rateSuspicious) {
        if (!"GUESTBOOK".equals(interaction.getTargetType())) {
            throw new IllegalArgumentException("此处不允许匿名评论");
        }
        String guestName = request.getGuestName() == null ? "" : request.getGuestName().trim();
        if (guestName.length() < 2 || guestName.length() > 30) {
            throw new IllegalArgumentException("昵称需要 2 到 30 个字符");
        }
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer(), "guestbook")) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        boolean previouslyApproved = interactionMapper.selectCount(new LambdaQueryWrapper<Interaction>()
                .eq(Interaction::getIpHash, ipHash)
                .eq(Interaction::getStatus, "PUBLISHED")) > 0;
        interaction.setGuestName(guestName);
        interaction.setNickname(guestName);
        interaction.setRole("GUEST");
        interaction.setTitleName("");
        interaction.setTitleStyle("default");
        interaction.setStatus(guestTrustService.statusFor(
                previouslyApproved, interaction.getContent(), rateSuspicious));
    }

    private LambdaQueryWrapper<Interaction> targetQuery(
            String targetType, Long targetId, boolean includeModeration) {
        LambdaQueryWrapper<Interaction> query = new LambdaQueryWrapper<Interaction>()
                .eq(Interaction::getTargetType, targetType)
                .eq(Interaction::getTargetId, targetId);
        if (!includeModeration) query.eq(Interaction::getStatus, "PUBLISHED");
        return query;
    }

    private Set<Long> likedCommentIds(
            Long viewerId, List<Interaction> roots, List<Interaction> replies) {
        if (viewerId == null) return Set.of();
        Set<Long> ids = new HashSet<>();
        roots.forEach(item -> ids.add(item.getId()));
        replies.forEach(item -> ids.add(item.getId()));
        if (ids.isEmpty()) return Set.of();
        return likeMapper.selectList(new LambdaQueryWrapper<InteractionLike>()
                        .eq(InteractionLike::getTargetType, "COMMENT")
                        .eq(InteractionLike::getUserId, viewerId)
                        .in(InteractionLike::getTargetId, ids))
                .stream()
                .map(InteractionLike::getTargetId)
                .collect(Collectors.toSet());
    }

    private String normalizeTargetType(String targetType) {
        String normalized = targetType == null ? "" : targetType.toUpperCase(Locale.ROOT);
        if (!Set.of("POST", "GUESTBOOK").contains(normalized)) {
            throw new IllegalArgumentException("不支持的互动目标");
        }
        return normalized;
    }

    private InteractionItemResponse toResponse(Interaction interaction, boolean liked) {
        InteractionItemResponse response = new InteractionItemResponse();
        response.setId(interaction.getId());
        response.setTargetType(interaction.getTargetType());
        response.setTargetId(interaction.getTargetId());
        response.setRootId(interaction.getRootId());
        response.setReplyToId(interaction.getReplyToId());
        response.setContent(interaction.getContent());
        response.setUserId(interaction.getUserId());
        response.setNickname(interaction.getNickname());
        response.setAvatarUrl(interaction.getAvatarUrl());
        response.setRole(interaction.getRole());
        response.setTitleName(interaction.getTitleName());
        response.setTitleStyle(interaction.getTitleStyle());
        response.setIpRegion(interaction.getIpRegion());
        response.setStatus(interaction.getStatus());
        response.setLikeCount(interaction.getLikeCount() == null ? 0 : interaction.getLikeCount());
        response.setLiked(liked);
        response.setPinned(Integer.valueOf(1).equals(interaction.getPinned()));
        response.setEditedTime(interaction.getEditedTime());
        response.setCreateTime(interaction.getCreateTime());
        return response;
    }
}
