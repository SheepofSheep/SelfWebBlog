package org.example.selfwebblog.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.selfwebblog.entity.BlogInfo;
import org.example.selfwebblog.mapper.BlogInfoMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class BlogInfoService extends ServiceImpl<BlogInfoMapper, BlogInfo> {

    // 获取博客信息（单例，只有一条记录）
    public BlogInfo getBlogInfo() {
        BlogInfo info = getById(1);
        if (info == null) {
            // 初始化默认数据
            info = new BlogInfo();
            info.setId(1); // 显式指定 ID 为 1
            info.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=Felix");
            info.setNickname("博主");
            info.setBio("OvO");
            info.setSiteStartDate(LocalDate.now());
            info.setCurrentStatus("");
            info.setAboutMarkdown("");
            info.setSocialLinks("{}");
            // 使用 saveOrUpdate 而不是 save，避免获取生成的 key
            saveOrUpdate(info);
        }
        return info;
    }

    // 更新头像
    public void updateAvatar(String avatarUrl) {
        BlogInfo info = getBlogInfo();
        info.setAvatarUrl(avatarUrl);
        updateById(info);
    }
    
    // 更新背景图
    public void updateBackground(String bgUrl) {
        BlogInfo info = getBlogInfo();
        info.setBgUrl(bgUrl);
        updateById(info);
    }
}
