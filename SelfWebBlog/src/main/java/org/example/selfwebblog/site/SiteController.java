package org.example.selfwebblog.site;

import org.example.selfwebblog.entity.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site")
public class SiteController {
    private final SiteSummaryService summaryService;
    private final SiteProfileService profileService;

    public SiteController(SiteSummaryService summaryService, SiteProfileService profileService) {
        this.summaryService = summaryService;
        this.profileService = profileService;
    }

    @GetMapping("/summary")
    public Result<SiteSummaryService.SiteSummary> summary() {
        return Result.success(summaryService.summary());
    }

    @GetMapping("/about")
    public Result<SiteProfileService.SiteProfile> about() {
        return Result.success(profileService.get());
    }
}
