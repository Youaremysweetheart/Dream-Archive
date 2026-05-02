package com.dreamarchive.dto.dreamroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 发送消息受理结果：是否入队成功及当前房间状态。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DreamRoomSendResponse {

    @JsonProperty("accepted")
    private Boolean accepted;

    @JsonProperty("dream_room_id")
    private String dreamRoomId;

    @JsonProperty("dream_room_status")
    private Integer dreamRoomStatus;

    @JsonProperty("has_pending_reply")
    private Boolean hasPendingReply;
}
