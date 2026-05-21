package org.example.selfwebblog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.selfwebblog.entity.Poster;
import org.example.selfwebblog.mapper.PosterMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PosterService extends ServiceImpl<PosterMapper, Poster> {

    public List<Poster> listAll() {
        return list(new LambdaQueryWrapper<Poster>().orderByAsc(Poster::getSortOrder));
    }

    public void updatePosterFields(Poster poster) {
        update(new LambdaUpdateWrapper<Poster>()
                .set(Poster::getImageUrl, poster.getImageUrl())
                .set(Poster::getTitle, poster.getTitle())
                .set(Poster::getLinkUrl, poster.getLinkUrl())
                .eq(Poster::getId, poster.getId()));
    }

    public void updateSort(Long id, int newOrder) {
        update(new LambdaUpdateWrapper<Poster>()
                .set(Poster::getSortOrder, newOrder)
                .eq(Poster::getId, id));
    }
}
