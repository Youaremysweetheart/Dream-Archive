package com.dreamarchive.dto.dreamroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** 向房间发送用户消息：房间 ID、可选帖子校验、正文、客户端幂等 ID。 */
@Data
public class DreamRoomSendRequest {

    @JsonProperty("dream_room_id")
    private String dreamRoomId;

    @JsonProperty("dream_post_id")
    private Long dreamPostId;

    @JsonProperty("text")
    private String text;

    @JsonProperty("client_msg_id")
    private String clientMsgId;
}
