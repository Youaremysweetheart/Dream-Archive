package com.dreamarchive.controller;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.common.Result;
import com.dreamarchive.dto.dreamroom.DreamRoomEnterRequest;
import com.dreamarchive.dto.dreamroom.DreamRoomEnterResponse;
import com.dreamarchive.dto.dreamroom.DreamRoomHistoryItem;
import com.dreamarchive.dto.dreamroom.DreamRoomSendRequest;
import com.dreamarchive.dto.dreamroom.DreamRoomSendResponse;
import com.dreamarchive.entity.DreamRoomMessage;
import com.dreamarchive.service.DreamRoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 梦境疏导房间 REST 接口：进入房间、历史列表、发送消息、分页拉取消息。
 */
@RestController
@RequestMapping("/dream-room")
@CrossOrigin
public class DreamRoomController {

    private final DreamRoomService dreamRoomService;

    public DreamRoomController(DreamRoomService dreamRoomService) {
        this.dreamRoomService = dreamRoomService;
    }

    /** 按「今日梦境」逻辑进入疏导房间（可与前端默认入口配合）。 */
    @PostMapping("/enter")
    public Result<DreamRoomEnterResponse> enterRoom(@RequestBody DreamRoomEnterRequest request,
                                                    HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            Long dreamPostId = request == null ? null : request.getDreamPostId();
            DreamRoomEnterResponse response = dreamRoomService.enterRoom(currentUserId, dreamPostId);
            return Result.success(response);
        } catch (Exception ex) {
            return Result.error("进入房间失败：" + ex.getMessage());
        }
    }

    /** 指定梦境帖子 ID 进入房间（需本人帖子）。 */
    @PostMapping("/enter-by-post")
    public Result<DreamRoomEnterResponse> enterRoomByPost(@RequestBody DreamRoomEnterRequest request,
                                                          HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            Long dreamPostId = request == null ? null : request.getDreamPostId();
            DreamRoomEnterResponse response = dreamRoomService.enterRoomByPost(currentUserId, dreamPostId);
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (Exception ex) {
            return Result.error("指定帖子进入房间失败：" + ex.getMessage());
        }
    }

    /** 当前用户近期梦境与对应房间摘要列表。 */
    @GetMapping("/history")
    public Result<List<DreamRoomHistoryItem>> getRoomHistory(@RequestParam(defaultValue = "20") int limit,
                                                             HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            List<DreamRoomHistoryItem> items = dreamRoomService.getRoomHistory(currentUserId, limit);
            return Result.success(items);
        } catch (Exception ex) {
            return Result.error("加载房间历史失败：" + ex.getMessage());
        }
    }

    /** 向房间发送用户问题，异步由队列调用 Dify 生成助手回复。 */
    @PostMapping("/send")
    public Result<DreamRoomSendResponse> sendMessage(@RequestBody DreamRoomSendRequest request,
                                                      HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            if (request == null) {
                return Result.error(400, "请求体不能为空");
            }
            DreamRoomSendResponse response = dreamRoomService.sendMessage(currentUserId, request);
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (IllegalStateException ex) {
            return Result.error(409, ex.getMessage());
        } catch (Exception ex) {
            return Result.error("发送消息失败：" + ex.getMessage());
        }
    }

    /** 分页查询房间内消息记录（仅房间所有者）。 */
    @GetMapping("/messages")
    public Result<PageResult<DreamRoomMessage>> getMessages(@RequestParam("dream_room_id") String dreamRoomId,
                                                             @RequestParam(defaultValue = "1") int pageNum,
                                                             @RequestParam(defaultValue = "50") int pageSize,
                                                             HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            PageResult<DreamRoomMessage> result = dreamRoomService.getMessages(currentUserId, dreamRoomId, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception ex) {
            return Result.error("加载消息失败：" + ex.getMessage());
        }
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value instanceof Long userId) {
            return userId;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
