package org.example.selfwebblog.analytics;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EngagementMapper {
    @Insert("""
            INSERT OR IGNORE INTO interaction_like (target_type, target_id, user_id)
            VALUES (#{targetType}, #{targetId}, #{userId})
            """)
    int insertLike(@Param("targetType") String targetType, @Param("targetId") Long targetId,
                   @Param("userId") Long userId);

    @Delete("""
            DELETE FROM interaction_like
            WHERE target_type = #{targetType} AND target_id = #{targetId} AND user_id = #{userId}
            """)
    int deleteLike(@Param("targetType") String targetType, @Param("targetId") Long targetId,
                   @Param("userId") Long userId);

    @Update("""
            <script>
            <choose>
              <when test="targetType == 'POST'">
                UPDATE post SET like_count = like_count + 1 WHERE id = #{targetId}
              </when>
              <otherwise>
                UPDATE comment SET like_count = like_count + 1 WHERE id = #{targetId}
              </otherwise>
            </choose>
            </script>
            """)
    int incrementLikeCount(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Update("""
            <script>
            <choose>
              <when test="targetType == 'POST'">
                UPDATE post SET like_count = MAX(0, like_count - 1) WHERE id = #{targetId}
              </when>
              <otherwise>
                UPDATE comment SET like_count = MAX(0, like_count - 1) WHERE id = #{targetId}
              </otherwise>
            </choose>
            </script>
            """)
    int decrementLikeCount(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("""
            <script>
            <choose>
              <when test="targetType == 'POST'">
                SELECT COALESCE(like_count, 0) FROM post WHERE id = #{targetId}
              </when>
              <otherwise>
                SELECT COALESCE(like_count, 0) FROM comment WHERE id = #{targetId}
              </otherwise>
            </choose>
            </script>
            """)
    Integer readLikeCount(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("""
            SELECT COUNT(*) FROM interaction_like
            WHERE target_type = #{targetType} AND target_id = #{targetId} AND user_id = #{userId}
            """)
    int hasLike(@Param("targetType") String targetType, @Param("targetId") Long targetId,
                @Param("userId") Long userId);

    @Insert("""
            INSERT OR IGNORE INTO post_view_daily (post_id, visitor_hash, view_date)
            VALUES (#{postId}, #{visitorHash}, #{viewDate})
            """)
    int insertDailyView(@Param("postId") Long postId, @Param("visitorHash") String visitorHash,
                        @Param("viewDate") String viewDate);

    @Update("UPDATE post SET view_count = view_count + 1 WHERE id = #{postId}")
    int incrementViewCount(@Param("postId") Long postId);

    @Delete("DELETE FROM interaction_like WHERE target_type = 'COMMENT' AND target_id IN (SELECT id FROM comment WHERE target_type = 'POST' AND target_id = #{postId})")
    int deleteCommentLikesForPost(@Param("postId") Long postId);

    @Delete("DELETE FROM interaction_like WHERE target_type = 'POST' AND target_id = #{postId}")
    int deletePostLikes(@Param("postId") Long postId);

    @Delete("DELETE FROM post_view_daily WHERE post_id = #{postId}")
    int deletePostViews(@Param("postId") Long postId);
}
