package org.example.selfwebblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.selfwebblog.config.PaginationPolicy;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.mapper.UserMapper;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_TITLE_LENGTH = 20;
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "USER");

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final VisitorIdentityService visitorIdentityService;

    public UserService(VisitorIdentityService visitorIdentityService) {
        this.visitorIdentityService = visitorIdentityService;
    }

    public User register(String username, String password, String email, String avatarUrl, String ipAddress) {
        if (getByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setAvatarUrl(avatarUrl != null ? avatarUrl : "");
        user.setRole("USER");
        user.setTokenVersion(0);
        user.setIpAddress("");
        user.setRegistrationIpHash(hashIp(ipAddress));
        save(user);
        return user;
    }

    public User login(String username, String password) {
        User user = getByUsername(username);
        if (user == null || user.getPassword() == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        return user;
    }

    public User getByGithubId(String githubId) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));
    }

    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    public Page<User> listForAdmin(
            String keyword, String role, String title, int page, int size) {
        PaginationPolicy.PageRequest pagination = PaginationPolicy.require(page, size);
        String normalizedKeyword = filter(keyword, "搜索关键词", MAX_KEYWORD_LENGTH);
        String normalizedRole = filter(role, "用户角色", 20).toUpperCase(Locale.ROOT);
        String normalizedTitle = filter(title, "用户称号", MAX_TITLE_LENGTH);
        if (!normalizedRole.isBlank() && !ADMIN_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("用户角色无效");
        }

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (!normalizedKeyword.isBlank()) {
            query.and(item -> item.like(User::getUsername, normalizedKeyword)
                    .or().like(User::getNickname, normalizedKeyword)
                    .or().like(User::getEmail, normalizedKeyword)
                    .or().like(User::getTitleName, normalizedKeyword));
        }
        if (!normalizedRole.isBlank()) query.eq(User::getRole, normalizedRole);
        if (!normalizedTitle.isBlank()) query.like(User::getTitleName, normalizedTitle);
        query.orderByDesc(User::getCreateTime).orderByDesc(User::getId);

        return baseMapper.selectPage(
                new Page<>(pagination.page(), pagination.size()), query);
    }

    public User findOrCreateGithubUser(String githubId, String username, String avatarUrl, String email, String ipAddress) {
        User user = getByGithubId(githubId);
        if (user != null) {
            return user;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = switch (attempt) {
                case 0 -> username;
                case 1 -> "github_" + githubId;
                default -> "github_" + githubId + "_" + UUID.randomUUID().toString().replace("-", "");
            };
            if (getByUsername(candidate) != null) continue;
            user = new User();
            user.setGithubId(githubId);
            user.setUsername(candidate);
            user.setAvatarUrl(avatarUrl);
            user.setEmail(email);
            user.setRole("USER");
            user.setTokenVersion(0);
            user.setIpAddress("");
            user.setRegistrationIpHash(hashIp(ipAddress));
            try {
                if (!save(user)) throw new IllegalStateException("GitHub 用户保存失败");
                return user;
            } catch (DataAccessException collision) {
                // Another callback or registration may have won a unique-key race.
                User existing = getByGithubId(githubId);
                if (existing != null) return existing;
                if (getByUsername(candidate) == null) throw collision;
            }
        }
        throw new IllegalStateException("无法分配 GitHub 用户名，请重试");
    }

    private String hashIp(String ipAddress) {
        return ipAddress == null || ipAddress.isBlank() ? "" : visitorIdentityService.hash(ipAddress.trim());
    }

    private String filter(String value, String label, int maximumLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(label + "内容过长");
        }
        return normalized;
    }
}
