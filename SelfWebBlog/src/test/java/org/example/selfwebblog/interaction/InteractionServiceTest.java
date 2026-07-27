package org.example.selfwebblog.interaction;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.interaction.model.Interaction;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.identity.captcha.CaptchaService;
import org.example.selfwebblog.interaction.dto.InteractionCreateRequest;
import org.example.selfwebblog.interaction.dto.AdminInteractionResponse;
import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.example.selfwebblog.interaction.dto.InteractionThreadResponse;
import org.example.selfwebblog.interaction.mapper.InteractionLikeMapper;
import org.example.selfwebblog.interaction.mapper.InteractionMapper;
import org.example.selfwebblog.interaction.security.IpCipher;
import org.example.selfwebblog.interaction.security.IpRegionResolver;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InteractionServiceTest {

    @Test
    void creationAcquiresTheSqliteWriteLockBeforeReadingItsTarget() {
        Fixture fixture = fixture();
        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setTargetType("POST");
        request.setTargetId(7L);
        request.setContent("正文");

        fixture.service.create(
                request, user(1L, "用户", "", "", "default"), "127.0.0.1", false);

        var order = inOrder(fixture.mapper, fixture.postService);
        order.verify(fixture.mapper).acquireWriteLock();
        order.verify(fixture.postService).getById(7L);
        order.verify(fixture.mapper).insert(any(Interaction.class));
    }

    @Test
    void repliesAlwaysResolveToOneRootLevel() {
        Interaction root = interaction(10L, null, "POST", 7L);
        Interaction reply = interaction(11L, 10L, "POST", 7L);

        assertEquals(10L, InteractionService.resolveRootId(root));
        assertEquals(10L, InteractionService.resolveRootId(reply));
    }

    @Test
    void replyCannotCrossInteractionTargets() {
        Interaction root = interaction(10L, null, "POST", 7L);

        assertThrows(IllegalArgumentException.class,
                () -> InteractionService.requireSameTarget(root, "POST", 8L));
    }

    @Test
    void moderationUsesAnExplicitStateMachine() {
        assertTrue(InteractionPolicy.canTransition("PENDING", "PUBLISHED"));
        assertTrue(InteractionPolicy.canTransition("PUBLISHED", "HIDDEN"));
        assertFalse(InteractionPolicy.canTransition("PUBLISHED", "PENDING"));
        assertFalse(InteractionPolicy.canTransition("SPAM", "PUBLISHED"));
    }

    @Test
    void anonymousVisitorsCannotReply() {
        assertThrows(IllegalArgumentException.class,
                () -> InteractionService.requireAuthenticatedReply(null, 10L));
        InteractionService.requireAuthenticatedReply(new User(), 10L);
    }

    @Test
    void repliesCannotBeCreatedAgainstAnUnpublishedParentEvenWhenTheIdAndTargetAreValid() {
        Fixture fixture = fixture();
        Interaction parent = interaction(10L, null, "POST", 7L);
        parent.setStatus("HIDDEN");
        when(fixture.mapper.selectById(10L)).thenReturn(parent);

        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setTargetType("POST");
        request.setTargetId(7L);
        request.setReplyToId(10L);
        request.setContent("猜中 ID 也不能回复");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.create(
                        request,
                        user(1L, "用户", "", "", "default"),
                        "127.0.0.1",
                        false));

        assertEquals("只能回复已发布评论", error.getMessage());
        verify(fixture.mapper, never()).insert(any(Interaction.class));
    }

    @Test
    void threadProjectsCurrentProfilesAndNamesTheSemanticReplyTarget() {
        Fixture fixture = fixture();
        Interaction root = interaction(10L, null, "POST", 7L);
        root.setUserId(1L);
        root.setNickname("旧昵称");
        root.setAvatarUrl("/old.webp");
        root.setTitleName("旧称号");
        root.setTitleStyle("default");
        root.setStatus("PUBLISHED");

        Interaction firstReply = interaction(11L, 10L, "POST", 7L);
        firstReply.setReplyToId(10L);
        firstReply.setUserId(2L);
        firstReply.setNickname("被回复者旧名");
        firstReply.setStatus("PUBLISHED");

        Interaction nestedReply = interaction(12L, 10L, "POST", 7L);
        nestedReply.setReplyToId(11L);
        nestedReply.setUserId(99L);
        nestedReply.setNickname("已删除用户快照");
        nestedReply.setStatus("PUBLISHED");

        fixture.returnsThread(List.of(root), List.of(firstReply, nestedReply));
        User currentRoot = user(1L, "最新昵称", "/uploads/avatars/latest.webp", "新称号", "gold");
        User currentReply = user(2L, "被回复者", "/reply.webp", "", "default");
        when(fixture.userService.listByIds(any(Collection.class)))
                .thenReturn(List.of(currentRoot, currentReply));

        InteractionThreadResponse thread = fixture.service.getThread("POST", 7L, 1, 20, null, false);

        InteractionItemResponse projectedRoot = thread.getItems().get(0);
        assertEquals("最新昵称", projectedRoot.getNickname());
        assertEquals("/uploads/avatars/latest.webp", projectedRoot.getAvatarUrl());
        assertEquals("新称号", projectedRoot.getTitleName());
        assertEquals("gold", projectedRoot.getTitleStyle());
        assertEquals("最新昵称", projectedRoot.getReplies().get(0).getReplyToNickname());
        assertEquals("被回复者", projectedRoot.getReplies().get(1).getReplyToNickname());
        assertEquals("已删除用户快照", projectedRoot.getReplies().get(1).getNickname());
    }

    @Test
    void publicThreadDoesNotRevealTheCurrentProfileOfAnUnpublishedReplyTarget() {
        Fixture fixture = fixture();
        Interaction root = interaction(10L, null, "POST", 7L);
        root.setStatus("PUBLISHED");

        Interaction reply = interaction(12L, 10L, "POST", 7L);
        reply.setReplyToId(11L);
        reply.setStatus("PUBLISHED");

        Interaction hiddenTarget = interaction(11L, 10L, "POST", 7L);
        hiddenTarget.setUserId(2L);
        hiddenTarget.setNickname("旧快照");
        hiddenTarget.setStatus("HIDDEN");

        fixture.returnsThread(List.of(root), List.of(reply));
        when(fixture.mapper.selectBatchIds(any(Collection.class))).thenReturn(List.of(hiddenTarget));
        when(fixture.userService.listByIds(any(Collection.class)))
                .thenReturn(List.of(user(2L, "不应公开的新昵称", "", "", "default")));

        InteractionThreadResponse thread = fixture.service.getThread("POST", 7L, 1, 20, null, false);

        assertEquals("评论不可见", thread.getItems().get(0).getReplies().get(0).getReplyToNickname());
    }

    @Test
    void archivedAndDraftPostsCannotBeReadOrReceiveInteractions() {
        Fixture fixture = fixture();
        Post archived = new Post();
        archived.setStatus("ARCHIVED");
        when(fixture.postService.getById(7L)).thenReturn(archived);

        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setTargetType("POST");
        request.setTargetId(7L);
        request.setContent("正文");

        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.getThread("POST", 7L, 1, 20, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.create(request, user(1L, "用户", "", "", "default"), "127.0.0.1", false));

        archived.setStatus("DRAFT");
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.getThread("POST", 7L, 1, 20, null, false));

        archived.setStatus("published");
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.getThread("POST", 7L, 1, 20, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.create(request, user(1L, "用户", "", "", "default"), "127.0.0.1", false));
    }

    @Test
    void pinningAppendsToTheStableOrderAndRecordsTheTime() {
        Fixture fixture = fixture();
        Interaction first = pinnedInteraction(1L, 0);
        Interaction second = pinnedInteraction(2L, 1);
        Interaction target = interaction(3L, null, "POST", 7L);
        target.setStatus("PUBLISHED");
        target.setPinned(0);
        when(fixture.mapper.selectById(3L)).thenReturn(target);
        when(fixture.mapper.listPinnedRoots("POST", 7L)).thenReturn(List.of(first, second));
        when(fixture.mapper.updateById(any(Interaction.class))).thenReturn(1);

        InteractionItemResponse response = fixture.service.setPinned(3L, true);

        assertTrue(response.isPinned());
        assertEquals(2, response.getPinOrder());
        assertNotNull(response.getPinnedAt());
        verify(fixture.mapper).updateById(target);
    }

    @Test
    void unpinningCompactsTheRemainingOrder() {
        Fixture fixture = fixture();
        Interaction first = pinnedInteraction(1L, 0);
        Interaction target = pinnedInteraction(2L, 1);
        Interaction third = pinnedInteraction(3L, 2);
        when(fixture.mapper.selectById(2L)).thenReturn(target);
        when(fixture.mapper.listPinnedRoots("POST", 7L)).thenReturn(List.of(first, third));
        when(fixture.mapper.updateById(any(Interaction.class))).thenReturn(1);

        InteractionItemResponse response = fixture.service.setPinned(2L, false);

        assertFalse(response.isPinned());
        assertEquals(null, response.getPinOrder());
        assertEquals(null, response.getPinnedAt());
        assertEquals(1, third.getPinOrder());
    }

    @Test
    void movingAPinSwapsItWithItsNeighbourAndRepliesCannotBePinned() {
        Fixture fixture = fixture();
        Interaction first = pinnedInteraction(1L, 0);
        Interaction second = pinnedInteraction(2L, 1);
        Interaction third = pinnedInteraction(3L, 2);
        when(fixture.mapper.selectById(2L)).thenReturn(second);
        when(fixture.mapper.listPinnedRoots("POST", 7L)).thenReturn(List.of(first, second, third));
        when(fixture.mapper.updateById(any(Interaction.class))).thenReturn(1);

        InteractionItemResponse moved = fixture.service.movePinOrder(2L, "UP");

        assertEquals(0, moved.getPinOrder());
        assertEquals(1, first.getPinOrder());

        Interaction reply = interaction(4L, 1L, "POST", 7L);
        reply.setStatus("PUBLISHED");
        when(fixture.mapper.selectById(4L)).thenReturn(reply);
        assertThrows(IllegalArgumentException.class, () -> fixture.service.setPinned(4L, true));
    }

    @Test
    void hidingAPinnedRootClearsItsPinAndCompactsTheRemainingOrder() {
        Fixture fixture = fixture();
        Interaction target = pinnedInteraction(2L, 0);
        Interaction remaining = pinnedInteraction(3L, 1);
        when(fixture.mapper.selectById(2L)).thenReturn(target);
        when(fixture.mapper.listPinnedRoots("POST", 7L)).thenReturn(List.of(remaining));
        when(fixture.mapper.updateById(any(Interaction.class))).thenReturn(1);

        InteractionItemResponse response = fixture.service.moderate(2L, "HIDDEN");

        assertEquals("HIDDEN", response.getStatus());
        assertFalse(response.isPinned());
        assertEquals(null, response.getPinOrder());
        assertEquals(0, remaining.getPinOrder());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void moderationListFiltersInTheDatabaseAndProjectsCurrentProfiles() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "interaction-test"),
                Interaction.class);
        Fixture fixture = fixture();
        Interaction root = interaction(10L, null, "POST", 7L);
        root.setStatus("PENDING");
        root.setPinned(1);
        root.setPinOrder(0);
        root.setUserId(1L);
        root.setNickname("旧昵称");
        Page<Interaction> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(root));
        ArgumentCaptor<LambdaQueryWrapper<Interaction>> wrapper = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(fixture.mapper.selectPage(any(Page.class), wrapper.capture())).thenReturn(page);
        when(fixture.userService.listByIds(any(Collection.class)))
                .thenReturn(List.of(user(1L, "当前昵称", "/current.webp", "当前称号", "blue")));

        Page<AdminInteractionResponse> result = fixture.service.listForModeration(
                "PENDING", "post", true, "needle", 1, 20);

        String sql = wrapper.getValue().getSqlSegment();
        assertTrue(sql.contains("target_type"));
        assertTrue(sql.contains("pinned"));
        assertTrue(sql.contains("content"));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue("PENDING"));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue("POST"));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue(1));
        assertTrue(wrapper.getValue().getParamNameValuePairs().containsValue("%needle%"));
        assertEquals("当前昵称", result.getRecords().get(0).nickname());
        assertEquals("当前称号", result.getRecords().get(0).titleName());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void moderationPagesRootsAndHydratesTheirCompleteThreadAndSourceTitle() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "moderation-thread-test"),
                Interaction.class);
        Fixture fixture = fixture();
        Interaction root = interaction(20L, null, "POST", 7L);
        root.setNickname("根作者");
        root.setStatus("PUBLISHED");
        root.setPinned(1);
        root.setPinOrder(0);
        Interaction firstReply = interaction(21L, 20L, "POST", 7L);
        firstReply.setReplyToId(20L);
        firstReply.setNickname("回复作者");
        firstReply.setStatus("HIDDEN");
        Interaction nestedReply = interaction(22L, 20L, "POST", 7L);
        nestedReply.setReplyToId(21L);
        nestedReply.setNickname("末位作者");
        nestedReply.setStatus("PENDING");

        Page<Interaction> roots = new Page<>(2, 1, 3);
        roots.setRecords(List.of(root));
        ArgumentCaptor<LambdaQueryWrapper<Interaction>> rootQuery =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(fixture.mapper.selectPage(any(Page.class), rootQuery.capture())).thenReturn(roots);
        when(fixture.mapper.selectList(any(Wrapper.class))).thenReturn(List.of(firstReply, nestedReply));
        Post source = new Post();
        source.setId(7L);
        source.setTitle("分页来源文章");
        when(fixture.postService.listByIds(any(Collection.class))).thenReturn(List.of(source));

        Page<AdminInteractionResponse> result = fixture.service.listForModeration(
                "", "POST", null, "", "", 2, 1);

        assertTrue(rootQuery.getValue().getSqlSegment().contains("root_id IS NULL"));
        assertEquals(3, result.getTotal());
        assertEquals(2, result.getCurrent());
        AdminInteractionResponse projectedRoot = result.getRecords().get(0);
        assertEquals("分页来源文章", projectedRoot.sourceTitle());
        assertEquals(2, projectedRoot.replyCount());
        assertEquals(2, projectedRoot.replies().size());
        assertEquals("根作者", projectedRoot.replies().get(0).replyToNickname());
        assertEquals("回复作者", projectedRoot.replies().get(1).replyToNickname());
        assertEquals("HIDDEN", projectedRoot.replies().get(0).status());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void moderationStickerFilterRecognizesTokensAndLegacyEmojiMarkdown() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "moderation-content-test"),
                Interaction.class);
        Fixture fixture = fixture();
        ArgumentCaptor<LambdaQueryWrapper<Interaction>> query =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(fixture.mapper.selectPage(any(Page.class), query.capture()))
                .thenReturn(new Page<>(1, 20, 0));

        fixture.service.listForModeration("", "", null, "STICKER", "", 1, 20);

        assertTrue(query.getValue().getSqlSegment().contains("content LIKE"));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue("%[sticker:%"));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue("%](/images/emojis/%"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void moderationTextFilterExcludesBothStickerRepresentations() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "moderation-text-test"),
                Interaction.class);
        Fixture fixture = fixture();
        ArgumentCaptor<LambdaQueryWrapper<Interaction>> query =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        when(fixture.mapper.selectPage(any(Page.class), query.capture()))
                .thenReturn(new Page<>(1, 20, 0));

        fixture.service.listForModeration("", "", null, "TEXT", "", 1, 20);

        assertTrue(query.getValue().getSqlSegment().contains("content NOT LIKE"));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue("%[sticker:%"));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue("%](/images/emojis/%"));
    }

    @Test
    void batchDeleteNormalizesIdsAndSoftDeletesTheWholeSelection() {
        Fixture fixture = fixture();
        Interaction pinnedRoot = pinnedInteraction(1L, 0);
        Interaction reply = interaction(2L, 1L, "POST", 7L);
        reply.setStatus("HIDDEN");
        reply.setPinned(0);
        when(fixture.mapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(pinnedRoot, reply));
        when(fixture.mapper.listPinnedRoots("POST", 7L)).thenReturn(List.of());
        when(fixture.mapper.updateById(any(Interaction.class))).thenReturn(1);

        int deleted = fixture.service.softDeleteBatch(List.of(1L, 2L, 1L));

        assertEquals(2, deleted);
        assertEquals("DELETED", pinnedRoot.getStatus());
        assertEquals("DELETED", reply.getStatus());
        assertFalse(Integer.valueOf(1).equals(pinnedRoot.getPinned()));
        assertEquals(null, pinnedRoot.getPinOrder());
    }

    @Test
    void batchDeleteValidatesEveryStateBeforeWritingAnything() {
        Fixture fixture = fixture();
        Interaction published = interaction(1L, null, "POST", 7L);
        published.setStatus("PUBLISHED");
        Interaction alreadyDeleted = interaction(2L, null, "POST", 7L);
        alreadyDeleted.setStatus("DELETED");
        when(fixture.mapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(published, alreadyDeleted));

        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.softDeleteBatch(List.of(1L, 2L)));

        verify(fixture.mapper, never()).updateById(any(Interaction.class));
        assertEquals("PUBLISHED", published.getStatus());
    }

    private Interaction interaction(Long id, Long rootId, String targetType, Long targetId) {
        Interaction interaction = new Interaction();
        interaction.setId(id);
        interaction.setRootId(rootId);
        interaction.setTargetType(targetType);
        interaction.setTargetId(targetId);
        return interaction;
    }

    private Interaction pinnedInteraction(Long id, int order) {
        Interaction interaction = interaction(id, null, "POST", 7L);
        interaction.setStatus("PUBLISHED");
        interaction.setPinned(1);
        interaction.setPinOrder(order);
        return interaction;
    }

    private User user(Long id, String nickname, String avatarUrl, String titleName, String titleStyle) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        user.setTitleName(titleName);
        user.setTitleStyle(titleStyle);
        return user;
    }

    private Fixture fixture() {
        InteractionMapper mapper = mock(InteractionMapper.class);
        InteractionLikeMapper likeMapper = mock(InteractionLikeMapper.class);
        PostService postService = mock(PostService.class);
        UserService userService = mock(UserService.class);
        Post published = new Post();
        published.setStatus("PUBLISHED");
        when(postService.getById(anyLong())).thenReturn(published);
        when(likeMapper.selectList(any())).thenReturn(List.of());
        when(userService.listByIds(any(Collection.class))).thenReturn(List.of());
        InteractionService service = new InteractionService(
                mapper,
                likeMapper,
                postService,
                mock(CaptchaService.class),
                mock(GuestTrustService.class),
                mock(IpCipher.class),
                mock(VisitorIdentityService.class),
                mock(IpRegionResolver.class),
                userService);
        return new Fixture(service, mapper, postService, userService);
    }

    private record Fixture(
            InteractionService service,
            InteractionMapper mapper,
            PostService postService,
            UserService userService) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        void returnsThread(List<Interaction> roots, List<Interaction> replies) {
            Page<Interaction> page = new Page<>(1, 20, roots.size());
            page.setRecords(roots);
            when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
            when(mapper.selectList(any(Wrapper.class))).thenReturn(replies);
        }
    }
}
