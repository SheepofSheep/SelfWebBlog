package org.example.selfwebblog.controller;

import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.security.ActionRateLimiter;
import org.example.selfwebblog.service.UserService;
import org.example.selfwebblog.service.upload.UploadStorageService;
import org.example.selfwebblog.service.upload.ImageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    void replacingAnAvatarNeverDeletesAnotherUsersFileButCleansUpOwnedUploads(@TempDir Path directory)
            throws Exception {
        Path avatars = Files.createDirectories(directory.resolve("avatars"));
        String foreignName = "22-" + UUID.randomUUID() + ".png";
        Path foreignAvatar = Files.write(avatars.resolve(foreignName), new byte[] {1});
        User user = user(7L);
        user.setAvatarUrl("/uploads/avatars/" + foreignName);
        when(userService.getById(7L)).thenReturn(user);
        when(userService.updateById(user)).thenReturn(true);
        when(rateLimiter.acquire(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new ActionRateLimiter.Decision(true, 0));
        UploadStorageService storage = new UploadStorageService(
                directory.toString(), new ImageValidator(1024 * 1024, 100, 100));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(userService, storage, rateLimiter))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler()).build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);

        mvc.perform(multipart("/api/user/avatar")
                        .file(new MockMultipartFile("file", "avatar.png", "image/png", bytes.toByteArray()))
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk());
        assertTrue(Files.exists(foreignAvatar), "A referenced avatar owned by another user must survive");
        Path firstOwnedAvatar = directory.resolve(user.getAvatarUrl().substring("/uploads/".length()));
        assertTrue(Files.exists(firstOwnedAvatar));

        mvc.perform(multipart("/api/user/avatar")
                        .file(new MockMultipartFile("file", "avatar.png", "image/png", bytes.toByteArray()))
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk());
        assertTrue(Files.exists(foreignAvatar));
        assertFalse(Files.exists(firstOwnedAvatar), "Superseded owned uploads can be cleaned up");
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
