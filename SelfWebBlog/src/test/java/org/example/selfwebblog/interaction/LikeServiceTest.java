package org.example.selfwebblog.interaction;

import org.example.selfwebblog.analytics.EngagementMapper;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.service.PostService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikeServiceTest {

    @Test
    void mutationAcquiresTheSqliteWriteLockBeforeAnyTargetRead() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        PostService postService = mock(PostService.class);
        LikeService service = new LikeService(mapper, postService);
        when(postService.getById(7L)).thenReturn(post(7L, "PUBLISHED"));
        when(mapper.readLikeCount("POST", 7L)).thenReturn(0);
        when(mapper.insertLike("POST", 7L, 3L)).thenReturn(0);

        service.setLiked("POST", 7L, 3L, true);

        var order = inOrder(mapper, postService);
        order.verify(mapper).acquireWriteLock();
        order.verify(postService).getById(7L);
        order.verify(mapper).readLikeCount("POST", 7L);
    }

    @Test
    void repeatedLikeOnlyChangesCounterOnce() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        PostService postService = mock(PostService.class);
        LikeService service = new LikeService(mapper, postService);
        when(postService.getById(7L)).thenReturn(post(7L, "PUBLISHED"));
        when(mapper.insertLike("POST", 7L, 3L)).thenReturn(1, 0);
        when(mapper.incrementLikeCount("POST", 7L)).thenReturn(1);
        when(mapper.readLikeCount("POST", 7L)).thenReturn(1);

        LikeService.LikeState first = service.setLiked("POST", 7L, 3L, true);
        LikeService.LikeState second = service.setLiked("POST", 7L, 3L, true);

        assertTrue(first.liked());
        assertTrue(second.liked());
        assertEquals(1, second.count());
        verify(mapper).incrementLikeCount("POST", 7L);
    }

    @Test
    void repeatedUnlikeCannotProduceNegativeCount() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        LikeService service = new LikeService(mapper, mock(PostService.class));
        when(mapper.deleteLike("COMMENT", 9L, 3L)).thenReturn(1, 0);
        when(mapper.decrementLikeCount("COMMENT", 9L)).thenReturn(1);
        when(mapper.readLikeCount("COMMENT", 9L)).thenReturn(0);

        assertFalse(service.setLiked("COMMENT", 9L, 3L, false).liked());
        assertFalse(service.setLiked("COMMENT", 9L, 3L, false).liked());
        verify(mapper, times(1)).decrementLikeCount("COMMENT", 9L);
    }

    @Test
    void missingTargetDoesNotCreateOrphanLike() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        PostService postService = mock(PostService.class);
        LikeService service = new LikeService(mapper, postService);
        when(postService.getById(404L)).thenReturn(post(404L, "PUBLISHED"));
        when(mapper.readLikeCount("POST", 404L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.setLiked("POST", 404L, 3L, true));

        verify(mapper, never()).insertLike("POST", 404L, 3L);
    }

    @Test
    void archivedPostLikeStateIsNotReadable() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        PostService postService = mock(PostService.class);
        LikeService service = new LikeService(mapper, postService);
        when(postService.getById(7L)).thenReturn(post(7L, "ARCHIVED"));
        when(mapper.readLikeCount("POST", 7L)).thenReturn(12);

        assertThrows(IllegalArgumentException.class,
                () -> service.state("POST", 7L, 3L));

        verify(mapper, never()).readLikeCount("POST", 7L);
        verify(mapper, never()).hasLike("POST", 7L, 3L);
    }

    @Test
    void archivedPostLikeCannotChangeRelationshipsOrCounters() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        PostService postService = mock(PostService.class);
        LikeService service = new LikeService(mapper, postService);
        when(postService.getById(7L)).thenReturn(post(7L, "ARCHIVED"));
        when(mapper.readLikeCount("POST", 7L)).thenReturn(12);

        assertThrows(IllegalArgumentException.class,
                () -> service.setLiked("POST", 7L, 3L, true));

        verify(mapper, never()).readLikeCount("POST", 7L);
        verify(mapper, never()).insertLike("POST", 7L, 3L);
        verify(mapper, never()).incrementLikeCount("POST", 7L);
    }

    @Test
    void commentLikeStateDoesNotRequireAPostLookup() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        PostService postService = mock(PostService.class);
        LikeService service = new LikeService(mapper, postService);
        when(mapper.readLikeCount("COMMENT", 9L)).thenReturn(4);
        when(mapper.hasLike("COMMENT", 9L, 3L)).thenReturn(1);

        LikeService.LikeState state = service.state("COMMENT", 9L, 3L);

        assertTrue(state.liked());
        assertEquals(4, state.count());
        verify(postService, never()).getById(9L);
    }

    private Post post(long id, String status) {
        Post post = new Post();
        post.setId(id);
        post.setStatus(status);
        return post;
    }
}
