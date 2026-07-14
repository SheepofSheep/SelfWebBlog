package org.example.selfwebblog.interaction;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.controller.AuthHelper;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.example.selfwebblog.interaction.model.Interaction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/interactions")
public class AdminInteractionController {
    private final InteractionService interactionService;

    public AdminInteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping
    public Result<Page<Interaction>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限");
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(page, size);
        return Result.success(interactionService.listForModeration(
                status, pagination.page(), pagination.size()));
    }

    @PutMapping("/{id}/status")
    public Result<InteractionItemResponse> moderate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限");
        return Result.success(interactionService.moderate(id, body.get("status")));
    }
}
