package org.example.selfwebblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.mapper.PostMapper;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.content.post.PostStatusPolicy;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Override
    public Page<Post> listByPage(int page, int size) {
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, PostStatusPolicy.PUBLISHED)
                .orderByDesc(Post::getPublishedTime)
                .orderByDesc(Post::getId);
        return baseMapper.selectPage(p, wrapper);
    }

    @Override
    public Page<Post> listDrafts(int page, int size) {
        return listForAdmin(PostStatusPolicy.DRAFT, null, page, size);
    }

    @Override
    public Page<Post> listForAdmin(String status, String keyword, int page, int size) {
        Page<Post> result = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        String normalizedStatus = PostStatusPolicy.normalize(status);
        if (!normalizedStatus.isBlank()) wrapper.eq(Post::getStatus, normalizedStatus);
        if (keyword != null && !keyword.isBlank()) {
            String query = keyword.trim();
            wrapper.and(item -> item.like(Post::getTitle, query)
                    .or().like(Post::getSummary, query)
                    .or().like(Post::getContent, query)
                    .or().like(Post::getCategory, query)
                    .or().like(Post::getTags, query));
        }
        if (PostStatusPolicy.ARCHIVED.equals(normalizedStatus)) {
            wrapper.orderByDesc(Post::getArchivedTime);
        }
        wrapper.orderByDesc(Post::getUpdateTime)
                .orderByDesc(Post::getCreateTime)
                .orderByDesc(Post::getId);
        return baseMapper.selectPage(result, wrapper);
    }

    @Override
    public Page<Post> search(String keyword, String category, String tag, String sort, int page, int size) {
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, PostStatusPolicy.PUBLISHED);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Post::getTitle, keyword)
                    .or().like(Post::getSummary, keyword)
                    .or().like(Post::getContent, keyword)
                    .or().like(Post::getTags, keyword));
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(Post::getCategory, category);
        }
        if (tag != null && !tag.isBlank()) {
            wrapper.like(Post::getTags, tag);
        }

        if ("title".equals(sort)) {
            wrapper.orderByAsc(Post::getTitle).orderByDesc(Post::getId);
        } else {
            wrapper.orderByDesc(Post::getPublishedTime).orderByDesc(Post::getId);
        }

        return baseMapper.selectPage(p, wrapper);
    }

    @Override
    public java.util.List<String> allCategories() {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .select(Post::getCategory)
                .eq(Post::getStatus, PostStatusPolicy.PUBLISHED)
                .isNotNull(Post::getCategory)
                .ne(Post::getCategory, "")
                .groupBy(Post::getCategory);
        return baseMapper.selectList(wrapper).stream()
                .map(Post::getCategory).filter(c -> c != null && !c.isBlank()).distinct().toList();
    }
}
