package com.dreamarchive.mapper;

import com.dreamarchive.entity.DreamRoom;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
}
