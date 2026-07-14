package org.example.selfwebblog.content.tag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.mapper.PostMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TagMigrationRunner implements ApplicationRunner {
    private final PostMapper postMapper;
    private final TagService tagService;

    public TagMigrationRunner(PostMapper postMapper, TagService tagService) {
        this.postMapper = postMapper;
        this.tagService = tagService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Post> legacyPosts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .isNotNull(Post::getTags)
                .ne(Post::getTags, ""));
        for (Post post : legacyPosts) {
            if (tagService.relationCountForPost(post.getId()) == 0) {
                tagService.replacePostTags(post.getId(), post.getTags());
            }
        }
    }
}
