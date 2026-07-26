package org.example.selfwebblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.selfwebblog.interaction.model.Interaction;

import java.util.List;

@Mapper
public interface InteractionMapper extends BaseMapper<Interaction> {
    @Update("UPDATE blog_info SET id = id WHERE id = 1")
    int acquireWriteLock();

    @Update("UPDATE comment SET pinned = pinned WHERE id = #{id}")
    int acquirePinMutationLock(@Param("id") Long id);

    @Select("""
            SELECT * FROM comment
            WHERE target_type = #{targetType}
              AND target_id = #{targetId}
              AND root_id IS NULL
              AND status = 'PUBLISHED'
              AND pinned = 1
            ORDER BY
              CASE WHEN pin_order IS NULL THEN 1 ELSE 0 END,
              pin_order ASC,
              COALESCE(pinned_at, create_time) ASC,
              id ASC
            """)
    List<Interaction> listPinnedRoots(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId);
}
