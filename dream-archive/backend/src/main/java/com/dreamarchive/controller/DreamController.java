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
            return Result.error("Failed to load public dreams: " + e.getMessage());
        }
    }

    @GetMapping("/hot")
    public Result<List<Dream>> getHotDreams(@RequestParam(defaultValue = "10") int limit) {
        try {
            return Result.success(dreamService.getHotDreams(limit));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Failed to load hot dreams: " + e.getMessage());
        }
    }

    @GetMapping("/{id:\\d+}")
    public Result<Dream> getDreamById(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Dream dream = dreamService.getDreamById(id, userId);
            if (dream == null) {
                return Result.error("Dream does not exist");
            }
            return Result.success(dream);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Failed to load dream detail: " + e.getMessage());
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
            return Result.error("Failed to load user dreams: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public Result<Dream> createDream(@RequestBody Dream dream, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "Unauthorized");
            }
            dream.setUserId(userId);
            return Result.success("Create success", dreamService.createDream(dream));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Create failed: " + e.getMessage());
        }
    }

    @PutMapping("/{id:\\d+}")
    public Result<Void> updateDream(@PathVariable Long id, @RequestBody Dream dream, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "Unauthorized");
            }
            Dream existing = dreamMapper.findById(id);
            if (existing == null) {
                return Result.error("Dream does not exist");
            }
            if (!isAdmin(request) && !userId.equals(existing.getUserId())) {
                return Result.error(403, "Permission denied");
            }
            dream.setId(id);
            dream.setUserId(existing.getUserId());
            boolean success = dreamService.updateDream(dream);
            return success ? Result.success("Update success", null) : Result.error("Update failed");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Update failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public Result<Void> deleteDream(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "Unauthorized");
            }
            boolean success = dreamService.deleteDream(id, userId);
            return success ? Result.success("Delete success", null) : Result.error("Delete failed");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Delete failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id:\\d+}/like")
    public Result<Boolean> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "Unauthorized");
            }
            boolean isLiked = dreamService.toggleLike(id, userId);
            return Result.success(isLiked ? "Like success" : "Unlike success", isLiked);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Action failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id:\\d+}/analyze")
    public Result<DreamAnalyzeRoomResponse> analyzeDream(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "Unauthorized");
            }
            Dream dream = dreamMapper.findById(id);
            if (dream == null) {
                return Result.error("Dream does not exist");
            }
            if (!isAdmin(request) && !userId.equals(dream.getUserId())) {
                return Result.error(403, "Permission denied");
            }

            boolean ok = dreamService.analyzeDream(id);
            if (!ok) {
                return Result.error("Analyze failed");
            }

            Dream refreshed = dreamMapper.findById(id);
            DreamRoom room = dreamRoomService.initRoomAfterAnalyze(refreshed);
            DreamAnalyzeRoomResponse response = new DreamAnalyzeRoomResponse(
                    id,
                    room != null ? room.getDreamRoomId() : "",
                    room != null ? room.getDreamRoomStatus() : 0,
                    room != null
            );
            return Result.success("Analyze success", response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Analyze failed: " + e.getMessage());
        }
    }

    @PostMapping("/analyze/batch")
    public Result<Integer> analyzeDreamBatch(@RequestParam(defaultValue = "50") int limit) {
        try {
            int done = dreamService.analyzeDreamsBatch(limit);
            return Result.success("Batch analyze success", done);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("Batch analyze failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload-image")
    public Result<String> uploadDreamImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("File is empty");
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
            return Result.success("Upload success", imageUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("Upload failed: " + e.getMessage());
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
