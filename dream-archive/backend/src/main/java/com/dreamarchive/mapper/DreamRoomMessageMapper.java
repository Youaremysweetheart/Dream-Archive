package com.dreamarchive.mapper;

import com.dreamarchive.entity.DreamRoomMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 房间内聊天记录：插入与按房间分页查询。 */
@Mapper
public interface DreamRoomMessageMapper {

    @Insert("INSERT INTO dream_room_message (dream_room_id, user_id, dream_post_id, sender_id, message_role, " +
            "message_text, is_violation, client_msg_id) VALUES (#{dreamRoomId}, #{userId}, #{dreamPostId}, " +
            "#{senderId}, #{messageRole}, #{messageText}, #{isViolation}, #{clientMsgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DreamRoomMessage message);

    @Select("SELECT id, dream_room_id, user_id, dream_post_id, sender_id, message_role, message_text, is_violation, " +
            "client_msg_id, create_time FROM dream_room_message WHERE dream_room_id = #{dreamRoomId} " +
            "ORDER BY id ASC LIMIT #{offset}, #{size}")
    List<DreamRoomMessage> findByRoomPage(@Param("dreamRoomId") String dreamRoomId,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    @Select("SELECT COUNT(*) FROM dream_room_message WHERE dream_room_id = #{dreamRoomId}")
    long countByRoom(@Param("dreamRoomId") String dreamRoomId);

    @Select("SELECT COUNT(*) FROM dream_room_message WHERE dream_room_id = #{dreamRoomId} AND message_role = #{messageRole}")
    long countByRoomAndRole(@Param("dreamRoomId") String dreamRoomId, @Param("messageRole") Integer messageRole);
}
