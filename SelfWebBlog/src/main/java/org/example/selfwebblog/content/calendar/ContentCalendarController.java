package org.example.selfwebblog.content.calendar;

import org.example.selfwebblog.entity.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/content/calendar")
public class ContentCalendarController {
    private final ContentCalendarService service;

    public ContentCalendarController(ContentCalendarService service) {
        this.service = service;
    }

    @GetMapping
    public Result<ContentCalendarService.MonthCalendar> month(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate today = LocalDate.now();
        return Result.success(service.month(
                year == null ? today.getYear() : year,
                month == null ? today.getMonthValue() : month));
    }
}
