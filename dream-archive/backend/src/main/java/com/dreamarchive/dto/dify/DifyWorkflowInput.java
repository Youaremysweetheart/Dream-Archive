package com.dreamarchive.dto.dify;

import lombok.Data;

@Data
public class DifyWorkflowInput {

    private Long userId;
    private Long dreamPostId;
    private String dreamRoomId;
    private Integer dreamRoomStatus;
    private String dreamPostContent;
    private String question;
}
