package org.example.selfwebblog.interaction;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.selfwebblog.admin.audit.SecurityAuditService;
import org.example.selfwebblog.admin.security.AdminAuthorizationInterceptor;
import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.interaction.dto.AdminInteractionResponse;
import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminInteractionControllerTest {
    private MockMvc mockMvc;
    private MockMvc securedMockMvc;

    @Mock
    private InteractionService interactionService;

    @BeforeEach
    void setUp() {
        AdminInteractionController controller = new AdminInteractionController(interactionService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
        AdminAuthorizationInterceptor authorization = new AdminAuthorizationInterceptor(
                new ObjectMapper(),
                org.mockito.Mockito.mock(SecurityAuditService.class),
                new ClientIpResolver(),
                org.mockito.Mockito.mock(VisitorIdentityService.class));
        securedMockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .addInterceptors(authorization)
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void nonAdminCannotReachTheModerationControllerThroughTheAuthorizationChain() throws Exception {
        securedMockMvc.perform(get("/api/admin/interactions").requestAttr("role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("当前账号没有管理员权限"));

        verifyNoInteractions(interactionService);
    }

    @Test
    void pinnedInteractionCanMoveUpThroughTheAdminApi() throws Exception {
        InteractionItemResponse response = new InteractionItemResponse();
        response.setId(7L);
        response.setPinOrder(0);
        when(interactionService.movePinOrder(7L, "UP")).thenReturn(response);

        mockMvc.perform(put("/api/admin/interactions/7/pin-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"UP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinOrder").value(0));

        verify(interactionService).movePinOrder(7L, "UP");
    }

    @Test
    void moderationListDelegatesOptionalDatabaseFilters() throws Exception {
        when(interactionService.listForModeration(
                "PENDING", "POST", 9007199254740993L, true, "STICKER", "needle", 2, 10))
                .thenReturn(new Page<AdminInteractionResponse>(2, 10, 0));

        mockMvc.perform(get("/api/admin/interactions")
                        .param("status", "PENDING")
                        .param("targetType", "POST")
                        .param("targetId", "9007199254740993")
                        .param("pinned", "true")
                        .param("contentType", "STICKER")
                        .param("keyword", "needle")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current").value(2));

        verify(interactionService).listForModeration(
                "PENDING", "POST", 9007199254740993L, true, "STICKER", "needle", 2, 10);
    }

    @Test
    void sourcesAreAdminOnlyAndSerializeTargetIdsWithoutLosingPrecision() throws Exception {
        securedMockMvc.perform(get("/api/admin/interactions/sources").requestAttr("role", "USER"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(interactionService);

        var source = new org.example.selfwebblog.interaction.dto.InteractionSourceResponse();
        source.setTargetType("POST");
        source.setTargetId(9007199254740993L);
        source.setSourceTitle("文章标题");
        source.setRootCount(21);
        when(interactionService.listModerationSources()).thenReturn(List.of(source));

        mockMvc.perform(get("/api/admin/interactions/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].targetId").value("9007199254740993"))
                .andExpect(jsonPath("$.data[0].rootCount").value(21));
    }

    @Test
    void selectedInteractionIdsCanBeDeletedAsExactStringIds() throws Exception {
        when(interactionService.softDeleteBatch(List.of(
                9007199254740993L, 9007199254740995L))).thenReturn(2);

        mockMvc.perform(post("/api/admin/interactions/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"9007199254740993\",\"9007199254740995\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        verify(interactionService).softDeleteBatch(List.of(
                9007199254740993L, 9007199254740995L));
    }
}
