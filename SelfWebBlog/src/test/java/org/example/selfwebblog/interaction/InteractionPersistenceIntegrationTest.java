package org.example.selfwebblog.interaction;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.interaction.dto.AdminInteractionResponse;
import org.example.selfwebblog.interaction.dto.InteractionCreateRequest;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "blog.data-dir=target/test-data/interaction-persistence",
        "spring.datasource.hikari.maximum-pool-size=4"
})
class InteractionPersistenceIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private InteractionService interactionService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private CommentService legacyCommentService;

    @BeforeEach
    void resetInteractions() {
        jdbc.update("DELETE FROM interaction_like");
        jdbc.update("DELETE FROM comment");
        jdbc.update("DELETE FROM post");
        jdbc.update("DELETE FROM user WHERE username <> 'admin'");
        jdbc.update("INSERT INTO post (id, title, content, status) VALUES (7, '公开文章', '正文', 'PUBLISHED')");
    }

    @Test
    void legacyCommentListHidesRepliesWhoseRootIsNotPublic() {
        insertRoot(400, 0, "PUBLISHED", "公开根");
        insertReply(401, 400, null, "PUBLISHED", "公开回复");
        insertRoot(402, 0, "HIDDEN", "隐藏根");
        insertReply(403, 402, null, "PUBLISHED", "隐藏根的回复");
        insertRoot(404, 0, "DELETED", "删除根");
        insertReply(405, 404, null, "PUBLISHED", "删除根的回复");
        insertReply(406, 999, null, "PUBLISHED", "孤立回复");

        var visible = legacyCommentService.getCommentsByPostId(7L, 1, 20);
        assertEquals(java.util.Set.of(400L, 401L), visible.getRecords().stream()
                .map(org.example.selfwebblog.entity.Comment::getId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(2, visible.getTotal());

        interactionService.softDelete(400L);
        assertEquals(0, legacyCommentService.getCommentsByPostId(7L, 1, 20).getTotal());
    }

    @Test
    void moderationSourcesAggregateAcrossPagesAndKeepArticlesSeparateFromTheGuestbook() {
        jdbc.update("INSERT INTO post (id, title, content, status) VALUES (8, '', '正文', 'ARCHIVED')");
        for (int index = 0; index < 24; index++) insertRoot(500 + index, 0, "PUBLISHED", "文章评论");
        insertReply(550, 500, null, "PENDING", "需要审核的回复");
        insertRoot(551, 0, "HIDDEN", "另一篇文章的隐藏评论");
        jdbc.update("UPDATE comment SET post_id = 8, target_id = 8 WHERE id = 551");
        insertRoot(552, 0, "PENDING", "留言板待处理");
        jdbc.update("UPDATE comment SET post_id = NULL, target_type = 'GUESTBOOK', target_id = 1 WHERE id = 552");
        insertRoot(553, 0, "DELETED", "已删除文章的评论");
        jdbc.update("UPDATE comment SET post_id = NULL, target_id = 999 WHERE id = 553");

        var sources = interactionService.listModerationSources();
        assertEquals(4, sources.size());
        var article = sources.stream().filter(source -> Long.valueOf(7).equals(source.getTargetId())).findFirst().orElseThrow();
        assertEquals("公开文章", article.getSourceTitle());
        assertEquals(24, article.getRootCount());
        assertEquals(1, article.getReplyCount());
        assertEquals(1, article.getPendingCount());
        assertEquals("无题文章", sources.stream().filter(source -> Long.valueOf(8).equals(source.getTargetId())).findFirst().orElseThrow().getSourceTitle());
        assertEquals("已删除文章", sources.stream().filter(source -> Long.valueOf(999).equals(source.getTargetId())).findFirst().orElseThrow().getSourceTitle());
        var guestbook = sources.stream().filter(source -> "GUESTBOOK".equals(source.getTargetType())).findFirst().orElseThrow();
        assertEquals("留言板", guestbook.getSourceTitle());
        assertEquals(1, guestbook.getRootCount());
        assertEquals(1, guestbook.getPendingCount());

        var articlePage = interactionService.listForModeration("", "POST", 7L, null, "", "", 2, 20);
        assertEquals(24, articlePage.getTotal());
        assertEquals(4, articlePage.getRecords().size());
        assertTrue(articlePage.getRecords().stream().allMatch(item -> Long.valueOf(7).equals(item.targetId())));
        var pending = interactionService.listForModeration("PENDING", "POST", 7L, null, "", "", 1, 20);
        assertEquals(1, pending.getTotal());
        assertEquals(500L, pending.getRecords().get(0).id());
        assertEquals(550L, pending.getRecords().get(0).replies().get(0).id());
        var guestbookPage = interactionService.listForModeration("", "GUESTBOOK", 1L, null, "", "", 1, 20);
        assertEquals(1, guestbookPage.getTotal());
        assertEquals(552L, guestbookPage.getRecords().get(0).id());
    }

    @Test
    void sourceIdFilterRequiresAnExplicitValidTargetType() {
        assertThrows(IllegalArgumentException.class, () -> interactionService.listForModeration(
                "", "", 7L, null, "", "", 1, 20));
        assertThrows(IllegalArgumentException.class, () -> interactionService.listForModeration(
                "", "POST", -1L, null, "", "", 1, 20));
        assertThrows(IllegalArgumentException.class, () -> interactionService.listForModeration(
                "", "UNKNOWN", 7L, null, "", "", 1, 20));
    }

    @Test
    void moderationFiltersAReplyAsOneCandidateAndSearchesItsCurrentUserProfile() {
        insertUser(20, "reply-user", "更新后的昵称", "夜游者");
        insertRoot(200, 1, "PUBLISHED", "普通根评论");
        insertReply(201, 200, 20, "PENDING", "[sticker:happy]");

        insertUser(21, "other-user", "其他用户", "其他称号");
        insertRoot(210, 1, "PUBLISHED", "另一个根评论");
        insertReply(211, 210, 21, "PENDING", "[sticker:happy]");

        Page<AdminInteractionResponse> result = interactionService.listForModeration(
                "PENDING", "POST", true, "STICKER", "夜游者", 1, 20);

        assertEquals(1, result.getTotal());
        assertEquals(200L, result.getRecords().get(0).id());
        assertEquals(1, result.getRecords().get(0).replyCount());
        assertEquals("更新后的昵称", result.getRecords().get(0).replies().get(0).nickname());
    }

    @Test
    void moderationDoesNotCombineFiltersMatchedByDifferentReplies() {
        insertUser(20, "status-user", "状态命中", "普通");
        insertUser(21, "keyword-user", "关键词命中", "夜游者");
        insertRoot(200, 1, "PUBLISHED", "普通根评论");
        insertReply(201, 200, 20, "PENDING", "纯文字");
        insertReply(202, 200, 21, "PUBLISHED", "[sticker:happy]");

        Page<AdminInteractionResponse> result = interactionService.listForModeration(
                "PENDING", "POST", true, "STICKER", "夜游者", 1, 20);

        assertEquals(0, result.getTotal());
    }

    @Test
    void moderationKeywordSearchUsesTheCurrentRootProfileToo() {
        insertUser(22, "root-user", "根评论新昵称", "根评论新称号");
        insertRoot(220, 0, "PUBLISHED", "不含关键词的快照");
        jdbc.update("UPDATE comment SET user_id = 22, nickname = '旧昵称' WHERE id = 220");

        Page<AdminInteractionResponse> result = interactionService.listForModeration(
                "", "POST", false, "TEXT", "根评论新称号", 1, 20);

        assertEquals(1, result.getTotal());
        assertEquals("根评论新昵称", result.getRecords().get(0).nickname());
    }

    @Test
    void hiddenDeletedPendingAndOrphanedCommentsCannotExposeOrMutateLikes() {
        insertRoot(300, 0, "HIDDEN", "隐藏评论");
        insertRoot(301, 0, "DELETED", "删除评论");
        insertRoot(302, 0, "PENDING", "待审评论");
        insertRoot(303, 0, "SPAM", "垃圾评论");
        insertRoot(304, 0, "HIDDEN", "隐藏根");
        insertReply(305, 304, null, "PUBLISHED", "根不可见的回复");

        for (long id : List.of(300L, 301L, 302L, 303L, 305L)) {
            assertThrows(IllegalArgumentException.class,
                    () -> likeService.state("COMMENT", id, 1L));
            assertThrows(IllegalArgumentException.class,
                    () -> likeService.setLiked("COMMENT", id, 1L, true));
        }

        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM interaction_like WHERE target_type = 'COMMENT'",
                Integer.class));
    }

    @Test
    void commentLikesRequireAPublicTargetAndAllowThePublishedGuestbook() {
        insertRoot(310, 0, "PUBLISHED", "公开文章评论");
        jdbc.update("UPDATE post SET status = 'ARCHIVED' WHERE id = 7");
        assertThrows(IllegalArgumentException.class,
                () -> likeService.setLiked("COMMENT", 310L, 1L, true));

        jdbc.update("""
                INSERT INTO comment (
                    id, post_id, target_type, target_id, content, nickname, role, pinned, status
                ) VALUES (311, NULL, 'GUESTBOOK', 1, '公开留言', '访客', 'GUEST', 0, 'PUBLISHED')
                """);

        LikeService.LikeState state = likeService.setLiked("COMMENT", 311L, 1L, true);

        assertTrue(state.liked());
        assertEquals(1, state.count());
    }

    @Test
    void likeRelationshipRollsBackWhenTheTargetStopsBeingPublicBeforeCounterUpdate() {
        insertRoot(312, 0, "PUBLISHED", "即将失效的评论");
        jdbc.execute("""
                CREATE TRIGGER hide_comment_during_like
                AFTER INSERT ON interaction_like
                WHEN NEW.target_type = 'COMMENT' AND NEW.target_id = 312
                BEGIN
                    UPDATE comment SET status = 'HIDDEN' WHERE id = 312;
                END
                """);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> likeService.setLiked("COMMENT", 312L, 1L, true));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS hide_comment_during_like");
        }

        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM interaction_like WHERE target_type = 'COMMENT' AND target_id = 312",
                Integer.class));
        assertEquals("PUBLISHED", jdbc.queryForObject(
                "SELECT status FROM comment WHERE id = 312",
                String.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT like_count FROM comment WHERE id = 312",
                Integer.class));
    }

    @Test
    void guessedIdsCannotCreateRepliesOnUnpublishedParents() {
        insertRoot(320, 0, "HIDDEN", "隐藏父评论");
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("admin");
        actor.setNickname("管理员");
        actor.setRole("ADMIN");

        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setTargetType("POST");
        request.setTargetId(7L);
        request.setReplyToId(320L);
        request.setContent("不应创建");

        assertThrows(IllegalArgumentException.class,
                () -> interactionService.create(request, actor, "127.0.0.1", false));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM comment", Integer.class));
    }

    @Test
    void publishedReplyCannotBeUsedAsAParentWhenItsRootIsHidden() {
        insertRoot(321, 0, "HIDDEN", "隐藏根评论");
        insertReply(322, 321, null, "PUBLISHED", "表面已发布的回复");
        User actor = new User();
        actor.setId(1L);
        actor.setUsername("admin");
        actor.setNickname("管理员");
        actor.setRole("ADMIN");

        InteractionCreateRequest request = new InteractionCreateRequest();
        request.setTargetType("POST");
        request.setTargetId(7L);
        request.setReplyToId(322L);
        request.setContent("不应挂到隐藏线程");

        assertThrows(IllegalArgumentException.class,
                () -> interactionService.create(request, actor, "127.0.0.1", false));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM comment", Integer.class));
    }

    @Test
    void concurrentPinsReceiveDistinctStableOrdersOnSQLite() throws Exception {
        insertRoot(330, 0, "PUBLISHED", "并发一");
        insertRoot(331, 0, "PUBLISHED", "并发二");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> futures = List.of(
                    executor.submit(() -> pinAfterBarrier(330L, ready, start, failures)),
                    executor.submit(() -> pinAfterBarrier(331L, ready, start, failures)));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(failures.isEmpty(), () -> "并发置顶异常: " + failures);
        assertEquals(List.of(0, 1), jdbc.queryForList("""
                SELECT pin_order FROM comment
                WHERE target_type = 'POST' AND target_id = 7 AND pinned = 1
                ORDER BY pin_order
                """, Integer.class));
    }

    @Test
    void concurrentLikesAcquireAWriteTransactionBeforeReading() throws Exception {
        insertRoot(335, 0, "PUBLISHED", "点赞一");
        insertRoot(336, 0, "PUBLISHED", "点赞二");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> futures = List.of(
                    executor.submit(() -> likeAfterBarrier(335L, ready, start, failures)),
                    executor.submit(() -> likeAfterBarrier(336L, ready, start, failures)));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(failures.isEmpty(), () -> "并发点赞异常: " + failures);
        assertEquals(List.of(1, 1), jdbc.queryForList("""
                SELECT like_count FROM comment WHERE id IN (335, 336) ORDER BY id
                """, Integer.class));
    }

    @Test
    void pinnedCommentsCanStillMoveUnderTheUniqueOrderInvariant() {
        insertRoot(340, 1, "PUBLISHED", "第一条");
        insertRoot(341, 1, "PUBLISHED", "第二条");
        jdbc.update("UPDATE comment SET pin_order = 0 WHERE id = 340");
        jdbc.update("UPDATE comment SET pin_order = 1 WHERE id = 341");

        interactionService.movePinOrder(341L, "UP");

        assertEquals(List.of(341L, 340L), jdbc.queryForList("""
                SELECT id FROM comment WHERE pinned = 1 ORDER BY pin_order
                """, Long.class));
    }

    @Test
    void unpinningClearsPersistedPinMetadata() {
        insertRoot(350, 1, "PUBLISHED", "取消置顶");
        jdbc.update("""
                UPDATE comment
                SET pin_order = 0, pinned_at = '2026-07-26 12:00:00'
                WHERE id = 350
                """);

        interactionService.setPinned(350L, false);

        assertEquals(0, jdbc.queryForObject(
                "SELECT pinned FROM comment WHERE id = 350", Integer.class));
        assertNull(jdbc.queryForObject(
                "SELECT pin_order FROM comment WHERE id = 350", Integer.class));
        assertNull(jdbc.queryForObject(
                "SELECT pinned_at FROM comment WHERE id = 350", String.class));
    }

    @Test
    void publicThreadOrderingIsStableWhenCommentsShareATimestamp() {
        insertRoot(360, 0, "PUBLISHED", "根一");
        insertRoot(361, 0, "PUBLISHED", "根二");
        insertRoot(362, 0, "PUBLISHED", "根三");
        insertReply(363, 360, null, "PUBLISHED", "回复一");
        insertReply(364, 360, null, "PUBLISHED", "回复二");
        jdbc.update("UPDATE comment SET create_time = '2026-07-26 12:00:00'");

        var firstPage = interactionService.getThread("POST", 7L, 1, 2, null, false);
        var secondPage = interactionService.getThread("POST", 7L, 2, 2, null, false);

        assertEquals(List.of(362L, 361L), firstPage.getItems().stream()
                .map(item -> item.getId()).toList());
        assertEquals(List.of(360L), secondPage.getItems().stream()
                .map(item -> item.getId()).toList());
        assertEquals(List.of(363L, 364L), secondPage.getItems().get(0).getReplies().stream()
                .map(item -> item.getId()).toList());
    }

    private void pinAfterBarrier(
            long id,
            CountDownLatch ready,
            CountDownLatch start,
            List<Throwable> failures) {
        ready.countDown();
        try {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            interactionService.setPinned(id, true);
        } catch (Throwable error) {
            failures.add(error);
        }
    }

    private void likeAfterBarrier(
            long id,
            CountDownLatch ready,
            CountDownLatch start,
            List<Throwable> failures) {
        ready.countDown();
        try {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            likeService.setLiked("COMMENT", id, 1L, true);
        } catch (Throwable error) {
            failures.add(error);
        }
    }

    private void insertUser(long id, String username, String nickname, String title) {
        jdbc.update("""
                INSERT INTO user (id, username, role, nickname, title_name, title_style)
                VALUES (?, ?, 'USER', ?, ?, 'default')
                """, id, username, nickname, title);
    }

    private void insertRoot(long id, int pinned, String status, String content) {
        Integer pinOrder = pinned == 1
                ? jdbc.queryForObject("""
                        SELECT COALESCE(MAX(pin_order), -1) + 1
                        FROM comment
                        WHERE target_type = 'POST' AND target_id = 7 AND pinned = 1
                        """, Integer.class)
                : null;
        jdbc.update("""
                INSERT INTO comment (
                    id, post_id, target_type, target_id, content, nickname, role,
                    pinned, pin_order, status
                ) VALUES (?, 7, 'POST', 7, ?, '根作者', 'USER', ?, ?, ?)
                """, id, content, pinned, pinOrder, status);
    }

    private void insertReply(long id, long rootId, Integer userId, String status, String content) {
        jdbc.update("""
                INSERT INTO comment (
                    id, post_id, target_type, target_id, root_id, reply_to_id, content,
                    user_id, nickname, role, pinned, status
                ) VALUES (?, 7, 'POST', 7, ?, ?, ?, ?, '旧资料', 'USER', 0, ?)
                """, id, rootId, rootId, content, userId, status);
    }
}
