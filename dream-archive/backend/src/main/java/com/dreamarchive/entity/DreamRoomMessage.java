package com.dreamarchive.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 房间内一条聊天消息（用户提问或助手回复等），含角色与违规标记。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DreamRoomMessage implements Serializable {

    private Long id;
    private String dreamRoomId;
    private Long userId;
    private Long dreamPostId;
    private Long senderId;
    private Integer messageRole;
    private String messageText;
    private Integer isViolation;
    private String clientMsgId;
    private LocalDateTime createTime;
}
