package com.dreamarchive.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DreamRoomAiTask implements Serializable {

    private Long id;
    private String taskId;
    private String dreamRoomId;
    private Long userId;
    private Long dreamPostId;
    private String dreamPostContent;
    private Integer dreamRoomStatus;
    private String question;
    private String answer;
    private Integer isViolation;
    private Integer taskType;
    private Integer taskStatus;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
