package org.example.selfwebblog.controller;

import org.example.selfwebblog.entity.Poster;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.service.PosterService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posters")
public class PosterController {

    private final PosterService posterService;

    public PosterController(PosterService posterService) {
        this.posterService = posterService;
    }

    @GetMapping
    public Result<List<Poster>> list() {
        return Result.success(posterService.listAll());
    }

    @PostMapping
    public Result<String> add(@RequestBody Poster poster, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限");
        posterService.save(poster);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody Poster poster, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限");
        poster.setId(id);
        posterService.updatePosterFields(poster);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限");
        posterService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/sort")
    public Result<String> updateSort(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!AuthHelper.isAdmin(request)) return Result.forbidden("无权限");
        Long id = Long.valueOf(body.get("id").toString());
        int sortOrder = Integer.parseInt(body.get("sortOrder").toString());
        posterService.updateSort(id, sortOrder);
        return Result.success("排序更新成功");
    }
}
