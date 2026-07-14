package org.example.selfwebblog.site;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SiteSummaryServiceTest {

    @Test
    void countsRunningDaysInclusively() {
        assertEquals(1, SiteSummaryService.runningDays(
                LocalDate.of(2026, 7, 14), LocalDate.of(2026, 7, 14)));
        assertEquals(4, SiteSummaryService.runningDays(
                LocalDate.of(2026, 7, 11), LocalDate.of(2026, 7, 14)));
        assertEquals(0, SiteSummaryService.runningDays(null, LocalDate.of(2026, 7, 14)));
    }
}
