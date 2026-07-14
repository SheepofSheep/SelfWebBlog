package org.example.selfwebblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.mapper.UserMapper;
import org.example.selfwebblog.interaction.security.VisitorIdentityService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

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

    public User findOrCreateGithubUser(String githubId, String username, String avatarUrl, String email, String ipAddress) {
        User user = getByGithubId(githubId);
        if (user != null) {
            return user;
        }
        user = new User();
        user.setGithubId(githubId);
        user.setUsername(username);
        user.setAvatarUrl(avatarUrl);
        user.setEmail(email);
        user.setRole("USER");
        user.setTokenVersion(0);
        user.setIpAddress("");
        user.setRegistrationIpHash(hashIp(ipAddress));
        save(user);
        return user;
    }

    private String hashIp(String ipAddress) {
        return ipAddress == null || ipAddress.isBlank() ? "" : visitorIdentityService.hash(ipAddress.trim());
    }
}
