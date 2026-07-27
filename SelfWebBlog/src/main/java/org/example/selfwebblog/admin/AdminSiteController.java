package org.example.selfwebblog.admin;

import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.site.SiteLink;
import org.example.selfwebblog.site.SiteLinkService;
import org.example.selfwebblog.site.SiteProfileService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/site")
@AdminOnly
public class AdminSiteController {
    private final SiteProfileService profileService;
    private final SiteLinkService linkService;

    public AdminSiteController(SiteProfileService profileService, SiteLinkService linkService) {
        this.profileService = profileService;
        this.linkService = linkService;
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

    @GetMapping("/links")
    public Result<List<SiteLink>> links() {
        return Result.success(linkService.listAll());
    }

    @PostMapping("/links")
    @AdminOnly(action = "SITE_LINK_CREATE")
    public Result<SiteLink> createLink(@RequestBody SiteLinkService.Request request) {
        return Result.success(linkService.create(request));
    }

    @PutMapping("/links/{id}")
    @AdminOnly(action = "SITE_LINK_UPDATE")
    public Result<SiteLink> updateLink(
            @PathVariable Long id,
            @RequestBody SiteLinkService.Request request) {
        return Result.success(linkService.update(id, request));
    }

    @PutMapping("/links/order")
    @AdminOnly(action = "SITE_LINK_REORDER")
    public Result<List<SiteLink>> reorderLinks(@RequestBody LinkOrderRequest request) {
        return Result.success(linkService.reorder(request == null ? null : request.ids()));
    }

    @DeleteMapping("/links/{id}")
    @AdminOnly(action = "SITE_LINK_DELETE")
    public Result<String> deleteLink(@PathVariable Long id) {
        linkService.delete(id);
        return Result.success("链接已删除");
    }

    public record LinkOrderRequest(List<Long> ids) {
    }
}
