package com.dreamarchive.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 疏导房间实体：与用户、梦境帖子一对一关联，记录状态与封禁原因。 */
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

    private String username;
    private String email;
    private String dreamTitle;
    private LocalDateTime dreamCreateTime;
    private Long messageCount;
}
