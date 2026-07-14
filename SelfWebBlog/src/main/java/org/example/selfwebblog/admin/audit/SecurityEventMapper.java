package org.example.selfwebblog.admin.audit;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SecurityEventMapper {
    @Insert("""
            INSERT INTO security_event
                (actor_user_id, action, target_type, target_id, detail, ip_hash, create_time)
            VALUES
                (#{actorUserId}, #{action}, #{targetType}, #{targetId}, #{detail}, #{ipHash}, #{createTime})
            """)
    int insert(EventWrite event);

    @Select("""
            SELECT id, actor_user_id, action, target_type, target_id, detail, ip_hash, create_time
            FROM security_event
            ORDER BY create_time DESC, id DESC
            LIMIT #{limit}
            """)
    List<SecurityEvent> listRecent(@Param("limit") int limit);

    record EventWrite(
            Long id,
            Long actorUserId,
            String action,
            String targetType,
            String targetId,
            String detail,
            String ipHash,
            java.time.LocalDateTime createTime) {
        public EventWrite(Long actorUserId, String action, String targetType, String targetId, String detail, String ipHash) {
            this(null, actorUserId, action, targetType, targetId, detail, ipHash, java.time.LocalDateTime.now());
        }
    }
}
