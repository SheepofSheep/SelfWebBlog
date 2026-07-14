package org.example.selfwebblog.interaction;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.controller.AuthHelper;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.example.selfwebblog.interaction.dto.AdminInteractionResponse;
import org.example.selfwebblog.interaction.model.Interaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin/interactions")
@AdminOnly
public class AdminInteractionController {
    private final InteractionService interactionService;

    public AdminInteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping
    public Result<Page<AdminInteractionResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(page, size);
        return Result.success(interactionService.listForModeration(
                status, pagination.page(), pagination.size()));
    }

    @PutMapping("/{id}/status")
    @AdminOnly(action = "INTERACTION_MODERATE")
    public Result<InteractionItemResponse> moderate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        return Result.success(interactionService.moderate(id, body.get("status")));
    }

    @PutMapping("/batch/status")
    @AdminOnly(action = "INTERACTION_BATCH_MODERATE")
    public Result<Integer> moderateBatch(@RequestBody BatchModerationRequest body) {
        if (body == null) throw new IllegalArgumentException("请求不能为空");
        return Result.success(interactionService.moderateBatch(body.ids(), body.status()));
    }

    @PutMapping("/{id}/pinned")
    @AdminOnly(action = "INTERACTION_PIN")
    public Result<InteractionItemResponse> setPinned(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        return Result.success(interactionService.setPinned(id, Boolean.TRUE.equals(body.get("pinned"))));
    }

    @DeleteMapping("/{id}")
    @AdminOnly(action = "INTERACTION_DELETE")
    public Result<String> delete(@PathVariable Long id) {
        interactionService.softDelete(id);
        return Result.success("互动已删除");
    }

    @PostMapping("/{id}/ip")
    @AdminOnly(action = "INTERACTION_IP_REVEAL")
    public Result<Map<String, String>> revealIp(@PathVariable Long id) {
        return Result.success(Map.of("ip", interactionService.revealIp(id)));
    }

    public record BatchModerationRequest(List<Long> ids, String status) {}
}
