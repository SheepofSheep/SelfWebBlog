package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.security.ClientIpResolver;
import org.example.selfwebblog.security.JwtUtil;
import org.example.selfwebblog.security.LoginRateLimiter;
import org.example.selfwebblog.security.OAuthLoginTicketService;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiNamespaceTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private OAuthLoginTicketService loginTicketService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PostController(postService))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void servesPostsFromLegacyAndApiNamespaces() throws Exception {
        Post post = new Post();
        post.setId(42L);
        post.setTitle("same response");
        post.setContent("content");
        post.setStatus("PUBLISHED");

        Page<Post> page = new Page<>(1, 1, 1);
        page.setRecords(List.of(post));
        when(postService.listByPage(1, 1)).thenReturn(page);

        mockMvc.perform(get("/posts").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value("42"));

        mockMvc.perform(get("/api/posts").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value("42"));
    }

    @Test
    void appliesTheSameAuthenticationContractToBothNamespaces() throws Exception {
        MockMvc authMockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(
                        jwtUtil,
                        userService,
                        clientIpResolver,
                        loginRateLimiter,
                        loginTicketService))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();

        authMockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());

        authMockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
