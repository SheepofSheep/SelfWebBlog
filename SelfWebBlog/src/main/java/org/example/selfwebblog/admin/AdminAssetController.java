package org.example.selfwebblog.admin;

import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.upload.asset.ImageAssetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/assets")
@AdminOnly
public class AdminAssetController {
    private final ImageAssetService assetService;

    public AdminAssetController(ImageAssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public Result<List<ImageAssetService.AdminAssetResponse>> list(
            @RequestParam(defaultValue = "200") int limit) {
        return Result.success(assetService.listForAdmin(limit));
    }

    @PostMapping("/scan")
    @AdminOnly(action = "ASSET_REFERENCE_SCAN")
    public Result<ImageAssetService.ReferenceScanResult> scan() {
        return Result.success(assetService.refreshReferenceStatus());
    }

    @DeleteMapping("/{id}")
    @AdminOnly(action = "ASSET_DELETE")
    public Result<String> delete(@PathVariable Long id) throws IOException {
        assetService.deleteOrphan(id);
        return Result.success("图片已删除");
    }
}
