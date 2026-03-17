package com.dreamarchive.controller;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.common.Result;
import com.dreamarchive.entity.Dream;
import com.dreamarchive.service.DreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
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

    @Autowired
    private DreamService dreamService;

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    // 获取公开梦境
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
            return Result.error("get public dreams failed: " + e.getMessage());
        }
    }

    // 获取最热梦境
    @GetMapping("/hot")
    public Result<List<Dream>> getHotDreams(@RequestParam(defaultValue = "10") int limit) {
        try {
            return Result.success(dreamService.getHotDreams(limit));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("get hot dreams failed: " + e.getMessage());
        }
    }

    // 获取梦境详情
    @GetMapping("/{id:\\d+}")
    public Result<Dream> getDreamById(@PathVariable Long id,
                                      @RequestParam(required = false) Long userId) {
        try {
            Dream dream = dreamService.getDreamById(id, userId);
            if (dream == null) {
                return Result.error("dream not found");
            }
            return Result.success(dream);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("get dream detail failed: " + e.getMessage());
        }
    }

    // 获取用户梦境
    @GetMapping("/user/{userId}")
    public Result<PageResult<Dream>> getUserDreams(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            return Result.success(dreamService.getUserDreams(userId, pageNum, pageSize));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("get user dreams failed: " + e.getMessage());
        }
    }

    // 创建梦境
    @PostMapping("/create")
    public Result<Dream> createDream(@RequestBody Dream dream) {
        try {
            return Result.success("create success", dreamService.createDream(dream));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("create failed: " + e.getMessage());
        }
    }

    // 更新梦境
    @PutMapping("/{id:\\d+}")
    public Result<Void> updateDream(@PathVariable Long id, @RequestBody Dream dream) {
        try {
            dream.setId(id);
            boolean success = dreamService.updateDream(dream);
            return success ? Result.success("update success", null) : Result.error("update failed");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("update failed: " + e.getMessage());
        }
    }

    // 删除梦境
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> deleteDream(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                return Result.error("userId is required");
            }
            boolean success = dreamService.deleteDream(id, userId);
            return success ? Result.success("delete success", null) : Result.error("delete failed");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("delete failed: " + e.getMessage());
        }
    }

    // 点赞/取消点赞
    @PostMapping("/{id:\\d+}/like")
    public Result<Boolean> toggleLike(@PathVariable Long id, @RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                return Result.error("userId is required");
            }
            boolean isLiked = dreamService.toggleLike(id, userId);
            return Result.success(isLiked ? "like success" : "cancel like", isLiked);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("operate failed: " + e.getMessage());
        }
    }

    // 重新分析梦境
    @PostMapping("/{id:\\d+}/analyze")
    public Result<Void> analyzeDream(@PathVariable Long id) {
        try {
            boolean ok = dreamService.analyzeDream(id);
            return ok ? Result.success("analyze success", null) : Result.error("analyze failed");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("analyze failed: " + e.getMessage());
        }
    }

    // 批量分析梦境（只分析尚未分析的记录）
    @PostMapping("/analyze/batch")
    public Result<Integer> analyzeDreamBatch(@RequestParam(defaultValue = "50") int limit) {
        try {
            int done = dreamService.analyzeDreamsBatch(limit);
            return Result.success("batch analyze success", done);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("batch analyze failed: " + e.getMessage());
        }
    }

    // 上传梦境图片
    @PostMapping("/upload-image")
    public Result<String> uploadDreamImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("file is empty");
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
            return Result.success("upload success", imageUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("upload failed: " + e.getMessage());
        }
    }

    private String buildUploadDir(String subFolder) {
        String base = uploadPath == null ? "uploads/" : uploadPath;
        String norm = base.replace("\\", "/");
        if (!norm.endsWith("/")) norm = norm + "/";
        return norm + subFolder + "/";
    }
}
