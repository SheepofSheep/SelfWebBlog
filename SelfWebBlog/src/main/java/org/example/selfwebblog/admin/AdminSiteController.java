package org.example.selfwebblog.admin;

import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.site.SiteProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/site")
@AdminOnly
public class AdminSiteController {
    private final SiteProfileService profileService;

    public AdminSiteController(SiteProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public Result<SiteProfileService.SiteProfile> get() {
        return Result.success(profileService.get());
    }

    @PutMapping
    @AdminOnly(action = "SITE_SETTINGS_UPDATE")
    public Result<SiteProfileService.SiteProfile> update(
            @RequestBody SiteProfileService.SiteSettingsRequest request) {
        return Result.success(profileService.update(request));
    }
}
