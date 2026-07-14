package org.example.selfwebblog.content.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentCalendarServiceTest {

    @Test
    void validatesCalendarRange() {
        assertEquals(YearMonth.of(2026, 7), ContentCalendarService.requireMonth(2026, 7));
        assertThrows(IllegalArgumentException.class,
                () -> ContentCalendarService.requireMonth(2026, 13));
        assertThrows(IllegalArgumentException.class,
                () -> ContentCalendarService.requireMonth(LocalDate.now().getYear() + 3, 1));
    }
}
