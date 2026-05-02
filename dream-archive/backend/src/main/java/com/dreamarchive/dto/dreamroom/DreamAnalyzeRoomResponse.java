package com.dreamarchive.dto.dreamroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 梦境分析完成后返回：帖子 ID、房间 ID、房间状态、是否可展示入口。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DreamAnalyzeRoomResponse {

    @JsonProperty("dream_post_id")
    private Long dreamPostId;

    @JsonProperty("dream_room_id")
    private String dreamRoomId;

    @JsonProperty("dream_room_status")
    private Integer dreamRoomStatus;

    @JsonProperty("entry_visible")
    private Boolean entryVisible;
}
