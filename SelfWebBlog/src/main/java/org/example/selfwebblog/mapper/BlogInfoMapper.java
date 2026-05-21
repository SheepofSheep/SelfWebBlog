package org.example.selfwebblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.selfwebblog.entity.BlogInfo;

@Mapper
public interface BlogInfoMapper extends BaseMapper<BlogInfo> {
}
