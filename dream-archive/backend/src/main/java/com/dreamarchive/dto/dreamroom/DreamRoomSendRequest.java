package com.dreamarchive.dto.dreamroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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
