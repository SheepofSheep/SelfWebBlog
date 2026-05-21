package org.example.selfwebblog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.selfwebblog.entity.Post;

public interface PostService extends IService<Post> {
    Page<Post> listByPage(int page, int size);
}