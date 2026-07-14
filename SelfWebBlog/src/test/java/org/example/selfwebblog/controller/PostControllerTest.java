package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.analytics.EngagementMapper;
import org.example.selfwebblog.content.tag.TagService;
import org.example.selfwebblog.interaction.mapper.InteractionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @Mock
    private TagService tagService;

    @Mock
    private InteractionMapper interactionMapper;

    @Mock
    private EngagementMapper engagementMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PostController(postService, tagService, interactionMapper, engagementMapper))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void anonymousCannotReadDraftById() throws Exception {
        Post draft = new Post();
        draft.setId(1L);
        draft.setTitle("draft");
        draft.setContent("hidden");
        draft.setStatus("DRAFT");
        when(postService.getById(1L)).thenReturn(draft);

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void adminCanReadDraftById() throws Exception {
        Post draft = new Post();
        draft.setId(1L);
        draft.setTitle("draft");
        draft.setContent("hidden");
        draft.setStatus("DRAFT");
        when(postService.getById(1L)).thenReturn(draft);

        mockMvc.perform(get("/posts/1").requestAttr("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void updateMergesWithExistingPostInsteadOfClearingMissingFields() throws Exception {
        Post existing = new Post();
        existing.setId(7L);
        existing.setTitle("old title");
        existing.setContent("old content");
        existing.setSummary("keep summary");
        existing.setCoverUrl("/uploads/cover.jpg");
        existing.setCategory("随笔");
        existing.setTags("生活,代码");
        existing.setStatus("PUBLISHED");
        when(postService.getById(7L)).thenReturn(existing);
        when(postService.updateById(any(Post.class))).thenReturn(true);

        mockMvc.perform(post("/posts")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 7,
                                  "title": "new title",
                                  "content": "new content",
                                  "status": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postService).updateById(captor.capture());
        Post updated = captor.getValue();
        assertThat(updated.getSummary()).isEqualTo("keep summary");
        assertThat(updated.getCoverUrl()).isEqualTo("/uploads/cover.jpg");
        assertThat(updated.getCategory()).isEqualTo("随笔");
        assertThat(updated.getTags()).isEqualTo("生活,代码");
    }

    @Test
    void rejectsUnknownPostStatus() throws Exception {
        mockMvc.perform(post("/posts")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "title",
                                  "content": "content",
                                  "status": "HIDDEN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(postService, never()).save(any(Post.class));
        verify(postService, never()).updateById(any(Post.class));
    }

    @Test
    void rejectsOversizedPageBeforeQueryingDatabase() throws Exception {
        mockMvc.perform(get("/posts").param("page", "1").param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(postService, never()).listByPage(anyInt(), anyInt());
    }

    @Test
    void deletingPostAlsoRemovesDependentRecords() throws Exception {
        Post post = new Post();
        post.setId(7L);
        when(postService.getById(7L)).thenReturn(post);
        when(postService.removeById(7L)).thenReturn(true);

        mockMvc.perform(delete("/posts/7").requestAttr("role", "ADMIN"))
                .andExpect(status().isOk());

        verify(engagementMapper).deleteCommentLikesForPost(7L);
        verify(engagementMapper).deletePostLikes(7L);
        verify(engagementMapper).deletePostViews(7L);
        verify(interactionMapper).delete(any());
        verify(tagService).replacePostTags(7L, "");
        verify(postService).removeById(7L);
    }
}
