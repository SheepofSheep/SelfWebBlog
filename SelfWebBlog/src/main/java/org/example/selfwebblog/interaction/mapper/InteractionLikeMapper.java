package org.example.selfwebblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.selfwebblog.interaction.model.InteractionLike;

@Mapper
public interface InteractionLikeMapper extends BaseMapper<InteractionLike> {
}
