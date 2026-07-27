package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(userService))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void titleUpdateRejectsNamesLongerThanTwentyCharacters() throws Exception {
        when(userService.getById(7L)).thenReturn(user());

        mockMvc.perform(put("/api/admin/users/7/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleName\":\"" + "称".repeat(21) + "\",\"titleStyle\":\"blue\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateById(any());
    }

    @Test
    void titleUpdateRejectsStylesOutsideTheWhitelist() throws Exception {
        when(userService.getById(7L)).thenReturn(user());

        mockMvc.perform(put("/api/admin/users/7/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleName\":\"贡献者\",\"titleStyle\":\"rainbow\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateById(any());
    }

    @Test
    void titleUpdateTrimsTheNameAndAcceptsWhitelistedStyles() throws Exception {
        User user = user();
        when(userService.getById(7L)).thenReturn(user);
        when(userService.updateById(user)).thenReturn(true);

        mockMvc.perform(put("/api/admin/users/7/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleName\":\"  贡献者  \",\"titleStyle\":\"gold\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titleName").value("贡献者"))
                .andExpect(jsonPath("$.data.titleStyle").value("gold"));

        verify(userService).updateById(user);
    }

    @Test
    void clearingATitleAlsoResetsItsStyle() throws Exception {
        User user = user();
        when(userService.getById(7L)).thenReturn(user);
        when(userService.updateById(user)).thenReturn(true);

        mockMvc.perform(put("/api/admin/users/7/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleName\":\"   \",\"titleStyle\":\"rose\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titleName").value(""))
                .andExpect(jsonPath("$.data.titleStyle").value("default"));
    }

    @Test
    void userListIncludesThePublicNickname() throws Exception {
        User user = user();
        user.setNickname("公开昵称");
        Page<User> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(user));
        when(userService.listForAdmin(null, null, null, 1, 20)).thenReturn(page);
        when(userService.list()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users[0].username").value("visitor"))
                .andExpect(jsonPath("$.data.users[0].nickname").value("公开昵称"));
    }

    @Test
    void userListDelegatesFiltersAndReturnsAStableCompatiblePage() throws Exception {
        User listed = user();
        listed.setId(9223372036854775000L);
        listed.setRegistrationIpHash("same-source");
        User related = user();
        related.setId(8L);
        related.setUsername("related");
        related.setRegistrationIpHash("same-source");
        Page<User> page = new Page<>(2, 10, 21);
        page.setRecords(List.of(listed));
        when(userService.listForAdmin("访客", "USER", "贡献", 2, 10)).thenReturn(page);
        when(userService.list()).thenReturn(List.of(listed, related));

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "2")
                        .param("size", "10")
                        .param("keyword", "访客")
                        .param("role", "USER")
                        .param("title", "贡献"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.pages").value(3))
                .andExpect(jsonPath("$.data.total").value(21))
                .andExpect(jsonPath("$.data.records[0].id").value("9223372036854775000"))
                .andExpect(jsonPath("$.data.users[0].id").value("9223372036854775000"))
                .andExpect(jsonPath("$.data.users[0].duplicateIp").value(true))
                .andExpect(jsonPath("$.data.users[0].sameIpUsers[0]").value("related"))
                .andExpect(jsonPath("$.data.duplicateIpCount").value(1));

        verify(userService).listForAdmin("访客", "USER", "贡献", 2, 10);
    }

    @Test
    void userListRejectsInvalidPaginationBeforeQuerying() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("page", "0").param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).listForAdmin(any(), any(), any(), anyInt(), anyInt());
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("visitor");
        user.setTitleName("旧称号");
        user.setTitleStyle("default");
        return user;
    }
}
