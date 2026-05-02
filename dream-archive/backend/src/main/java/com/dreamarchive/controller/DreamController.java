package com.dreamarchive.controller;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.common.Result;
import com.dreamarchive.dto.dreamroom.DreamAnalyzeRoomResponse;
import com.dreamarchive.entity.Dream;
import com.dreamarchive.entity.DreamRoom;
import com.dreamarchive.mapper.DreamMapper;
import com.dreamarchive.service.DreamRoomService;
import com.dreamarchive.service.DreamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 梦境帖子 REST：公开列表、详情、用户列表、增删改、点赞、情感分析入口及图片上传。
 */
@RestController
@RequestMapping("/dream")
@CrossOrigin
public class DreamController {

    private final DreamService dreamService;
    private final DreamMapper dreamMapper;
    private final DreamRoomService dreamRoomService;

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    public DreamController(DreamService dreamService,
                           DreamMapper dreamMapper,
                           DreamRoomService dreamRoomService) {
        this.dreamService = dreamService;
        this.dreamMapper = dreamMapper;
        this.dreamRoomService = dreamRoomService;
    }

    @GetMapping("/public")
    public Result<PageResult<Dream>> getPublicDreams(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize) {
        try {
            PageResult<Dream> result = dreamService.getPublicDreams(categoryId, keyword, pageNum, pageSize, null);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("加载公开梦境列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/hot")
    public Result<List<Dream>> getHotDreams(@RequestParam(defaultValue = "10") int limit) {
        try {
            return Result.success(dreamService.getHotDreams(limit));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("加载热门梦境失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id:\\d+}")
    public Result<Dream> getDreamById(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Dream dream = dreamService.getDreamById(id, userId);
            if (dream == null) {
                return Result.error("梦境不存在");
            }
            return Result.success(dream);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("加载梦境详情失败：" + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public Result<PageResult<Dream>> getUserDreams(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            return Result.success(dreamService.getUserDreams(userId, pageNum, pageSize));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("加载用户梦境列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/create")
    public Result<Dream> createDream(@RequestBody Dream dream, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            dream.setUserId(userId);
            return Result.success("创建成功", dreamService.createDream(dream));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> updateDream(@PathVariable Long id, @RequestBody Dream dream, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            Dream existing = dreamMapper.findById(id);
            if (existing == null) {
                return Result.error("梦境不存在");
            }
            if (!isAdmin(request) && !userId.equals(existing.getUserId())) {
                return Result.error(403, "无权限操作");
            }
            dream.setId(id);
            dream.setUserId(existing.getUserId());
            boolean success = dreamService.updateDream(dream);
            return success ? Result.success("更新成功", null) : Result.error("更新失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> deleteDream(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            boolean success = dreamService.deleteDream(id, userId);
            return success ? Result.success("删除成功", null) : Result.error("删除失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id:\\d+}/like")
    public Result<Boolean> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            boolean isLiked = dreamService.toggleLike(id, userId);
            return Result.success(isLiked ? "点赞成功" : "已取消点赞", isLiked);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id:\\d+}/analyze")
    public Result<DreamAnalyzeRoomResponse> analyzeDream(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            Dream dream = dreamMapper.findById(id);
            if (dream == null) {
                return Result.error("梦境不存在");
            }
            if (!isAdmin(request) && !userId.equals(dream.getUserId())) {
                return Result.error(403, "无权限操作");
            }

            boolean ok = dreamService.analyzeDream(id);
            if (!ok) {
                return Result.error("分析失败");
            }

            Dream refreshed = dreamMapper.findById(id);
            DreamRoom room = dreamRoomService.initRoomAfterAnalyze(refreshed);
            DreamAnalyzeRoomResponse response = new DreamAnalyzeRoomResponse(
                    id,
                    room != null ? room.getDreamRoomId() : "",
                    room != null ? room.getDreamRoomStatus() : 0,
                    room != null
            );
            return Result.success("分析成功", response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("分析失败：" + e.getMessage());
        }
    }

    @PostMapping("/analyze/batch")
    public Result<Integer> analyzeDreamBatch(@RequestParam(defaultValue = "50") int limit) {
        try {
            int done = dreamService.analyzeDreamsBatch(limit);
            return Result.success("批量分析完成", done);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量分析失败：" + e.getMessage());
        }
    }

    @PostMapping("/upload-image")
    public Result<String> uploadDreamImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }

            String uploadDir = buildUploadDir("dreams");
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".png";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;
            Path path = Paths.get(uploadDir, filename);
            Files.copy(file.getInputStream(), path);

            String imageUrl = "/uploads/dreams/" + filename;
            return Result.success("上传成功", imageUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    private String buildUploadDir(String subFolder) {
        String base = uploadPath == null ? "uploads/" : uploadPath;
        String norm = base.replace("\\", "/");
        if (!norm.endsWith("/")) norm = norm + "/";
        return norm + subFolder + "/";
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

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("currentUserRole");
        return role instanceof String s && "ADMIN".equalsIgnoreCase(s.trim());
    }
}
