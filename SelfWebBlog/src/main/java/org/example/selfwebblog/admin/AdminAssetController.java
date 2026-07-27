package org.example.selfwebblog.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.upload.asset.ImageAssetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public Result<Page<ImageAssetService.AdminAssetResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String keyword) {
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(page, size);
        return Result.success(assetService.listForAdmin(
                pagination.page(), pagination.size(), status, purpose, keyword));
    }

    @PostMapping("/scan")
    @AdminOnly(action = "ASSET_REFERENCE_SCAN")
    public Result<ImageAssetService.ReferenceScanResult> scan() {
        return Result.success(assetService.refreshReferenceStatus());
    }

    @GetMapping("/{id}/references")
    public Result<ImageAssetService.AssetReferenceDetails> references(@PathVariable Long id) {
        return Result.success(assetService.referencesForAdmin(id));
    }

    @DeleteMapping("/{id}")
    @AdminOnly(action = "ASSET_DELETE")
    public Result<String> delete(@PathVariable Long id) throws IOException {
        assetService.deleteOrphan(id);
        return Result.success("图片已删除");
    }

    @DeleteMapping("/batch")
    @AdminOnly(action = "ASSET_DELETE")
    public Result<AssetBatchDeleteResponse> deleteBatch(
            @RequestBody AssetBatchDeleteRequest request) throws IOException {
        return Result.success(new AssetBatchDeleteResponse(
                assetService.deleteOrphans(request == null ? null : request.ids())));
    }

    public record AssetBatchDeleteRequest(List<Long> ids) {
    }

    public record AssetBatchDeleteResponse(int deleted) {
    }
}
