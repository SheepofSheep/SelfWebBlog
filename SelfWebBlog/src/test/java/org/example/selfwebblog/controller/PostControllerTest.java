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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void anonymousCannotReadArchivedPostById() throws Exception {
        Post archived = new Post();
        archived.setId(2L);
        archived.setTitle("archived");
        archived.setContent("hidden");
        archived.setStatus("ARCHIVED");
        when(postService.getById(2L)).thenReturn(archived);

        mockMvc.perform(get("/api/posts/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void anonymousCannotReadMalformedPublishedStatusById() throws Exception {
        Post malformed = new Post();
        malformed.setId(2L);
        malformed.setStatus("published");
        when(postService.getById(2L)).thenReturn(malformed);

        mockMvc.perform(get("/api/posts/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void adminCanReadArchivedPostById() throws Exception {
        Post archived = new Post();
        archived.setId(2L);
        archived.setStatus("ARCHIVED");
        when(postService.getById(2L)).thenReturn(archived);

        mockMvc.perform(get("/api/posts/2").requestAttr("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    @Test
    void archivesPublishedPostThroughDedicatedStatusEndpoint() throws Exception {
        Post published = new Post();
        published.setId(3L);
        published.setStatus("PUBLISHED");
        published.setContent("正文");
        when(postService.getById(3L)).thenReturn(published);
        when(postService.updateById(any(Post.class))).thenReturn(true);

        mockMvc.perform(put("/api/posts/3/status")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ARCHIVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.archivedTime").isNotEmpty());

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postService).updateById(captor.capture());
        assertThat(captor.getValue().getArchivedTime()).isNotNull();
    }

    @Test
    void restoresArchivedPostWithoutChangingItsCreationTime() throws Exception {
        LocalDateTime created = LocalDateTime.of(2025, 1, 2, 3, 4);
        LocalDateTime published = LocalDateTime.of(2025, 2, 3, 4, 5);
        Post archived = new Post();
        archived.setId(3L);
        archived.setStatus("ARCHIVED");
        archived.setContent("正文");
        archived.setCreateTime(created);
        archived.setPublishedTime(published);
        archived.setArchivedTime(LocalDateTime.of(2026, 1, 2, 3, 4));
        when(postService.getById(3L)).thenReturn(archived);
        when(postService.updateById(any(Post.class))).thenReturn(true);

        mockMvc.perform(put("/api/posts/3/status")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.archivedTime").doesNotExist());

        assertThat(archived.getCreateTime()).isEqualTo(created);
        assertThat(archived.getPublishedTime()).isEqualTo(published);
        assertThat(archived.getArchivedTime()).isNull();
    }

    @Test
    void publishingADraftRecordsTheFirstPublicationInsteadOfItsDraftCreationTime() throws Exception {
        LocalDateTime draftCreated = LocalDateTime.of(2025, 1, 2, 3, 4);
        Post draft = createPost(8L, "DRAFT", "正文");
        draft.setCreateTime(draftCreated);
        when(postService.getById(8L)).thenReturn(draft);
        when(postService.updateById(any(Post.class))).thenReturn(true);

        LocalDateTime before = LocalDateTime.now();
        mockMvc.perform(put("/api/posts/8/status")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publishedTime").isNotEmpty());

        assertThat(draft.getPublishedTime()).isAfterOrEqualTo(before);
        assertThat(draft.getPublishedTime()).isNotEqualTo(draftCreated);
    }

    @Test
    void reapplyingArchiveKeepsTheOriginalArchivedTime() throws Exception {
        LocalDateTime firstArchived = LocalDateTime.of(2026, 1, 2, 3, 4);
        Post archived = new Post();
        archived.setId(3L);
        archived.setStatus("ARCHIVED");
        archived.setContent("正文");
        archived.setArchivedTime(firstArchived);
        when(postService.getById(3L)).thenReturn(archived);
        when(postService.updateById(any(Post.class))).thenReturn(true);

        mockMvc.perform(put("/api/posts/3/status")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk());

        assertThat(archived.getArchivedTime()).isEqualTo(firstArchived);
    }

    @Test
    void editsArchivedContentWithoutRestoringOrResettingArchiveTime() throws Exception {
        LocalDateTime firstArchived = LocalDateTime.of(2026, 1, 2, 3, 4);
        Post archived = createPost(3L, "ARCHIVED", "旧正文");
        archived.setArchivedTime(firstArchived);
        when(postService.getById(3L)).thenReturn(archived);
        when(postService.updateById(any(Post.class))).thenReturn(true);

        mockMvc.perform(post("/posts")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 3,
                                  "title": "归档稿",
                                  "content": "更新后的正文",
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(archived.getStatus()).isEqualTo("ARCHIVED");
        assertThat(archived.getContent()).isEqualTo("更新后的正文");
        assertThat(archived.getArchivedTime()).isEqualTo(firstArchived);
        verify(postService).updateById(archived);
    }

    @Test
    void batchArchivesEverySelectedPublishedPost() throws Exception {
        Post first = createPost(11L, "PUBLISHED", "正文一");
        Post second = createPost(12L, "PUBLISHED", "正文二");
        when(postService.getById(11L)).thenReturn(first);
        when(postService.getById(12L)).thenReturn(second);
        when(postService.updateById(any(Post.class))).thenReturn(true);

        mockMvc.perform(put("/api/posts/batch/status")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[11,12],\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        assertThat(List.of(first.getStatus(), second.getStatus()))
                .containsExactly("ARCHIVED", "ARCHIVED");
        assertThat(first.getArchivedTime()).isNotNull();
        assertThat(second.getArchivedTime()).isNotNull();
        verify(postService).updateById(first);
        verify(postService).updateById(second);
    }

    @Test
    void batchStatusPrevalidatesAllPostsBeforeWriting() throws Exception {
        Post published = createPost(11L, "PUBLISHED", "正文");
        Post draft = createPost(12L, "DRAFT", "草稿");
        when(postService.getById(11L)).thenReturn(published);
        when(postService.getById(12L)).thenReturn(draft);

        mockMvc.perform(put("/api/posts/batch/status")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[11,12],\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isBadRequest());

        verify(postService, never()).updateById(any(Post.class));
        assertThat(published.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void adminListDelegatesNormalizedStatusKeywordAndPagination() throws Exception {
        when(postService.listForAdmin("ARCHIVED", "needle", 2, 10))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(2, 10, 0));

        mockMvc.perform(get("/api/posts/admin")
                        .requestAttr("role", "ADMIN")
                        .param("status", "archived")
                        .param("keyword", "needle")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(10));

        verify(postService).listForAdmin("ARCHIVED", "needle", 2, 10);
    }

    @Test
    void adminListRejectsUnsupportedStatusBeforeQuerying() throws Exception {
        mockMvc.perform(get("/api/posts/admin")
                        .requestAttr("role", "ADMIN")
                        .param("status", "DELETED"))
                .andExpect(status().isBadRequest());

        verify(postService, never()).listForAdmin(any(), any(), anyInt(), anyInt());
    }

    @Test
    void publishesImageOnlyPostWithoutMetadata() throws Exception {
        when(postService.save(any(Post.class))).thenReturn(true);

        mockMvc.perform(post("/posts")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": "![](/uploads/image.webp)",
                                  "status": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postService).save(captor.capture());
        assertThat(captor.getValue().getPublishedTime()).isNotNull();
    }

    @Test
    void rejectsCompletelyEmptyPublishedPost() throws Exception {
        mockMvc.perform(post("/posts")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "content": "  # **  ",
                                  "status": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("文章需要包含正文或媒体"));

        verify(postService, never()).save(any(Post.class));
    }

    @Test
    void rejectsCompletelyEmptyArchivedPostUpdate() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 3,
                                  "title": "归档稿",
                                  "content": "  # **  ",
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("文章需要包含正文或媒体"));

        verify(postService, never()).updateById(any(Post.class));
    }

    @Test
    void archivesImageOnlyPostThroughStatusEndpoint() throws Exception {
        Post published = createPost(4L, "PUBLISHED", "![](/uploads/only-image.webp)");
        when(postService.getById(4L)).thenReturn(published);
        when(postService.updateById(any(Post.class))).thenReturn(true);

        mockMvc.perform(put("/api/posts/4/status")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        verify(postService).updateById(published);
    }

    @Test
    void rejectsArchivingEmptyPostThroughStatusEndpoint() throws Exception {
        Post published = createPost(5L, "PUBLISHED", "  # **  ");
        when(postService.getById(5L)).thenReturn(published);

        mockMvc.perform(put("/api/posts/5/status")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("文章需要包含正文或媒体"));

        verify(postService, never()).updateById(any(Post.class));
        assertThat(published.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void rejectsRawCssInArticleBackgroundConfiguration() throws Exception {
        mockMvc.perform(post("/posts")
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "正文",
                                  "status": "PUBLISHED",
                                  "backgroundMode": "GRADIENT",
                                  "backgroundValue": "linear-gradient(red, blue)"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("文章背景配置不合法"));

        verify(postService, never()).save(any(Post.class));
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

    private Post createPost(long id, String status, String content) {
        Post post = new Post();
        post.setId(id);
        post.setStatus(status);
        post.setContent(content);
        return post;
    }
}
