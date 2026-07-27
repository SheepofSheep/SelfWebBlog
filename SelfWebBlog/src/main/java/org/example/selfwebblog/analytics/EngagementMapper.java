package org.example.selfwebblog.analytics;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EngagementMapper {
    @Update("UPDATE blog_info SET id = id WHERE id = 1")
    int acquireWriteLock();

    @Insert("""
            <script>
            INSERT OR IGNORE INTO interaction_like (target_type, target_id, user_id)
            SELECT #{targetType}, #{targetId}, #{userId}
            WHERE
            <choose>
              <when test="targetType == 'POST'">
                EXISTS (SELECT 1 FROM post WHERE id = #{targetId} AND status = 'PUBLISHED')
              </when>
              <otherwise>
                EXISTS (
                  SELECT 1 FROM comment AS candidate
                  WHERE candidate.id = #{targetId}
                    AND candidate.status = 'PUBLISHED'
                    AND (
                      candidate.root_id IS NULL
                      OR EXISTS (
                        SELECT 1 FROM comment AS root
                        WHERE root.id = candidate.root_id
                          AND root.root_id IS NULL
                          AND root.status = 'PUBLISHED'
                          AND root.target_type = candidate.target_type
                          AND root.target_id = candidate.target_id
                      )
                    )
                    AND (
                      (candidate.target_type = 'POST' AND EXISTS (
                        SELECT 1 FROM post
                        WHERE id = candidate.target_id AND status = 'PUBLISHED'
                      ))
                      OR (candidate.target_type = 'GUESTBOOK' AND candidate.target_id = 1)
                    )
                )
              </otherwise>
            </choose>
            </script>
            """)
    int insertLike(@Param("targetType") String targetType, @Param("targetId") Long targetId,
                   @Param("userId") Long userId);

    @Delete("""
            <script>
            DELETE FROM interaction_like
            WHERE target_type = #{targetType} AND target_id = #{targetId} AND user_id = #{userId}
              AND
              <choose>
                <when test="targetType == 'POST'">
                  EXISTS (SELECT 1 FROM post WHERE id = #{targetId} AND status = 'PUBLISHED')
                </when>
                <otherwise>
                  EXISTS (
                    SELECT 1 FROM comment AS candidate
                    WHERE candidate.id = #{targetId}
                      AND candidate.status = 'PUBLISHED'
                      AND (
                        candidate.root_id IS NULL
                        OR EXISTS (
                          SELECT 1 FROM comment AS root
                          WHERE root.id = candidate.root_id
                            AND root.root_id IS NULL
                            AND root.status = 'PUBLISHED'
                            AND root.target_type = candidate.target_type
                            AND root.target_id = candidate.target_id
                        )
                      )
                      AND (
                        (candidate.target_type = 'POST' AND EXISTS (
                          SELECT 1 FROM post
                          WHERE id = candidate.target_id AND status = 'PUBLISHED'
                        ))
                        OR (candidate.target_type = 'GUESTBOOK' AND candidate.target_id = 1)
                      )
                  )
                </otherwise>
              </choose>
            </script>
            """)
    int deleteLike(@Param("targetType") String targetType, @Param("targetId") Long targetId,
                   @Param("userId") Long userId);

    @Update("""
            <script>
            <choose>
              <when test="targetType == 'POST'">
                UPDATE post SET like_count = like_count + 1
                WHERE id = #{targetId} AND status = 'PUBLISHED'
              </when>
              <otherwise>
                UPDATE comment SET like_count = like_count + 1
                WHERE id = #{targetId} AND status = 'PUBLISHED'
                  AND (
                    root_id IS NULL
                    OR EXISTS (
                      SELECT 1 FROM comment AS root
                      WHERE root.id = comment.root_id
                        AND root.root_id IS NULL
                        AND root.status = 'PUBLISHED'
                        AND root.target_type = comment.target_type
                        AND root.target_id = comment.target_id
                    )
                  )
                  AND (
                    (target_type = 'POST' AND EXISTS (
                      SELECT 1 FROM post WHERE id = comment.target_id AND status = 'PUBLISHED'
                    ))
                    OR (target_type = 'GUESTBOOK' AND target_id = 1)
                  )
              </otherwise>
            </choose>
            </script>
            """)
    int incrementLikeCount(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Update("""
            <script>
            <choose>
              <when test="targetType == 'POST'">
                UPDATE post SET like_count = MAX(0, like_count - 1)
                WHERE id = #{targetId} AND status = 'PUBLISHED'
              </when>
              <otherwise>
                UPDATE comment SET like_count = MAX(0, like_count - 1)
                WHERE id = #{targetId} AND status = 'PUBLISHED'
                  AND (
                    root_id IS NULL
                    OR EXISTS (
                      SELECT 1 FROM comment AS root
                      WHERE root.id = comment.root_id
                        AND root.root_id IS NULL
                        AND root.status = 'PUBLISHED'
                        AND root.target_type = comment.target_type
                        AND root.target_id = comment.target_id
                    )
                  )
                  AND (
                    (target_type = 'POST' AND EXISTS (
                      SELECT 1 FROM post WHERE id = comment.target_id AND status = 'PUBLISHED'
                    ))
                    OR (target_type = 'GUESTBOOK' AND target_id = 1)
                  )
              </otherwise>
            </choose>
            </script>
            """)
    int decrementLikeCount(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("""
            <script>
            <choose>
              <when test="targetType == 'POST'">
                SELECT COALESCE(like_count, 0) FROM post
                WHERE id = #{targetId} AND status = 'PUBLISHED'
              </when>
              <otherwise>
                SELECT COALESCE(candidate.like_count, 0)
                FROM comment AS candidate
                WHERE candidate.id = #{targetId}
                  AND candidate.status = 'PUBLISHED'
                  AND (
                    candidate.root_id IS NULL
                    OR EXISTS (
                      SELECT 1 FROM comment AS root
                      WHERE root.id = candidate.root_id
                        AND root.root_id IS NULL
                        AND root.status = 'PUBLISHED'
                        AND root.target_type = candidate.target_type
                        AND root.target_id = candidate.target_id
                    )
                  )
                  AND (
                    (candidate.target_type = 'POST' AND EXISTS (
                      SELECT 1 FROM post
                      WHERE id = candidate.target_id AND status = 'PUBLISHED'
                    ))
                    OR (candidate.target_type = 'GUESTBOOK' AND candidate.target_id = 1)
                  )
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
