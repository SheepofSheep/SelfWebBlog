package org.example.selfwebblog.content.tag;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.selfwebblog.entity.Post;
import org.example.selfwebblog.entity.Tag;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface PostTagMapper {
    @Select("SELECT * FROM tag WHERE lower(name) = lower(#{name}) LIMIT 1")
    Tag findByName(@Param("name") String name);

    @Select("SELECT * FROM tag WHERE lower(slug) = lower(#{slug}) LIMIT 1")
    Tag findBySlug(@Param("slug") String slug);

    @Insert("INSERT INTO tag(name, slug, create_time) VALUES (#{name}, #{slug}, #{createTime})")
    int insertTag(@Param("name") String name, @Param("slug") String slug,
                  @Param("createTime") LocalDateTime createTime);

    @Insert("INSERT INTO post_tag(post_id, tag_id) VALUES (#{postId}, #{tagId})")
    int insertRelation(@Param("postId") Long postId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM post_tag WHERE post_id = #{postId}")
    int deleteRelationsForPost(@Param("postId") Long postId);

    @Select("SELECT COUNT(*) FROM post_tag WHERE post_id = #{postId}")
    int relationCountForPost(@Param("postId") Long postId);

    @Select("""
            SELECT t.id, t.name, t.slug,
                   COUNT(CASE WHEN p.status = 'PUBLISHED' THEN 1 END) AS postCount,
                   MAX(CASE WHEN p.status = 'PUBLISHED' THEN p.update_time END) AS latestPostTime
            FROM tag t
            LEFT JOIN post_tag pt ON pt.tag_id = t.id
            LEFT JOIN post p ON p.id = pt.post_id
            GROUP BY t.id, t.name, t.slug
            ORDER BY postCount DESC, lower(t.name) ASC
            """)
    List<TagSummary> listSummaries();

    @Select("""
            SELECT p.* FROM post p
            JOIN post_tag pt ON pt.post_id = p.id
            JOIN tag t ON t.id = pt.tag_id
            WHERE lower(t.slug) = lower(#{slug}) AND p.status = 'PUBLISHED'
            ORDER BY p.create_time DESC
            """)
    List<Post> listPublishedPosts(@Param("slug") String slug);

    @Insert("""
            INSERT OR IGNORE INTO post_tag(post_id, tag_id)
            SELECT post_id, #{targetId} FROM post_tag WHERE tag_id = #{sourceId}
            """)
    int mergeRelations(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);

    @Delete("DELETE FROM post_tag WHERE tag_id = #{tagId}")
    int deleteRelationsForTag(@Param("tagId") Long tagId);

    @Select("SELECT COUNT(*) FROM post_tag WHERE tag_id = #{tagId}")
    int usageCount(@Param("tagId") Long tagId);
}
