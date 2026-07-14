package org.example.selfwebblog.interaction;

import org.example.selfwebblog.analytics.EngagementMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikeServiceTest {

    @Test
    void repeatedLikeOnlyChangesCounterOnce() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        LikeService service = new LikeService(mapper);
        when(mapper.insertLike("POST", 7L, 3L)).thenReturn(1, 0);
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
        LikeService service = new LikeService(mapper);
        when(mapper.deleteLike("COMMENT", 9L, 3L)).thenReturn(1, 0);
        when(mapper.readLikeCount("COMMENT", 9L)).thenReturn(0);

        assertFalse(service.setLiked("COMMENT", 9L, 3L, false).liked());
        assertFalse(service.setLiked("COMMENT", 9L, 3L, false).liked());
        verify(mapper, times(1)).decrementLikeCount("COMMENT", 9L);
    }

    @Test
    void missingTargetDoesNotCreateOrphanLike() {
        EngagementMapper mapper = mock(EngagementMapper.class);
        LikeService service = new LikeService(mapper);
        when(mapper.readLikeCount("POST", 404L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.setLiked("POST", 404L, 3L, true));

        verify(mapper, never()).insertLike("POST", 404L, 3L);
    }
}
