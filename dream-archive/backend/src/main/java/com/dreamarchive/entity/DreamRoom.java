package com.dreamarchive.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DreamRoom implements Serializable {

    private Long id;
    private String dreamRoomId;
    private Long userId;
    private Long dreamPostId;
    private Integer dreamRoomStatus;
    private Integer openingMessageGenerated;
    private String difyConversationId;
    private String bannedReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
