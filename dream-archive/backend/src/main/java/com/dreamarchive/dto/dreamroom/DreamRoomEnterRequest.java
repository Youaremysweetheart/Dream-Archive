package com.dreamarchive.dto.dreamroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** 进入疏导房间请求体：可选指定梦境帖子 ID。 */
@Data
public class DreamRoomEnterRequest {

    @JsonProperty("dream_post_id")
    private Long dreamPostId;
}
