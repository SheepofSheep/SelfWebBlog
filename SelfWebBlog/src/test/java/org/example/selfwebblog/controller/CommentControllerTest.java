package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.service.CommentRateLimiter;
import org.example.selfwebblog.service.CommentService;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @Mock
    private UserService userService;

    @Mock
    private BlogInfoService blogInfoService;

    @Mock
    private PostService postService;

    @Mock
    private CommentRateLimiter commentRateLimiter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentController(commentService, userService, blogInfoService, postService, commentRateLimiter))
                .build();
    }

    @Test
    void rejectsCommentForMissingPost() throws Exception {
        User user = new User();
        user.setId(10L);
        user.setUsername("reader");
        user.setRole("USER");
        when(userService.getById(10L)).thenReturn(user);
        when(postService.getById(99L)).thenReturn(null);

        mockMvc.perform(post("/comments")
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": 99,
                                  "content": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(commentService, never()).save(any());
    }

    @Test
    void rejectsCommentForDraftPost() throws Exception {
        User user = new User();
        user.setId(10L);
        user.setUsername("reader");
        user.setRole("USER");
        when(userService.getById(10L)).thenReturn(user);

        Post draft = new Post();
        draft.setId(99L);
        draft.setTitle("draft");
        draft.setContent("hidden");
        draft.setStatus("DRAFT");
        when(postService.getById(99L)).thenReturn(draft);

        mockMvc.perform(post("/comments")
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": 99,
                                  "content": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(commentService, never()).save(any());
    }

    @Test
    void rejectsCommentWhenRateLimited() throws Exception {
        User user = new User();
        user.setId(10L);
        user.setUsername("reader");
        user.setRole("USER");
        when(userService.getById(10L)).thenReturn(user);

        Post post = new Post();
        post.setId(99L);
        post.setTitle("published");
        post.setContent("visible");
        post.setStatus("PUBLISHED");
        when(postService.getById(99L)).thenReturn(post);
        when(commentRateLimiter.tryAcquire(10L, "127.0.0.1")).thenReturn(false);

        mockMvc.perform(post("/comments")
                        .requestAttr("userId", 10L)
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": 99,
                                  "content": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(commentService, never()).save(any());
    }
}
