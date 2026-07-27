package org.example.selfwebblog.admin;

import org.example.selfwebblog.admin.audit.SecurityAuditService;
import org.example.selfwebblog.config.ResultHttpStatusAdvice;
import org.example.selfwebblog.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {
    private MockMvc mockMvc;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SecurityAuditService auditService;

    @Mock
    private DashboardTrendService trendService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminDashboardController(jdbcTemplate, auditService, trendService))
                .setControllerAdvice(new ResultHttpStatusAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTheRequestedThirtyDayTrend() throws Exception {
        LocalDate end = LocalDate.of(2026, 7, 26);
        LocalDate start = end.minusDays(29);
        when(trendService.load(30)).thenReturn(new DashboardTrendService.DashboardTrend(
                30,
                start,
                end,
                List.of(new DashboardTrendService.TrendDay(end, 8, 3, 1)),
                new DashboardTrendService.TrendTotals(8, 3, 1)));

        mockMvc.perform(get("/api/admin/dashboard/trend").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.range").value(30))
                .andExpect(jsonPath("$.data.days[0].views").value(8));

        verify(trendService).load(30);
    }
}
