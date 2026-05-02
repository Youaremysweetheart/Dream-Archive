package com.dreamarchive.service;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.dto.dreamroom.DreamRoomHistoryItem;
import com.dreamarchive.dto.dreamroom.DreamRoomEnterResponse;
import com.dreamarchive.dto.dreamroom.DreamRoomSendRequest;
import com.dreamarchive.dto.dreamroom.DreamRoomSendResponse;
import com.dreamarchive.entity.Dream;
import com.dreamarchive.entity.DreamRoom;
import com.dreamarchive.entity.DreamRoomMessage;

import java.util.List;

/** 梦境 AI 疏导房间：分析与建房间、进入、历史、发消息、分页消息。 */
public interface DreamRoomService {

    /** 情感分析完成后初始化或重置对应疏导房间（异常态）。 */
    DreamRoom initRoomAfterAnalyze(Dream dream);

    /** 按今日梦境或默认规则进入房间。 */
    DreamRoomEnterResponse enterRoom(Long currentUserId, Long dreamPostId);

    /** 指定梦境帖子 ID 进入房间（须本人）。 */
    DreamRoomEnterResponse enterRoomByPost(Long currentUserId, Long dreamPostId);

    /** 当前用户近期梦境及房间摘要。 */
    List<DreamRoomHistoryItem> getRoomHistory(Long currentUserId, int limit);

    /** 提交用户消息并入队 AI 回答任务。 */
    DreamRoomSendResponse sendMessage(Long currentUserId, DreamRoomSendRequest request);

    /** 分页查询某房间内消息。 */
    PageResult<DreamRoomMessage> getMessages(Long currentUserId, String dreamRoomId, int pageNum, int pageSize);
}
