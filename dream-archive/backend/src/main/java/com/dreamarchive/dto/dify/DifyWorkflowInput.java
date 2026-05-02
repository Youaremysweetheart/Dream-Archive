package com.dreamarchive.dto.dify;

import lombok.Data;

/** 调用 Dify 工作流时组装的输入变量（与平台侧 input 名一致）。 */
@Data
public class DifyWorkflowInput {

    private Long userId;
    private Long dreamPostId;
    private String dreamRoomId;
    private Integer dreamRoomStatus;
    private String dreamPostContent;
    private String question;
}
