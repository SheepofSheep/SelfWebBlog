package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.entity.Comment;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.service.CommentRateLimiter;
import org.example.selfwebblog.service.CommentService;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.service.UserService;
import org.example.selfwebblog.interaction.InteractionService;
import org.example.selfwebblog.security.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Mock
    private InteractionService interactionService;

    @Mock
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentController(commentService, userService, blogInfoService, postService,
                        commentRateLimiter, interactionService, clientIpResolver))
                .setControllerAdvice(new ResultHttpStatusAdvice())
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

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
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
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
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429));

        verify(commentService, never()).save(any());
    }

    @Test
    void archivedPostCommentsReturnNotFoundWithoutReadingComments() throws Exception {
        Post archived = new Post();
        archived.setId(99L);
        archived.setStatus("ARCHIVED");
        when(postService.getById(99L)).thenReturn(archived);

        mockMvc.perform(get("/api/comments/post/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        verify(commentService, never()).getCommentsByPostId(any(), anyInt(), anyInt());
    }

    @Test
    void legacyReadProjectsCurrentAccountProfileForEveryExistingUserAndKeepsDeletedSnapshots() throws Exception {
        Post post = new Post();
        post.setId(99L);
        post.setStatus("PUBLISHED");
        when(postService.getById(99L)).thenReturn(post);

        Comment adminComment = comment(1L, 10L, "ADMIN", "旧管理员", "/old-admin.png",
                "旧称号", "old-admin-style");
        Comment readerComment = comment(2L, 20L, "USER", "旧读者", "/old-reader.png",
                "旧读者称号", "old-reader-style");
        Comment deletedUserComment = comment(3L, 30L, "USER", "已注销用户", "/snapshot.png",
                "留存称号", "snapshot-style");
        Page<Comment> page = new Page<>(1, 10, 3);
        page.setRecords(List.of(adminComment, readerComment, deletedUserComment));
        when(commentService.getCommentsByPostId(99L, 1, 10)).thenReturn(page);

        User admin = user(10L, "admin-account", "当前管理员", "/account-admin.png", "ADMIN",
                "站长", "admin-title");
        User reader = user(20L, "reader-account", "当前读者", "/account-reader.png", "MODERATOR",
                "常客", "reader-title");
        when(userService.listByIds(any())).thenReturn(List.of(admin, reader));
        mockMvc.perform(get("/api/comments/post/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].nickname").value("当前管理员"))
                .andExpect(jsonPath("$.data.records[0].avatarUrl").value("/account-admin.png"))
                .andExpect(jsonPath("$.data.records[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.data.records[0].titleName").value("站长"))
                .andExpect(jsonPath("$.data.records[0].titleStyle").value("admin-title"))
                .andExpect(jsonPath("$.data.records[1].nickname").value("当前读者"))
                .andExpect(jsonPath("$.data.records[1].avatarUrl").value("/account-reader.png"))
                .andExpect(jsonPath("$.data.records[1].role").value("MODERATOR"))
                .andExpect(jsonPath("$.data.records[1].titleName").value("常客"))
                .andExpect(jsonPath("$.data.records[1].titleStyle").value("reader-title"))
                .andExpect(jsonPath("$.data.records[2].nickname").value("已注销用户"))
                .andExpect(jsonPath("$.data.records[2].avatarUrl").value("/snapshot.png"))
                .andExpect(jsonPath("$.data.records[2].role").value("USER"))
                .andExpect(jsonPath("$.data.records[2].titleName").value("留存称号"))
                .andExpect(jsonPath("$.data.records[2].titleStyle").value("snapshot-style"));

        verify(blogInfoService, never()).getById(any());
    }

    @Test
    void rejectsCommentForArchivedPost() throws Exception {
        User user = new User();
        user.setId(10L);
        user.setUsername("reader");
        when(userService.getById(10L)).thenReturn(user);
        Post archived = new Post();
        archived.setId(99L);
        archived.setStatus("ARCHIVED");
        when(postService.getById(99L)).thenReturn(archived);

        mockMvc.perform(post("/api/comments")
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":99,\"content\":\"hello\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        verify(interactionService, never()).create(any(), any(), any(), eq(false));
        verify(commentService, never()).save(any());
    }

    @Test
    void legacyWriteDelegatesToTheInteractionCore() throws Exception {
        User user = new User();
        user.setId(10L);
        user.setUsername("reader");
        user.setRole("USER");
        when(userService.getById(10L)).thenReturn(user);
        Post post = new Post();
        post.setId(99L);
        post.setStatus("PUBLISHED");
        when(postService.getById(99L)).thenReturn(post);
        when(clientIpResolver.resolve(any())).thenReturn("198.51.100.8");
        when(commentRateLimiter.tryAcquire(10L, "198.51.100.8")).thenReturn(true);

        mockMvc.perform(post("/api/comments")
                        .requestAttr("userId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"postId":99,"content":"hello"}
                                """))
                .andExpect(status().isOk());

        verify(interactionService).create(any(), eq(user), eq("198.51.100.8"), eq(false));
        verify(commentService, never()).save(any());
    }

    private Comment comment(Long id, Long userId, String role, String nickname, String avatarUrl,
                            String titleName, String titleStyle) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setPostId(99L);
        comment.setUserId(userId);
        comment.setContent("comment-" + id);
        comment.setRole(role);
        comment.setNickname(nickname);
        comment.setAvatarUrl(avatarUrl);
        comment.setTitleName(titleName);
        comment.setTitleStyle(titleStyle);
        return comment;
    }

    private User user(Long id, String username, String nickname, String avatarUrl, String role,
                      String titleName, String titleStyle) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setAvatarUrl(avatarUrl);
        user.setRole(role);
        user.setTitleName(titleName);
        user.setTitleStyle(titleStyle);
        return user;
    }
}
