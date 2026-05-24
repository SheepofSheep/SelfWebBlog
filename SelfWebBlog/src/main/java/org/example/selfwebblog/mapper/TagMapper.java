package org.example.selfwebblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.selfwebblog.entity.Tag;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
