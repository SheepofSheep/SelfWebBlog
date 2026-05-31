package org.example.selfwebblog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.selfwebblog.entity.Post;

import java.util.List;

public interface PostService extends IService<Post> {
    Page<Post> listByPage(int page, int size);

    Page<Post> listDrafts(int page, int size);

    Page<Post> search(String keyword, String category, String tag, String sort, int page, int size);

    List<String> allCategories();
}
