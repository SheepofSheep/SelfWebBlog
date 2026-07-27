package org.example.selfwebblog.controller;

import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.security.ActionRateLimiter;
import org.example.selfwebblog.service.UserService;
import org.example.selfwebblog.service.upload.UploadStorageService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    private MockMvc mockMvc;

    @Mock
    private UserService userService;
    @Mock
    private UploadStorageService uploadStorageService;
    @Mock
    private ActionRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService, uploadStorageService, rateLimiter))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void profileUpdateRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新昵称\"}"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateById(any());
    }

    @Test
    void profileUpdateRejectsNullOrBlankNickname() throws Exception {
        User user = user(7L);
        when(userService.getById(7L)).thenReturn(user);

        mockMvc.perform(put("/api/user/profile")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":null}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/user/profile")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateById(any());
    }

    @Test
    void profileUpdateRejectsOverlongNicknameAndUnsafeAvatarUrl() throws Exception {
        User user = user(7L);
        when(userService.getById(7L)).thenReturn(user);

        mockMvc.perform(put("/api/user/profile")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + "名".repeat(31) + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/user/profile")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateById(any());
    }

    @Test
    void profileUpdateTrimsNicknameAndAcceptsManagedAvatar() throws Exception {
        User user = user(7L);
        when(userService.getById(7L)).thenReturn(user);
        when(userService.updateById(user)).thenReturn(true);

        mockMvc.perform(put("/api/user/profile")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"  新昵称  ","avatarUrl":"/uploads/avatars/new.webp"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.avatarUrl").value("/uploads/avatars/new.webp"));

        verify(userService).updateById(user);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("visitor");
        user.setNickname("旧昵称");
        user.setAvatarUrl("/uploads/avatars/old.webp");
        return user;
    }
}
