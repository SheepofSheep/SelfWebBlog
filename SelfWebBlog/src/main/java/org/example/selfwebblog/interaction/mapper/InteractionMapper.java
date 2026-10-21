package org.example.selfwebblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.selfwebblog.interaction.model.Interaction;
import org.example.selfwebblog.interaction.dto.InteractionSourceResponse;

import java.util.List;

@Mapper
public interface InteractionMapper extends BaseMapper<Interaction> {
    @Select("""
            SELECT c.target_type AS target_type, c.target_id AS target_id,
                   CASE WHEN c.target_type = 'GUESTBOOK' THEN '留言板'
                        WHEN p.id IS NULL THEN '已删除文章'
                        WHEN TRIM(COALESCE(p.title, '')) = '' THEN '无题文章'
                        ELSE p.title END AS source_title,
                   p.status AS source_status,
                   SUM(CASE WHEN c.root_id IS NULL THEN 1 ELSE 0 END) AS root_count,
                   SUM(CASE WHEN c.root_id IS NOT NULL THEN 1 ELSE 0 END) AS reply_count,
                   SUM(CASE WHEN c.status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count,
                   MAX(c.create_time) AS latest_activity
            FROM comment c
            LEFT JOIN post p ON c.target_type = 'POST' AND p.id = c.target_id
            WHERE c.target_type IN ('POST', 'GUESTBOOK') AND c.target_id IS NOT NULL
            GROUP BY c.target_type, c.target_id, p.id, p.title, p.status
            ORDER BY pending_count DESC, latest_activity DESC, c.target_type, c.target_id
            """)
    List<InteractionSourceResponse> listModerationSources();

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
