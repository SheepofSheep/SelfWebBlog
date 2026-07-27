package org.example.selfwebblog.analytics;

import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.service.PostService;
import org.example.selfwebblog.content.post.PostStatusPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PostViewService {
    private final EngagementMapper mapper;
    private final PostService postService;

    public PostViewService(EngagementMapper mapper, PostService postService) {
        this.mapper = mapper;
        this.postService = postService;
    }

    @Transactional
    public boolean record(Long postId, String visitorHash, LocalDate date) {
        Post post = postService.getById(postId);
        if (post == null || !PostStatusPolicy.isPublic(post.getStatus())) return false;
        int inserted = mapper.insertDailyView(postId, visitorHash, date.toString());
        if (inserted == 1) mapper.incrementViewCount(postId);
        return inserted == 1;
    }
}
