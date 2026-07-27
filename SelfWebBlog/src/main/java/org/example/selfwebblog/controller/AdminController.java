package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.selfwebblog.admin.security.AdminOnly;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.UserService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/admin", "/api/admin"})
@AdminOnly
public class AdminController {
    private static final int MAX_TITLE_LENGTH = 20;
    private static final Set<String> TITLE_STYLES = Set.of("default", "blue", "green", "gold", "rose");

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // ==================== 用户列表（含 IP 重复检测） ====================

    @GetMapping("/users")
    public Result<?> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String title,
            HttpServletRequest request) {

        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(page, size);
        Page<User> userPage = userService.listForAdmin(
                keyword, role, title, pagination.page(), pagination.size());
        List<User> users = userService.list();

        // 按 IP 分组，找出同一 IP 注册多账号的情况
        Map<String, List<User>> ipGroup = users.stream()
                .filter(u -> u.getRegistrationIpHash() != null && !u.getRegistrationIpHash().isBlank())
                .collect(Collectors.groupingBy(User::getRegistrationIpHash));

        Set<String> duplicateIps = ipGroup.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = userPage.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", String.valueOf(u.getId()));
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("email", u.getEmail());
            m.put("avatarUrl", u.getAvatarUrl());
            m.put("role", u.getRole());
            m.put("githubId", u.getGithubId());
            m.put("titleName", u.getTitleName());
            m.put("titleStyle", u.getTitleStyle());
            m.put("createTime", u.getCreateTime());
            // 标记同 IP 多账号
            boolean dup = u.getRegistrationIpHash() != null
                    && duplicateIps.contains(u.getRegistrationIpHash());
            m.put("duplicateIp", dup);
            if (dup) {
                m.put("sameIpUsers", ipGroup.get(u.getRegistrationIpHash()).stream()
                        .map(User::getUsername)
                        .filter(name -> !name.equals(u.getUsername()))
                        .collect(Collectors.toList()));
            }
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", result);
        data.put("records", result);
        data.put("total", userPage.getTotal());
        data.put("current", userPage.getCurrent());
        data.put("size", userPage.getSize());
        data.put("pages", userPage.getPages());
        data.put("duplicateIpCount", duplicateIps.size());
        return Result.success(data);
    }

    // ==================== 授予/修改称号 ====================

    @PutMapping("/users/{id}/title")
    @AdminOnly(action = "USER_TITLE_UPDATE")
    public Result<?> grantTitle(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {

        User user = userService.getById(id);
        if (user == null) return Result.notFound("用户不存在");

        String titleName = body.get("titleName") == null ? "" : body.get("titleName").trim();
        if (titleName.length() > MAX_TITLE_LENGTH) {
            return Result.badRequest("用户称号不能超过20个字符");
        }
        String titleStyle = body.get("titleStyle") == null || body.get("titleStyle").isBlank()
                ? "default"
                : body.get("titleStyle").trim().toLowerCase(Locale.ROOT);
        if (!TITLE_STYLES.contains(titleStyle)) {
            return Result.badRequest("用户称号样式不合法");
        }
        if (titleName.isBlank()) titleStyle = "default";

        user.setTitleName(titleName);
        user.setTitleStyle(titleStyle);
        userService.updateById(user);

        return Result.success(Map.of(
                "id", String.valueOf(user.getId()),
                "titleName", user.getTitleName(),
                "titleStyle", user.getTitleStyle()
        ));
    }

    // ==================== 删除用户 ====================

    @DeleteMapping("/users/{id}")
    @AdminOnly(action = "USER_DELETE")
    public Result<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {

        Long adminUserId = AuthHelper.getUserId(request);
        if (adminUserId != null && adminUserId.equals(id)) {
            return Result.badRequest("不能删除自己");
        }

        User user = userService.getById(id);
        if (user == null) return Result.notFound("用户不存在");
        if ("ADMIN".equals(user.getRole())) {
            return Result.badRequest("不能删除管理员账号");
        }

        userService.removeById(id);
        return Result.success("用户已删除");
    }
}
