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

public interface DreamRoomService {

    DreamRoom initRoomAfterAnalyze(Dream dream);

    DreamRoomEnterResponse enterRoom(Long currentUserId, Long dreamPostId);

    DreamRoomEnterResponse enterRoomByPost(Long currentUserId, Long dreamPostId);

    List<DreamRoomHistoryItem> getRoomHistory(Long currentUserId, int limit);

    DreamRoomSendResponse sendMessage(Long currentUserId, DreamRoomSendRequest request);

    PageResult<DreamRoomMessage> getMessages(Long currentUserId, String dreamRoomId, int pageNum, int pageSize);
}
