package com.dreamarchive.dto.dreamroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DreamRoomHistoryItem {

    @JsonProperty("dream_post_id")
    private Long dreamPostId;

    @JsonProperty("dream_title")
    private String dreamTitle;

    @JsonProperty("dream_date")
    private LocalDate dreamDate;

    @JsonProperty("dream_create_time")
    private LocalDateTime dreamCreateTime;

    @JsonProperty("dream_room_id")
    private String dreamRoomId;

    @JsonProperty("dream_room_status")
    private Integer dreamRoomStatus;

    @JsonProperty("room_exists")
    private Boolean roomExists;
}
