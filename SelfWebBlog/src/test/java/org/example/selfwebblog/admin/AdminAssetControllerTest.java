package org.example.selfwebblog.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.upload.asset.ImageAssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAssetControllerTest {
    private MockMvc mockMvc;

    @Mock
    private ImageAssetService assetService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminAssetController(assetService))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void delegatesAssetFiltersAndSerializesIdsAsStrings() throws Exception {
        ImageAssetService.AdminAssetResponse asset = new ImageAssetService.AdminAssetResponse(
                9223372036854775000L,
                "/uploads/a.png",
                "/uploads/a-small.webp",
                "/uploads/a-large.webp",
                1200,
                800,
                1000,
                "ARTICLE",
                "ORPHANED",
                21L,
                0,
                LocalDateTime.of(2026, 7, 26, 10, 0));
        Page<ImageAssetService.AdminAssetResponse> page = new Page<>(2, 10, 21);
        page.setRecords(List.of(asset));
        when(assetService.listForAdmin(2, 10, "ORPHANED", "ARTICLE", "cover"))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/assets")
                        .param("page", "2")
                        .param("size", "10")
                        .param("status", "ORPHANED")
                        .param("purpose", "ARTICLE")
                        .param("keyword", "cover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.records[0].id").value("9223372036854775000"))
                .andExpect(jsonPath("$.data.records[0].uploaderId").value("21"))
                .andExpect(jsonPath("$.data.records[0].referenceCount").value(0));

        verify(assetService).listForAdmin(2, 10, "ORPHANED", "ARTICLE", "cover");
    }

    @Test
    void exposesCompleteReferenceDetailsWithStringAssetId() throws Exception {
        when(assetService.referencesForAdmin(7L)).thenReturn(
                new ImageAssetService.AssetReferenceDetails(
                        7L,
                        "ACTIVE",
                        1,
                        List.of(new ImageAssetService.AssetReference(
                                "POST", "9", "文章", "coverUrl"))));

        mockMvc.perform(get("/api/admin/assets/7/references"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value("7"))
                .andExpect(jsonPath("$.data.references[0].field").value("coverUrl"));
    }

    @Test
    void batchDeletesOnlyThroughTheValidatedServiceContract() throws Exception {
        when(assetService.deleteOrphans(List.of(7L, 8L))).thenReturn(2);

        mockMvc.perform(delete("/api/admin/assets/batch")
                        .contentType("application/json")
                        .content("{\"ids\":[\"7\",\"8\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(2));

        verify(assetService).deleteOrphans(List.of(7L, 8L));
    }
}
