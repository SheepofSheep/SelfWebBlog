package org.example.selfwebblog.interaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.identity.captcha.CaptchaService;
import org.example.selfwebblog.interaction.dto.InteractionCreateRequest;
import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.example.selfwebblog.interaction.dto.AdminInteractionResponse;
import org.example.selfwebblog.interaction.dto.InteractionSourceResponse;
import org.example.selfwebblog.interaction.dto.InteractionThreadResponse;
import org.example.selfwebblog.interaction.mapper.InteractionLikeMapper;
import org.example.selfwebblog.interaction.mapper.InteractionMapper;
import org.example.selfwebblog.interaction.model.Interaction;
import org.example.selfwebblog.interaction.model.InteractionLike;
import org.example.selfwebblog.interaction.security.IpCipher;
import org.example.selfwebblog.interaction.security.IpRegionResolver;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private final UserService userService;

    public InteractionService(
            InteractionMapper interactionMapper,
            InteractionLikeMapper likeMapper,
            PostService postService,
            CaptchaService captchaService,
            GuestTrustService guestTrustService,
            IpCipher ipCipher,
            VisitorIdentityService visitorIdentityService,
            IpRegionResolver ipRegionResolver,
            UserService userService) {
        this.interactionMapper = interactionMapper;
        this.likeMapper = likeMapper;
        this.postService = postService;
        this.captchaService = captchaService;
        this.guestTrustService = guestTrustService;
        this.ipCipher = ipCipher;
        this.visitorIdentityService = visitorIdentityService;
        this.ipRegionResolver = ipRegionResolver;
        this.userService = userService;
    }

    @Transactional
    public InteractionItemResponse create(
            InteractionCreateRequest request,
            User actor,
            String ipAddress,
            boolean rateSuspicious) {
        String targetType = normalizeTargetType(request.getTargetType());
        long targetId = request.getTargetId();
        interactionMapper.acquireWriteLock();
        validateTarget(targetType, targetId, actor);

        Interaction parent = null;
        if (request.getReplyToId() != null) {
            requireAuthenticatedReply(actor, request.getReplyToId());
            parent = requireInteraction(request.getReplyToId());
            requireSameTarget(parent, targetType, targetId);
            requirePublishedReplyParent(parent, targetType, targetId);
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
        InteractionItemResponse response = toResponse(interaction, false);
        if (parent != null) {
            ProfileProjection target = projectProfiles(List.of(parent)).get(parent.getId());
            response.setReplyToNickname(target == null ? "已删除用户" : target.nickname());
        }
        return response;
    }

    public InteractionThreadResponse getThread(
            String rawTargetType,
            Long targetId,
            int page,
            int size,
            Long viewerId,
            boolean includeModeration) {
        String targetType = normalizeTargetType(rawTargetType);
        validateReadableTarget(targetType, targetId);
        LambdaQueryWrapper<Interaction> rootsQuery = targetQuery(targetType, targetId, includeModeration)
                .isNull(Interaction::getRootId)
                .orderByDesc(Interaction::getPinned)
                .orderByAsc(Interaction::getPinOrder)
                .orderByDesc(Interaction::getCreateTime)
                .orderByDesc(Interaction::getId);
        Page<Interaction> roots = interactionMapper.selectPage(new Page<>(page, size), rootsQuery);
        if (roots.getRecords().isEmpty()) {
            return new InteractionThreadResponse(List.of(), roots.getTotal(), page, size);
        }

        List<Long> rootIds = roots.getRecords().stream().map(Interaction::getId).toList();
        LambdaQueryWrapper<Interaction> repliesQuery = new LambdaQueryWrapper<Interaction>()
                .in(Interaction::getRootId, rootIds)
                .orderByAsc(Interaction::getCreateTime)
                .orderByAsc(Interaction::getId);
        if (!includeModeration) repliesQuery.eq(Interaction::getStatus, "PUBLISHED");
        List<Interaction> replies = interactionMapper.selectList(repliesQuery);

        Set<Long> likedIds = likedCommentIds(viewerId, roots.getRecords(), replies);
        List<Interaction> projectionSources = withReplyTargets(
                roots.getRecords(), replies, includeModeration);
        Map<Long, ProfileProjection> profiles = projectProfiles(projectionSources);
        Map<Long, List<InteractionItemResponse>> repliesByRoot = new HashMap<>();
        for (Interaction reply : replies) {
            repliesByRoot.computeIfAbsent(reply.getRootId(), ignored -> new ArrayList<>())
                    .add(toResponse(
                            reply,
                            likedIds.contains(reply.getId()),
                            profiles.get(reply.getId()),
                            replyTargetNickname(reply, profiles, includeModeration)));
        }
        List<InteractionItemResponse> items = roots.getRecords().stream().map(root -> {
            InteractionItemResponse response = toResponse(
                    root, likedIds.contains(root.getId()), profiles.get(root.getId()), null);
            response.setReplies(repliesByRoot.getOrDefault(root.getId(), List.of()));
            return response;
        }).toList();
        return new InteractionThreadResponse(items, roots.getTotal(), page, size);
    }

    public Page<AdminInteractionResponse> listForModeration(String status, int page, int size) {
        return listForModeration(status, null, null, null, page, size);
    }

    public Page<AdminInteractionResponse> listForModeration(
            String status,
            String targetType,
            Boolean pinned,
            String keyword,
            int page,
            int size) {
        return listForModeration(status, targetType, pinned, null, keyword, page, size);
    }

    public Page<AdminInteractionResponse> listForModeration(
            String status,
            String targetType,
            Boolean pinned,
            String contentType,
            String keyword,
            int page,
            int size) {
        return listForModeration(status, targetType, null, pinned, contentType, keyword, page, size);
    }

    public List<InteractionSourceResponse> listModerationSources() {
        return interactionMapper.listModerationSources();
    }

    public Page<AdminInteractionResponse> listForModeration(
            String status,
            String targetType,
            Long targetId,
            Boolean pinned,
            String contentType,
            String keyword,
            int page,
            int size) {
        if (targetId != null && (targetId <= 0 || targetType == null || targetType.isBlank())) {
            throw new IllegalArgumentException("筛选评论来源需要有效的目标类型和目标编号");
        }
        LambdaQueryWrapper<Interaction> query = new LambdaQueryWrapper<Interaction>()
                .isNull(Interaction::getRootId)
                .orderByDesc(Interaction::getPinned)
                .orderByAsc(Interaction::getPinOrder)
                .orderByDesc(Interaction::getCreateTime)
                .orderByDesc(Interaction::getId);
        String normalizedStatus = status == null || status.isBlank()
                ? null
                : status.trim().toUpperCase(Locale.ROOT);
        String normalizedContentType = normalizeContentType(contentType);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (targetType != null && !targetType.isBlank()) {
            query.eq(Interaction::getTargetType, normalizeTargetType(targetType));
        }
        if (targetId != null) query.eq(Interaction::getTargetId, targetId);
        if (pinned != null) query.eq(Interaction::getPinned, pinned ? 1 : 0);
        if (normalizedStatus != null || normalizedContentType != null || normalizedKeyword != null) {
            query.and(thread -> {
                applyModerationCandidateFilters(
                        thread, normalizedStatus, normalizedContentType, normalizedKeyword);
                List<Object> replyParameters = new ArrayList<>();
                thread.or().exists(buildReplyCandidateSql(
                        normalizedStatus,
                        normalizedContentType,
                        normalizedKeyword,
                        replyParameters),
                        replyParameters.toArray());
            });
        }
        Page<Interaction> entities = interactionMapper.selectPage(new Page<>(page, size), query);
        Page<AdminInteractionResponse> result = new Page<>(
                entities.getCurrent(), entities.getSize(), entities.getTotal());
        if (entities.getRecords().isEmpty()) {
            result.setRecords(List.of());
            return result;
        }

        List<Long> rootIds = entities.getRecords().stream().map(Interaction::getId).toList();
        List<Interaction> replies = interactionMapper.selectList(new LambdaQueryWrapper<Interaction>()
                .in(Interaction::getRootId, rootIds)
                .orderByAsc(Interaction::getCreateTime)
                .orderByAsc(Interaction::getId));
        if (replies == null) replies = List.of();

        List<Interaction> projectionSources = withReplyTargets(entities.getRecords(), replies, true);
        Map<Long, ProfileProjection> profiles = projectProfiles(projectionSources);
        Map<InteractionTarget, String> sourceTitles = projectSourceTitles(projectionSources);
        Map<Long, List<AdminInteractionResponse>> repliesByRoot = new HashMap<>();
        for (Interaction reply : replies) {
            repliesByRoot.computeIfAbsent(reply.getRootId(), ignored -> new ArrayList<>())
                    .add(toAdminResponse(
                            reply,
                            profiles.get(reply.getId()),
                            replyTargetNickname(reply, profiles, true),
                            sourceTitle(reply, sourceTitles),
                            0,
                            List.of()));
        }
        result.setRecords(entities.getRecords().stream()
                .map(root -> {
                    List<AdminInteractionResponse> rootReplies =
                            repliesByRoot.getOrDefault(root.getId(), List.of());
                    return toAdminResponse(
                            root,
                            profiles.get(root.getId()),
                            null,
                            sourceTitle(root, sourceTitles),
                            rootReplies.size(),
                            rootReplies);
                })
                .toList());
        return result;
    }

    private void applyContentTypeFilter(
            LambdaQueryWrapper<Interaction> query,
            String rawContentType) {
        String contentType = normalizeContentType(rawContentType);
        if (contentType == null) return;
        if ("STICKER".equals(contentType)) {
            query.and(content -> content
                    .like(Interaction::getContent, "[sticker:")
                    .or()
                    .like(Interaction::getContent, "](/images/emojis/"));
            return;
        }
        if ("TEXT".equals(contentType)) {
            query.notLike(Interaction::getContent, "[sticker:")
                    .notLike(Interaction::getContent, "](/images/emojis/");
            return;
        }
        throw new IllegalArgumentException("不支持的评论内容类型");
    }

    private String normalizeContentType(String rawContentType) {
        if (rawContentType == null || rawContentType.isBlank()) return null;
        String contentType = rawContentType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("STICKER", "TEXT").contains(contentType)) {
            throw new IllegalArgumentException("不支持的评论内容类型");
        }
        return contentType;
    }

    private void applyModerationCandidateFilters(
            LambdaQueryWrapper<Interaction> query,
            String status,
            String contentType,
            String keyword) {
        if (status != null) query.eq(Interaction::getStatus, status);
        applyContentTypeFilter(query, contentType);
        if (keyword == null) return;
        String pattern = "%" + keyword + "%";
        query.and(candidate -> candidate
                .like(Interaction::getContent, keyword)
                .or().like(Interaction::getNickname, keyword)
                .or().like(Interaction::getGuestName, keyword)
                .or().like(Interaction::getTitleName, keyword)
                .or().exists("""
                        SELECT 1 FROM user AS current_profile
                        WHERE current_profile.id = comment.user_id
                          AND (
                            current_profile.nickname LIKE {0}
                            OR current_profile.title_name LIKE {0}
                          )
                        """, pattern));
    }

    private String buildReplyCandidateSql(
            String status,
            String contentType,
            String keyword,
            List<Object> parameters) {
        StringBuilder sql = new StringBuilder("""
                SELECT 1
                FROM comment AS reply_filter
                LEFT JOIN user AS reply_profile ON reply_profile.id = reply_filter.user_id
                WHERE reply_filter.root_id = comment.id
                """);
        if (status != null) {
            sql.append(" AND reply_filter.status = ")
                    .append(addSqlParameter(parameters, status));
        }
        if ("STICKER".equals(contentType)) {
            sql.append(" AND (reply_filter.content LIKE ")
                    .append(addSqlParameter(parameters, "%[sticker:%"))
                    .append(" OR reply_filter.content LIKE ")
                    .append(addSqlParameter(parameters, "%](/images/emojis/%"))
                    .append(')');
        } else if ("TEXT".equals(contentType)) {
            sql.append(" AND reply_filter.content NOT LIKE ")
                    .append(addSqlParameter(parameters, "%[sticker:%"))
                    .append(" AND reply_filter.content NOT LIKE ")
                    .append(addSqlParameter(parameters, "%](/images/emojis/%"));
        }
        if (keyword != null) {
            String pattern = "%" + keyword + "%";
            String placeholder = addSqlParameter(parameters, pattern);
            sql.append(" AND (reply_filter.content LIKE ").append(placeholder)
                    .append(" OR reply_filter.nickname LIKE ").append(placeholder)
                    .append(" OR reply_filter.guest_name LIKE ").append(placeholder)
                    .append(" OR reply_filter.title_name LIKE ").append(placeholder)
                    .append(" OR reply_profile.nickname LIKE ").append(placeholder)
                    .append(" OR reply_profile.title_name LIKE ").append(placeholder)
                    .append(')');
        }
        return sql.toString();
    }

    private String addSqlParameter(List<Object> parameters, Object value) {
        int index = parameters.size();
        parameters.add(value);
        return "{" + index + "}";
    }

    public String revealIp(Long id) {
        Interaction interaction = requireInteraction(id);
        if (interaction.getIpCiphertext() == null || interaction.getIpCiphertext().isBlank()) return "unknown";
        return ipCipher.decrypt(interaction.getIpCiphertext());
    }

    @Transactional
    public InteractionItemResponse moderate(Long id, String nextStatus) {
        interactionMapper.acquireWriteLock();
        Interaction interaction = requireInteraction(id);
        String normalized = nextStatus == null ? "" : nextStatus.toUpperCase(Locale.ROOT);
        if (!InteractionPolicy.canTransition(interaction.getStatus(), normalized)) {
            throw new IllegalArgumentException("不允许的审核状态变更");
        }
        boolean clearPin = Integer.valueOf(1).equals(interaction.getPinned())
                && !"PUBLISHED".equals(normalized);
        interaction.setStatus(normalized);
        if (clearPin) clearPin(interaction);
        interactionMapper.updateById(interaction);
        if (clearPin) compactPinned(interaction.getTargetType(), interaction.getTargetId());
        return toResponse(interaction, false);
    }

    @Transactional
    public int moderateBatch(List<Long> ids, String nextStatus) {
        if (ids == null || ids.isEmpty() || ids.size() > 100) {
            throw new IllegalArgumentException("单次可处理 1 到 100 条互动");
        }
        List<Long> normalized = new LinkedHashSet<>(ids).stream()
                .filter(id -> id != null && id > 0)
                .toList();
        if (normalized.isEmpty()) throw new IllegalArgumentException("至少选择一条互动");
        interactionMapper.acquireWriteLock();
        for (Long id : normalized) moderate(id, nextStatus);
        return normalized.size();
    }

    @Transactional
    public InteractionItemResponse setPinned(Long id, boolean pinned) {
        interactionMapper.acquirePinMutationLock(id);
        Interaction interaction = requireInteraction(id);
        if (interaction.getRootId() != null) {
            throw new IllegalArgumentException("仅顶层互动可以置顶");
        }
        if (!"PUBLISHED".equals(interaction.getStatus())) {
            throw new IllegalArgumentException("仅已发布互动可以置顶");
        }
        boolean currentlyPinned = Integer.valueOf(1).equals(interaction.getPinned());
        if (currentlyPinned == pinned && (!pinned || interaction.getPinOrder() != null)) {
            return projectedResponse(interaction, false);
        }
        if (pinned) {
            List<Interaction> existing = compactPinned(interaction.getTargetType(), interaction.getTargetId());
            interaction.setPinned(1);
            interaction.setPinOrder(existing.size());
            interaction.setPinnedAt(LocalDateTime.now());
        } else {
            clearPin(interaction);
        }
        interactionMapper.updateById(interaction);
        if (!pinned) compactPinned(interaction.getTargetType(), interaction.getTargetId());
        return projectedResponse(interaction, false);
    }

    @Transactional
    public InteractionItemResponse movePinOrder(Long id, String rawDirection) {
        String direction = rawDirection == null ? "" : rawDirection.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("UP", "DOWN").contains(direction)) {
            throw new IllegalArgumentException("置顶移动方向无效");
        }
        interactionMapper.acquirePinMutationLock(id);
        Interaction interaction = requireInteraction(id);
        if (interaction.getRootId() != null || !Integer.valueOf(1).equals(interaction.getPinned())) {
            throw new IllegalArgumentException("仅置顶的顶层互动可以排序");
        }
        List<Interaction> ordered = compactPinned(interaction.getTargetType(), interaction.getTargetId());
        int index = -1;
        for (int current = 0; current < ordered.size(); current++) {
            if (Objects.equals(ordered.get(current).getId(), id)) {
                index = current;
                break;
            }
        }
        if (index < 0) throw new IllegalArgumentException("置顶互动不存在");
        int neighbourIndex = "UP".equals(direction) ? index - 1 : index + 1;
        Interaction target = ordered.get(index);
        if (neighbourIndex >= 0 && neighbourIndex < ordered.size()) {
            Interaction neighbour = ordered.get(neighbourIndex);
            int targetOrder = target.getPinOrder();
            int neighbourOrder = neighbour.getPinOrder();
            target.setPinOrder(-1);
            requirePinOrderUpdate(target);
            neighbour.setPinOrder(targetOrder);
            requirePinOrderUpdate(neighbour);
            target.setPinOrder(neighbourOrder);
            requirePinOrderUpdate(target);
        }
        return projectedResponse(target, false);
    }

    private void requirePinOrderUpdate(Interaction interaction) {
        if (interactionMapper.updateById(interaction) != 1) {
            throw new IllegalStateException("置顶顺序更新失败");
        }
    }

    @Transactional
    public void softDelete(Long id) {
        interactionMapper.acquireWriteLock();
        Interaction interaction = requireInteraction(id);
        requireDeletable(interaction);
        boolean clearPin = Integer.valueOf(1).equals(interaction.getPinned());
        interaction.setStatus("DELETED");
        clearPin(interaction);
        interactionMapper.updateById(interaction);
        if (clearPin) compactPinned(interaction.getTargetType(), interaction.getTargetId());
    }

    @Transactional
    public int softDeleteBatch(List<Long> ids) {
        List<Long> normalized = normalizeBatchIds(ids);
        interactionMapper.acquireWriteLock();
        List<Interaction> selected = interactionMapper.selectBatchIds(normalized);
        if (selected == null) selected = List.of();
        Map<Long, Interaction> byId = selected.stream()
                .filter(interaction -> interaction.getId() != null)
                .collect(Collectors.toMap(
                        Interaction::getId,
                        interaction -> interaction,
                        (first, ignored) -> first));
        if (!byId.keySet().containsAll(normalized)) {
            throw new IllegalArgumentException("所选评论包含不存在的记录");
        }
        List<Interaction> ordered = normalized.stream()
                .map(byId::get)
                .toList();
        ordered.forEach(this::requireDeletable);

        Set<InteractionTarget> pinsToCompact = new LinkedHashSet<>();
        for (Interaction interaction : ordered) {
            if (Integer.valueOf(1).equals(interaction.getPinned())) {
                pinsToCompact.add(new InteractionTarget(
                        interaction.getTargetType(), interaction.getTargetId()));
            }
            interaction.setStatus("DELETED");
            clearPin(interaction);
            if (interactionMapper.updateById(interaction) != 1) {
                throw new IllegalStateException("评论删除失败");
            }
        }
        pinsToCompact.forEach(target -> compactPinned(target.type(), target.id()));
        return ordered.size();
    }

    private List<Long> normalizeBatchIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100) {
            throw new IllegalArgumentException("单次可删除 1 到 100 条互动");
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("评论 ID 无效");
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private void requireDeletable(Interaction interaction) {
        if (interaction == null || !Set.of("PENDING", "PUBLISHED", "HIDDEN", "SPAM")
                .contains(interaction.getStatus())) {
            throw new IllegalArgumentException("当前评论状态不可删除");
        }
    }

    public Interaction requireInteraction(Long id) {
        Interaction interaction = interactionMapper.selectById(id);
        if (interaction == null) throw new IllegalArgumentException("评论不存在");
        return interaction;
    }

    static long resolveRootId(Interaction parent) {
        return parent.getRootId() == null ? parent.getId() : parent.getRootId();
    }

    static void requireAuthenticatedReply(User actor, Long replyToId) {
        if (replyToId != null && actor == null) throw new IllegalArgumentException("回复需要登录");
    }

    static void requireSameTarget(Interaction parent, String targetType, Long targetId) {
        if (!Objects.equals(parent.getTargetType(), targetType)
                || !Objects.equals(parent.getTargetId(), targetId)) {
            throw new IllegalArgumentException("不能跨内容回复");
        }
    }

    private void requirePublishedReplyParent(
            Interaction parent,
            String targetType,
            Long targetId) {
        if (!"PUBLISHED".equals(parent.getStatus())) {
            throw new IllegalArgumentException("只能回复已发布评论");
        }
        if (parent.getRootId() == null) return;
        Interaction root = requireInteraction(parent.getRootId());
        requireSameTarget(root, targetType, targetId);
        if (root.getRootId() != null || !"PUBLISHED".equals(root.getStatus())) {
            throw new IllegalArgumentException("只能回复已发布评论");
        }
    }

    private void validateTarget(String targetType, Long targetId, User actor) {
        if (targetId == null || targetId <= 0) throw new IllegalArgumentException("目标不存在");
        if ("POST".equals(targetType)) {
            if (actor == null) throw new IllegalArgumentException("文章评论需要登录");
            Post post = postService.getById(targetId);
            if (post == null || !"PUBLISHED".equals(post.getStatus())) {
                throw new IllegalArgumentException("文章不存在或未发布");
            }
        } else if (targetId != 1L) {
            throw new IllegalArgumentException("留言板目标无效");
        }
    }

    private void validateReadableTarget(String targetType, Long targetId) {
        if (targetId == null || targetId <= 0) throw new IllegalArgumentException("目标不存在");
        if ("POST".equals(targetType)) {
            Post post = postService.getById(targetId);
            if (post == null || !"PUBLISHED".equals(post.getStatus())) {
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
        return toResponse(interaction, liked, snapshotProfile(interaction), null);
    }

    private InteractionItemResponse projectedResponse(Interaction interaction, boolean liked) {
        ProfileProjection profile = projectProfiles(List.of(interaction)).get(interaction.getId());
        return toResponse(interaction, liked, profile, null);
    }

    private InteractionItemResponse toResponse(
            Interaction interaction,
            boolean liked,
            ProfileProjection profile,
            String replyToNickname) {
        InteractionItemResponse response = new InteractionItemResponse();
        response.setId(interaction.getId());
        response.setTargetType(interaction.getTargetType());
        response.setTargetId(interaction.getTargetId());
        response.setRootId(interaction.getRootId());
        response.setReplyToId(interaction.getReplyToId());
        response.setReplyToNickname(replyToNickname);
        response.setContent(interaction.getContent());
        response.setUserId(interaction.getUserId());
        ProfileProjection display = profile == null ? snapshotProfile(interaction) : profile;
        response.setNickname(display.nickname());
        response.setAvatarUrl(display.avatarUrl());
        response.setRole(display.role());
        response.setTitleName(display.titleName());
        response.setTitleStyle(display.titleStyle());
        response.setIpRegion(interaction.getIpRegion());
        response.setStatus(interaction.getStatus());
        response.setLikeCount(interaction.getLikeCount() == null ? 0 : interaction.getLikeCount());
        response.setLiked(liked);
        response.setPinned(Integer.valueOf(1).equals(interaction.getPinned()));
        response.setPinOrder(interaction.getPinOrder());
        response.setPinnedAt(interaction.getPinnedAt());
        response.setEditedTime(interaction.getEditedTime());
        response.setCreateTime(interaction.getCreateTime());
        return response;
    }

    private AdminInteractionResponse toAdminResponse(
            Interaction interaction,
            ProfileProjection profile,
            String replyToNickname,
            String sourceTitle,
            int replyCount,
            List<AdminInteractionResponse> replies) {
        ProfileProjection display = profile == null ? snapshotProfile(interaction) : profile;
        return new AdminInteractionResponse(
                interaction.getId(),
                interaction.getTargetType(),
                interaction.getTargetId(),
                sourceTitle,
                interaction.getRootId(),
                interaction.getReplyToId(),
                replyToNickname,
                interaction.getContent(),
                interaction.getUserId(),
                display.nickname(),
                display.avatarUrl(),
                display.role(),
                display.titleName(),
                display.titleStyle(),
                interaction.getStatus(),
                interaction.getIpRegion(),
                interaction.getLikeCount() == null ? 0 : interaction.getLikeCount(),
                Integer.valueOf(1).equals(interaction.getPinned()),
                interaction.getPinOrder(),
                interaction.getPinnedAt(),
                interaction.getCreateTime(),
                replyCount,
                List.copyOf(replies));
    }

    private Map<InteractionTarget, String> projectSourceTitles(Collection<Interaction> interactions) {
        if (interactions == null || interactions.isEmpty()) return Map.of();
        Set<Long> postIds = interactions.stream()
                .filter(interaction -> "POST".equals(interaction.getTargetType()))
                .map(Interaction::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Post> posts = postIds.isEmpty() ? List.of() : postService.listByIds(postIds);
        if (posts == null) posts = List.of();
        Map<InteractionTarget, String> result = new HashMap<>();
        for (Post post : posts) {
            result.put(new InteractionTarget("POST", post.getId()), value(post.getTitle()));
        }
        interactions.stream()
                .filter(interaction -> "GUESTBOOK".equals(interaction.getTargetType()))
                .forEach(interaction -> result.put(
                        new InteractionTarget("GUESTBOOK", interaction.getTargetId()), "留言板"));
        return result;
    }

    private String sourceTitle(
            Interaction interaction,
            Map<InteractionTarget, String> sourceTitles) {
        return sourceTitles.getOrDefault(
                new InteractionTarget(interaction.getTargetType(), interaction.getTargetId()),
                "POST".equals(interaction.getTargetType()) ? "已删除文章" : "留言板");
    }

    private List<Interaction> withReplyTargets(
            Collection<Interaction> primary,
            Collection<Interaction> additional,
            boolean includeModeration) {
        List<Interaction> result = new ArrayList<>();
        if (primary != null) result.addAll(primary);
        if (additional != null) result.addAll(additional);
        Set<Long> present = result.stream()
                .map(Interaction::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> missing = result.stream()
                .map(Interaction::getReplyToId)
                .filter(Objects::nonNull)
                .filter(id -> !present.contains(id))
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            List<Interaction> targets = interactionMapper.selectBatchIds(missing);
            if (targets != null) {
                targets.stream()
                        .filter(target -> includeModeration || "PUBLISHED".equals(target.getStatus()))
                        .forEach(result::add);
            }
        }
        return result;
    }

    private Map<Long, ProfileProjection> projectProfiles(Collection<Interaction> interactions) {
        if (interactions == null || interactions.isEmpty()) return Map.of();
        Set<Long> userIds = interactions.stream()
                .map(Interaction::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<User> users = userIds.isEmpty() ? List.of() : userService.listByIds(userIds);
        if (users == null) users = List.of();
        Map<Long, User> usersById = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user, (first, ignored) -> first));
        Map<Long, ProfileProjection> result = new HashMap<>();
        for (Interaction interaction : interactions) {
            if (interaction.getId() == null) continue;
            User current = interaction.getUserId() == null ? null : usersById.get(interaction.getUserId());
            result.put(interaction.getId(), current == null
                    ? snapshotProfile(interaction)
                    : currentProfile(current));
        }
        return result;
    }

    private ProfileProjection currentProfile(User user) {
        String nickname = user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername()
                : user.getNickname();
        return new ProfileProjection(
                value(nickname),
                value(user.getAvatarUrl()),
                value(user.getRole()),
                value(user.getTitleName()),
                user.getTitleStyle() == null || user.getTitleStyle().isBlank()
                        ? "default"
                        : user.getTitleStyle());
    }

    private ProfileProjection snapshotProfile(Interaction interaction) {
        return new ProfileProjection(
                value(interaction.getNickname()),
                value(interaction.getAvatarUrl()),
                value(interaction.getRole()),
                value(interaction.getTitleName()),
                interaction.getTitleStyle() == null || interaction.getTitleStyle().isBlank()
                        ? "default"
                        : interaction.getTitleStyle());
    }

    private String replyTargetNickname(
            Interaction interaction,
            Map<Long, ProfileProjection> profiles,
            boolean includeModeration) {
        if (interaction.getReplyToId() == null) return null;
        ProfileProjection target = profiles.get(interaction.getReplyToId());
        if (target == null || target.nickname().isBlank()) {
            return includeModeration ? "已删除用户" : "评论不可见";
        }
        return target.nickname();
    }

    private List<Interaction> compactPinned(String targetType, Long targetId) {
        List<Interaction> pinned = interactionMapper.listPinnedRoots(targetType, targetId);
        if (pinned == null) return new ArrayList<>();
        for (int index = 0; index < pinned.size(); index++) {
            Interaction item = pinned.get(index);
            if (!Objects.equals(item.getPinOrder(), index)) {
                item.setPinOrder(index);
                interactionMapper.updateById(item);
            }
        }
        return pinned;
    }

    private void clearPin(Interaction interaction) {
        interaction.setPinned(0);
        interaction.setPinOrder(null);
        interaction.setPinnedAt(null);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private record ProfileProjection(
            String nickname,
            String avatarUrl,
            String role,
            String titleName,
            String titleStyle) {
    }

    private record InteractionTarget(String type, Long id) {
    }
}
