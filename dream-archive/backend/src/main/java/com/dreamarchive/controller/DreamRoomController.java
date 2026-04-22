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

@RestController
@RequestMapping("/dream-room")
@CrossOrigin
public class DreamRoomController {

    private final DreamRoomService dreamRoomService;

    public DreamRoomController(DreamRoomService dreamRoomService) {
        this.dreamRoomService = dreamRoomService;
    }

    @PostMapping("/enter")
    public Result<DreamRoomEnterResponse> enterRoom(@RequestBody DreamRoomEnterRequest request,
                                                    HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "Unauthorized");
            }
            Long dreamPostId = request == null ? null : request.getDreamPostId();
            DreamRoomEnterResponse response = dreamRoomService.enterRoom(currentUserId, dreamPostId);
            return Result.success(response);
        } catch (Exception ex) {
            return Result.error("Failed to enter room: " + ex.getMessage());
        }
    }

    @PostMapping("/enter-by-post")
    public Result<DreamRoomEnterResponse> enterRoomByPost(@RequestBody DreamRoomEnterRequest request,
                                                          HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "Unauthorized");
            }
            Long dreamPostId = request == null ? null : request.getDreamPostId();
            DreamRoomEnterResponse response = dreamRoomService.enterRoomByPost(currentUserId, dreamPostId);
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (Exception ex) {
            return Result.error("Failed to enter room by post: " + ex.getMessage());
        }
    }

    @GetMapping("/history")
    public Result<List<DreamRoomHistoryItem>> getRoomHistory(@RequestParam(defaultValue = "20") int limit,
                                                             HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "Unauthorized");
            }
            List<DreamRoomHistoryItem> items = dreamRoomService.getRoomHistory(currentUserId, limit);
            return Result.success(items);
        } catch (Exception ex) {
            return Result.error("Failed to load room history: " + ex.getMessage());
        }
    }

    @PostMapping("/send")
    public Result<DreamRoomSendResponse> sendMessage(@RequestBody DreamRoomSendRequest request,
                                                      HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "Unauthorized");
            }
            if (request == null) {
                return Result.error(400, "Request body is required");
            }
            DreamRoomSendResponse response = dreamRoomService.sendMessage(currentUserId, request);
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (IllegalStateException ex) {
            return Result.error(409, ex.getMessage());
        } catch (Exception ex) {
            return Result.error("Failed to send message: " + ex.getMessage());
        }
    }

    @GetMapping("/messages")
    public Result<PageResult<DreamRoomMessage>> getMessages(@RequestParam("dream_room_id") String dreamRoomId,
                                                             @RequestParam(defaultValue = "1") int pageNum,
                                                             @RequestParam(defaultValue = "50") int pageSize,
                                                             HttpServletRequest httpServletRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpServletRequest);
            if (currentUserId == null) {
                return Result.error(401, "Unauthorized");
            }
            PageResult<DreamRoomMessage> result = dreamRoomService.getMessages(currentUserId, dreamRoomId, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception ex) {
            return Result.error("Failed to load messages: " + ex.getMessage());
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
