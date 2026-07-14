package org.example.selfwebblog.interaction;

import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.interaction.dto.InteractionThreadResponse;
import org.example.selfwebblog.security.ClientIpResolver;
import org.example.selfwebblog.service.CommentRateLimiter;
import org.example.selfwebblog.service.UserService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InteractionControllerTest {
    private MockMvc mockMvc;

    @Mock
    private InteractionService interactionService;
    @Mock
    private UserService userService;
    @Mock
    private ClientIpResolver clientIpResolver;
    @Mock
    private CommentRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InteractionController(
                        interactionService, userService, clientIpResolver, rateLimiter))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void publicCanReadPublishedThread() throws Exception {
        when(interactionService.getThread(eq("POST"), eq(7L), eq(1), eq(20), any(), eq(false)))
                .thenReturn(new InteractionThreadResponse(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/interactions/POST/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void anonymousArticleCommentRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"POST","targetId":7,"content":"hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
