package org.example.selfwebblog.site;

import org.example.selfwebblog.entity.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site")
public class SiteController {
    private final SiteSummaryService summaryService;

    public SiteController(SiteSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public Result<SiteSummaryService.SiteSummary> summary() {
        return Result.success(summaryService.summary());
    }
}
