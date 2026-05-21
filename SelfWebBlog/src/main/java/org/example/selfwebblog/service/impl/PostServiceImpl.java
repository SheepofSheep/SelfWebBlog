package org.example.selfwebblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.mapper.PostMapper;
import org.example.selfwebblog.service.PostService;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Override
    public Page<Post> listByPage(int page, int size) {
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .orderByDesc(Post::getCreateTime);
        return baseMapper.selectPage(p, wrapper);
    }
}