package org.example.selfwebblog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.entity.Tag;
import org.example.selfwebblog.mapper.TagMapper;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping({"/tags", "/api/tags"})
public class TagController {

    private final TagMapper tagMapper;

    public TagController(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @GetMapping
    public Result<List<Tag>> listTags() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName));
        return Result.success(tags);
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限操作");
        if (tag.getName() == null || tag.getName().trim().isEmpty()) return Result.badRequest("标签名不能为空");
        String name = tag.getName().trim();
        Tag existing = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
        if (existing != null) return Result.success(existing);
        tag.setName(name);
        tag.setSlug(name.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-"));
        tagMapper.insert(tag);
        return Result.success(tag);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限操作");
        tagMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
