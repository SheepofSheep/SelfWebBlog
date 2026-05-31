package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.BlogInfoService;
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentController(commentService, userService, blogInfoService, postService))
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
}
