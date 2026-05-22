package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.UserService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // ==================== 用户列表（含 IP 重复检测） ====================

    @GetMapping("/users")
    public Result<?> listUsers(HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限");

        List<User> users = userService.list();

        // 按 IP 分组，找出同一 IP 注册多账号的情况
        Map<String, List<User>> ipGroup = users.stream()
                .filter(u -> u.getIpAddress() != null && !u.getIpAddress().isBlank())
                .collect(Collectors.groupingBy(User::getIpAddress));

        Set<String> duplicateIps = ipGroup.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("avatarUrl", u.getAvatarUrl());
            m.put("role", u.getRole());
            m.put("githubId", u.getGithubId());
            m.put("ipAddress", u.getIpAddress());
            m.put("titleName", u.getTitleName());
            m.put("titleStyle", u.getTitleStyle());
            m.put("createTime", u.getCreateTime());
            // 标记同 IP 多账号
            boolean dup = u.getIpAddress() != null && duplicateIps.contains(u.getIpAddress());
            m.put("duplicateIp", dup);
            if (dup) {
                m.put("sameIpUsers", ipGroup.get(u.getIpAddress()).stream()
                        .map(User::getUsername)
                        .filter(name -> !name.equals(u.getUsername()))
                        .collect(Collectors.toList()));
            }
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", result);
        data.put("total", users.size());
        data.put("duplicateIpCount", duplicateIps.size());
        return Result.success(data);
    }

    // ==================== 授予/修改称号 ====================

    @PutMapping("/users/{id}/title")
    public Result<?> grantTitle(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限");

        User user = userService.getById(id);
        if (user == null) return Result.error("用户不存在");

        String titleName = body.get("titleName");
        String titleStyle = body.get("titleStyle");

        user.setTitleName(titleName != null ? titleName.trim() : "");
        user.setTitleStyle(titleStyle != null && !titleStyle.isBlank() ? titleStyle : "default");
        userService.updateById(user);

        return Result.success(Map.of(
                "id", user.getId(),
                "titleName", user.getTitleName(),
                "titleStyle", user.getTitleStyle()
        ));
    }

    // ==================== 删除用户 ====================

    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.error("无权限");

        Long adminUserId = AuthHelper.getUserId(request);
        if (adminUserId != null && adminUserId.equals(id)) {
            return Result.error("不能删除自己");
        }

        User user = userService.getById(id);
        if (user == null) return Result.error("用户不存在");
        if ("ADMIN".equals(user.getRole())) {
            return Result.error("不能删除管理员账号");
        }

        userService.removeById(id);
        return Result.success("用户已删除");
    }
}
