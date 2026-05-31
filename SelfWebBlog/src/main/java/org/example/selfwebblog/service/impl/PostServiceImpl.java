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
                .ne(Post::getStatus, "DRAFT")
                .orderByDesc(Post::getCreateTime);
        return baseMapper.selectPage(p, wrapper);
    }

    @Override
    public Page<Post> listDrafts(int page, int size) {
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, "DRAFT")
                .orderByDesc(Post::getUpdateTime);
        return baseMapper.selectPage(p, wrapper);
    }

    @Override
    public Page<Post> search(String keyword, String category, String tag, String sort, int page, int size) {
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .ne(Post::getStatus, "DRAFT");

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getSummary, keyword));
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(Post::getCategory, category);
        }
        if (tag != null && !tag.isBlank()) {
            wrapper.like(Post::getTags, tag);
        }

        if ("title".equals(sort)) {
            wrapper.orderByAsc(Post::getTitle);
        } else {
            wrapper.orderByDesc(Post::getCreateTime);
        }

        return baseMapper.selectPage(p, wrapper);
    }

    @Override
    public java.util.List<String> allCategories() {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .select(Post::getCategory)
                .ne(Post::getStatus, "DRAFT")
                .isNotNull(Post::getCategory)
                .ne(Post::getCategory, "")
                .groupBy(Post::getCategory);
        return baseMapper.selectList(wrapper).stream()
                .map(Post::getCategory).filter(c -> c != null && !c.isBlank()).distinct().toList();
    }
}
