package org.example.selfwebblog.admin;

import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.example.selfwebblog.site.SiteLink;
import org.example.selfwebblog.site.SiteLinkService;
import org.example.selfwebblog.site.SiteProfileService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSiteControllerTest {
    private MockMvc mockMvc;

    @Mock
    private SiteProfileService profileService;

    @Mock
    private SiteLinkService linkService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminSiteController(profileService, linkService))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsAndCreatesArbitrarySiteLinks() throws Exception {
        SiteLink existing = link(9223372036854775000L, "摄影册", "https://example.com/photos", 0);
        when(linkService.listAll()).thenReturn(List.of(existing));
        when(linkService.create(new SiteLinkService.Request(
                "摄影册", "https://example.com/photos", "camera", true)))
                .thenReturn(existing);

        mockMvc.perform(get("/api/admin/site/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("9223372036854775000"))
                .andExpect(jsonPath("$.data[0].label").value("摄影册"));

        mockMvc.perform(post("/api/admin/site/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"摄影册","url":"https://example.com/photos","iconKey":"camera","enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://example.com/photos"));

        verify(linkService).create(new SiteLinkService.Request(
                "摄影册", "https://example.com/photos", "camera", true));
    }

    @Test
    void updatesReordersAndDeletesSiteLinks() throws Exception {
        SiteLink updated = new SiteLink(
                2L, "邮箱", "mailto:me@example.com", "mail", false, 0, null, null);
        when(linkService.update(2L, new SiteLinkService.Request(
                "邮箱", "mailto:me@example.com", "mail", false)))
                .thenReturn(updated);
        when(linkService.reorder(List.of(2L, 1L))).thenReturn(List.of(
                updated,
                link(1L, "主页", "https://example.com", 1)));

        mockMvc.perform(put("/api/admin/site/links/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"邮箱","url":"mailto:me@example.com","iconKey":"mail","enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(put("/api/admin/site/links/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"2\",\"1\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("2"));

        mockMvc.perform(delete("/api/admin/site/links/2"))
                .andExpect(status().isOk());

        verify(linkService).delete(2L);
    }

    private SiteLink link(long id, String label, String url, int order) {
        return new SiteLink(id, label, url, "link", true, order, null, null);
    }
}
