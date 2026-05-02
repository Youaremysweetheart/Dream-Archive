package com.dreamarchive.controller;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.common.Result;
import com.dreamarchive.entity.Dream;
import com.dreamarchive.entity.DreamRoom;
import com.dreamarchive.entity.User;
import com.dreamarchive.mapper.CommentMapper;
import com.dreamarchive.mapper.DreamMapper;
import com.dreamarchive.mapper.DreamRoomMapper;
import com.dreamarchive.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DreamMapper dreamMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private DreamRoomMapper dreamRoomMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return Result.error(auth.getCode(), auth.getMessage());
        }

        try {
            Map<String, Object> stats = new HashMap<>();
            int adminUsers = userMapper.countByRoleIgnoreCase("ADMIN");
            int totalUsers = userMapper.countAllUsers();

            stats.put("totalUsers", totalUsers);
            stats.put("totalDreams", dreamMapper.countAllDreams());
            stats.put("totalComments", commentMapper.countAllComments());
            stats.put("publicDreams", dreamMapper.countPublicDreams());
            stats.put("privateDreams", dreamMapper.countPrivateDreams());
            stats.put("adminUsers", adminUsers);
            stats.put("normalUsers", totalUsers - adminUsers);
            stats.put("totalDreamRooms", dreamRoomMapper.countAllRooms());
            stats.put("bannedDreamRooms", dreamRoomMapper.countBannedRooms());
            stats.put("todayUsers", userMapper.countTodayUsers());
            stats.put("todayDreams", dreamMapper.countTodayDreams());
            stats.put("todayComments", commentMapper.countTodayComments());
            return Result.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    public Result<PageResult<User>> getAllUsers(HttpServletRequest request,
                                                @RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "20") int pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "id") String sortBy,
                                                @RequestParam(defaultValue = "asc") String sortOrder) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return Result.error(auth.getCode(), auth.getMessage());
        }

        try {
            int offset = (pageNum - 1) * pageSize;
            String normalizedSortBy = normalizeSortBy(sortBy);
            String normalizedSortOrder = normalizeSortOrder(sortOrder);
            List<User> users = userMapper.findAdminPage(
                    keyword, role, status, normalizedSortBy, normalizedSortOrder, offset, pageSize
            );
            int total = userMapper.countAdminPage(keyword, role, status);
            users.forEach(u -> u.setPassword(null));
            return Result.success(new PageResult<>((long) total, pageNum, pageSize, users));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/dreams")
    public Result<PageResult<Dream>> getAllDreams(HttpServletRequest request,
                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "20") int pageSize) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return Result.error(auth.getCode(), auth.getMessage());
        }

        try {
            int offset = (pageNum - 1) * pageSize;
            List<Dream> dreams = dreamMapper.findAllForAdmin(offset, pageSize);
            int total = dreamMapper.countAllDreams();
            return Result.success(new PageResult<>((long) total, pageNum, pageSize, dreams));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取梦境列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/dream-rooms")
    public Result<PageResult<DreamRoom>> getDreamRooms(HttpServletRequest request,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer status) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return Result.error(auth.getCode(), auth.getMessage());
        }

        try {
            int offset = (pageNum - 1) * pageSize;
            List<DreamRoom> rooms = dreamRoomMapper.findAdminPage(keyword, status, offset, pageSize);
            int total = dreamRoomMapper.countAdminPage(keyword, status);
            return Result.success(new PageResult<>((long) total, pageNum, pageSize, rooms));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取心理辅导室列表失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/dream/{id}")
    public Result<Void> deleteDream(@PathVariable Long id, HttpServletRequest request) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return auth;
        }

        try {
            dreamMapper.delete(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // 删除用户
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return auth;
        }

        try {
            Long adminId = getCurrentUserId(request);
            if (id.equals(adminId)) {
                return Result.error("不能删除当前管理员账号");
            }

            User target = userMapper.findById(id);
            if (target == null) {
                return Result.error("用户不存在");
            }
            if (isAdminRole(target.getRole())) {
                return Result.error("不能删除管理员账号");
            }

            userMapper.delete(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    // 更新用户角色
    @PutMapping("/user/{id}/role")
    public Result<User> updateUserRole(@PathVariable Long id,
                                       HttpServletRequest request,
                                       @RequestBody Map<String, String> params) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return Result.error(auth.getCode(), auth.getMessage());
        }

        try {
            Long adminId = getCurrentUserId(request);
            if (id.equals(adminId)) {
                return Result.error("不能修改自己的角色");
            }

            String role = normalizeRole(params.get("role"));
            if (role == null) {
                return Result.error("角色必须是管理员或普通用户");
            }

            User user = userMapper.findById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }

            userMapper.updateRole(id, role);
            user = userMapper.findById(id);
            user.setPassword(null);
            return Result.success("更新成功", user);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @PutMapping("/user/{id}/status")
    public Result<User> updateUserStatus(@PathVariable Long id,
                                         HttpServletRequest request,
                                         @RequestBody Map<String, Integer> params) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return Result.error(auth.getCode(), auth.getMessage());
        }

        try {
            Long adminId = getCurrentUserId(request);
            Integer status = params.get("status");
            if (status == null || (status != 0 && status != 1)) {
                return Result.error("状态必须是 0（禁用）或 1（正常）");
            }

            if (id.equals(adminId) && status == 0) {
                return Result.error("不能禁用当前管理员账号");
            }

            User user = userMapper.findById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }

            userMapper.updateStatus(id, status);
            user = userMapper.findById(id);
            user.setPassword(null);
            return Result.success("更新成功", user);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/dreams/batch-delete")
    public Result<Void> batchDeleteDreams(HttpServletRequest request,
                                          @RequestBody Map<String, List<Long>> params) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return auth;
        }

        try {
            List<Long> ids = params.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("列表不能为空");
            }

            for (Long id : ids) {
                dreamMapper.delete(id);
            }
            return Result.success("批量删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }

    @PutMapping("/dream-room/{dreamRoomId}/ban")
    public Result<DreamRoom> banDreamRoom(@PathVariable String dreamRoomId,
                                          HttpServletRequest request,
                                          @RequestBody Map<String, String> params) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return Result.error(auth.getCode(), auth.getMessage());
        }

        try {
            String reason = params == null ? null : params.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return Result.error("封禁原因不能为空");
            }

            DreamRoom room = dreamRoomMapper.findByDreamRoomId(dreamRoomId);
            if (room == null) {
                return Result.error("心理辅导室不存在");
            }

            dreamRoomMapper.banRoom(dreamRoomId, reason.trim());
            return Result.success("封禁成功", dreamRoomMapper.findByDreamRoomId(dreamRoomId));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("封禁失败: " + e.getMessage());
        }
    }

    @PutMapping("/dream-room/{dreamRoomId}/unban")
    public Result<DreamRoom> unbanDreamRoom(@PathVariable String dreamRoomId,
                                            HttpServletRequest request) {
        Result<Void> auth = checkAdmin(request);
        if (auth != null) {
            return Result.error(auth.getCode(), auth.getMessage());
        }

        try {
            DreamRoom room = dreamRoomMapper.findByDreamRoomId(dreamRoomId);
            if (room == null) {
                return Result.error("心理辅导室不存在");
            }

            int nextStatus = room.getOpeningMessageGenerated() != null && room.getOpeningMessageGenerated() == 1 ? 2 : 1;
            int nextOpeningGenerated = room.getOpeningMessageGenerated() == null ? 0 : room.getOpeningMessageGenerated();
            dreamRoomMapper.recoverRoom(dreamRoomId, nextStatus, nextOpeningGenerated);
            return Result.success("解禁成功", dreamRoomMapper.findByDreamRoomId(dreamRoomId));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("解禁失败: " + e.getMessage());
        }
    }

    private Result<Void> checkAdmin(HttpServletRequest request) {
        Long adminId = getCurrentUserId(request);
        if (adminId == null) {
            return Result.error(401, "管理员编号不能为空");
        }

        User admin = userMapper.findById(adminId);
        if (admin == null) {
            return Result.error(401, " 管理员不存在");
        }
        if (!isAdminRole(admin.getRole())) {
            return Result.error(403, "没有管理员权限");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            return Result.error(403, "管理员账号已被禁用");
        }

        return null;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private boolean isAdminRole(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String value = role.trim().toUpperCase();
        if (!"ADMIN".equals(value) && !"USER".equals(value)) {
            return null;
        }
        return value;
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) {
            return "id";
        }
        String value = sortBy.trim().toLowerCase();
        if (!"id".equals(value) && !"username".equals(value)) {
            return "id";
        }
        return value;
    }

    private String normalizeSortOrder(String sortOrder) {
        if (sortOrder == null) {
            return "asc";
        }
        String value = sortOrder.trim().toLowerCase();
        if (!"asc".equals(value) && !"desc".equals(value)) {
            return "asc";
        }
        return value;
    }
}
