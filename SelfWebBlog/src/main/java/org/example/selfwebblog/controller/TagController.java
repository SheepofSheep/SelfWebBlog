package org.example.selfwebblog.controller;

import org.example.selfwebblog.admin.security.AdminOnly;
import jakarta.servlet.http.HttpServletRequest;
import org.example.selfwebblog.content.tag.TagService;
import org.example.selfwebblog.content.tag.TagSummary;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/tags", "/api/tags"})
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<TagSummary>> listTags() {
        return Result.success(tagService.listSummaries());
    }

    @GetMapping("/{slug}/posts")
    public Result<List<Post>> posts(@PathVariable String slug) {
        return Result.success(tagService.listPublishedPosts(slug));
    }

    @PostMapping
    @AdminOnly(action = "TAG_CREATE")
    public Result<Tag> create(@RequestBody NameRequest body, HttpServletRequest request) {
        return Result.success(tagService.create(body.name()));
    }

    @PutMapping("/{id}")
    @AdminOnly(action = "TAG_RENAME")
    public Result<Tag> rename(
            @PathVariable Long id,
            @RequestBody NameRequest body,
            HttpServletRequest request) {
        return Result.success(tagService.rename(id, body.name()));
    }

    @PostMapping("/merge")
    @AdminOnly(action = "TAG_MERGE")
    public Result<String> merge(@RequestBody MergeRequest body, HttpServletRequest request) {
        tagService.merge(body.sourceId(), body.targetId());
        return Result.success("合并成功");
    }

    @DeleteMapping("/{id}")
    @AdminOnly(action = "TAG_DELETE")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        tagService.deleteUnused(id);
        return Result.success("删除成功");
    }

    public record NameRequest(String name) {
    }

    public record MergeRequest(Long sourceId, Long targetId) {
    }
}
