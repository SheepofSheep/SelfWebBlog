package org.example.selfwebblog.controller;

import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.service.BlogInfoService;
import org.example.selfwebblog.service.UserService;
import org.example.selfwebblog.service.upload.StoredUpload;
import org.example.selfwebblog.service.upload.UploadStorageService;
import org.example.selfwebblog.service.upload.UploadTarget;
import org.example.selfwebblog.upload.asset.ImageAssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {
    private MockMvc mockMvc;

    @Mock
    private BlogInfoService blogInfoService;
    @Mock
    private UploadStorageService uploadStorageService;
    @Mock
    private ImageAssetService imageAssetService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UploadController(
                        blogInfoService, uploadStorageService, imageAssetService))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void siteAvatarUploadUpdatesOnlyBlogInfoAndLeavesTheConfiguredAdminAccountUntouched()
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "site.png", "image/png", new byte[]{1, 2, 3});
        when(uploadStorageService.storeImage(file, UploadTarget.AVATAR))
                .thenReturn(new StoredUpload(
                        "site.webp", "/uploads/avatars/site.webp", "webp", 3));

        mockMvc.perform(multipart("/api/upload/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("/uploads/avatars/site.webp"));

        verify(blogInfoService).updateAvatar("/uploads/avatars/site.webp");
    }

    @Test
    void siteAvatarControllerHasNoAccountServiceDependency() {
        assertThat(Arrays.stream(UploadController.class.getDeclaredFields())
                .noneMatch(field -> field.getType().equals(UserService.class)))
                .isTrue();
    }
}
