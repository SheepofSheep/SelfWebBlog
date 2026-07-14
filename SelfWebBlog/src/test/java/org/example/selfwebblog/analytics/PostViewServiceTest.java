package org.example.selfwebblog.analytics;

import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.service.PostService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostViewServiceTest {

    @Test
    void sameVisitorOnlyCountsOncePerPostAndDay() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        PostService postService = mock(PostService.class);
        Post post = new Post();
        post.setStatus("PUBLISHED");
        when(postService.getById(7L)).thenReturn(post);
        when(mapper.insertDailyView(7L, "visitor", "2026-07-14")).thenReturn(1, 0);
        PostViewService service = new PostViewService(mapper, postService);

        assertTrue(service.record(7L, "visitor", LocalDate.of(2026, 7, 14)));
        assertFalse(service.record(7L, "visitor", LocalDate.of(2026, 7, 14)));
        verify(mapper).incrementViewCount(7L);
    }

    @Test
    void draftViewsAreIgnored() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        PostService postService = mock(PostService.class);
        Post post = new Post();
        post.setStatus("DRAFT");
        when(postService.getById(7L)).thenReturn(post);
        PostViewService service = new PostViewService(mapper, postService);

        assertFalse(service.record(7L, "visitor", LocalDate.now()));
        verify(mapper, never()).insertDailyView(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
