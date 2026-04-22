package com.dreamarchive.dto.dreamroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DreamRoomEnterRequest {

    @JsonProperty("dream_post_id")
    private Long dreamPostId;
}
