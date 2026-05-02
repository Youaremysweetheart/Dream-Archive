package com.dreamarchive.mapper;

import com.dreamarchive.entity.DreamRoom;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 疏导房间主表：按用户与帖子查询、状态迁移、封禁与恢复。 */
@Mapper
public interface DreamRoomMapper {

    @Select("SELECT id, dream_room_id, user_id, dream_post_id, dream_room_status, opening_message_generated, " +
            "dify_conversation_id, banned_reason, create_time, update_time " +
            "FROM dream_room WHERE user_id = #{userId} AND dream_post_id = #{dreamPostId}")
    DreamRoom findByUserAndDreamPost(@Param("userId") Long userId, @Param("dreamPostId") Long dreamPostId);

    @Select("SELECT id, dream_room_id, user_id, dream_post_id, dream_room_status, opening_message_generated, " +
            "dify_conversation_id, banned_reason, create_time, update_time " +
            "FROM dream_room WHERE dream_room_id = #{dreamRoomId}")
    DreamRoom findByDreamRoomId(@Param("dreamRoomId") String dreamRoomId);

    @Select("SELECT id, dream_room_id, user_id, dream_post_id, dream_room_status, opening_message_generated, " +
            "dify_conversation_id, banned_reason, create_time, update_time " +
            "FROM dream_room WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<DreamRoom> findByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT dr.id, dr.dream_room_id, dr.user_id, dr.dream_post_id, dr.dream_room_status, " +
            "dr.opening_message_generated, dr.dify_conversation_id, dr.banned_reason, dr.create_time, dr.update_time, " +
            "u.username, u.email, d.title AS dream_title, d.create_time AS dream_create_time, " +
            "(SELECT COUNT(*) FROM dream_room_message drm WHERE drm.dream_room_id = dr.dream_room_id) AS message_count " +
            "FROM dream_room dr " +
            "LEFT JOIN user u ON dr.user_id = u.id " +
            "LEFT JOIN dream d ON dr.dream_post_id = d.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (dr.dream_room_id LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.email LIKE CONCAT('%', #{keyword}, '%') " +
            "OR d.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "<if test='status != null'> AND dr.dream_room_status = #{status} </if> " +
            "ORDER BY dr.update_time DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<DreamRoom> findAdminPage(@Param("keyword") String keyword,
                                  @Param("status") Integer status,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM dream_room dr " +
            "LEFT JOIN user u ON dr.user_id = u.id " +
            "LEFT JOIN dream d ON dr.dream_post_id = d.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (dr.dream_room_id LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.email LIKE CONCAT('%', #{keyword}, '%') " +
            "OR d.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "<if test='status != null'> AND dr.dream_room_status = #{status} </if> " +
            "</script>")
    int countAdminPage(@Param("keyword") String keyword,
                       @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM dream_room")
    int countAllRooms();

    @Select("SELECT COUNT(*) FROM dream_room WHERE dream_room_status = 3")
    int countBannedRooms();

    @Insert("INSERT INTO dream_room (dream_room_id, user_id, dream_post_id, dream_room_status, opening_message_generated, " +
            "dify_conversation_id, banned_reason) VALUES (#{dreamRoomId}, #{userId}, #{dreamPostId}, #{dreamRoomStatus}, " +
            "#{openingMessageGenerated}, #{difyConversationId}, #{bannedReason})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DreamRoom room);

    @Update("UPDATE dream_room SET dream_room_status = #{status}, update_time = NOW() WHERE dream_room_id = #{dreamRoomId}")
    int updateStatus(@Param("dreamRoomId") String dreamRoomId, @Param("status") Integer status);

    @Update("UPDATE dream_room SET dream_room_status = #{status}, opening_message_generated = #{openingGenerated}, " +
            "update_time = NOW() WHERE dream_room_id = #{dreamRoomId}")
    int updateStatusAndOpening(@Param("dreamRoomId") String dreamRoomId,
                               @Param("status") Integer status,
                               @Param("openingGenerated") Integer openingGenerated);

    @Update("UPDATE dream_room SET dream_room_status = #{toStatus}, update_time = NOW() " +
            "WHERE dream_room_id = #{dreamRoomId} AND dream_room_status = #{fromStatus}")
    int updateStatusIfMatch(@Param("dreamRoomId") String dreamRoomId,
                            @Param("fromStatus") Integer fromStatus,
                            @Param("toStatus") Integer toStatus);

    @Update("UPDATE dream_room SET opening_message_generated = #{openingGenerated}, update_time = NOW() " +
            "WHERE dream_room_id = #{dreamRoomId}")
    int updateOpeningGenerated(@Param("dreamRoomId") String dreamRoomId, @Param("openingGenerated") Integer openingGenerated);

    @Update("UPDATE dream_room SET dream_room_status = 3, banned_reason = #{reason}, update_time = NOW() " +
            "WHERE dream_room_id = #{dreamRoomId}")
    int banRoom(@Param("dreamRoomId") String dreamRoomId, @Param("reason") String reason);

    @Update("UPDATE dream_room SET dream_room_status = #{status}, opening_message_generated = #{openingGenerated}, " +
            "banned_reason = NULL, update_time = NOW() WHERE dream_room_id = #{dreamRoomId}")
    int recoverRoom(@Param("dreamRoomId") String dreamRoomId,
                    @Param("status") Integer status,
                    @Param("openingGenerated") Integer openingGenerated);
}
