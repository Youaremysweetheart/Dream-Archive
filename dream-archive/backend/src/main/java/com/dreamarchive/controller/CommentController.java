package com.dreamarchive.controller;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.common.Result;
import com.dreamarchive.entity.Comment;
import com.dreamarchive.mapper.CommentMapper;
import com.dreamarchive.mapper.DreamMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comment")
@CrossOrigin
public class CommentController {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private DreamMapper dreamMapper;

    // 新评论
    @PostMapping("/create")
    public Result<Comment> createComment(@RequestBody Comment comment, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或登录已过期");
            }

            if (comment.getDreamId() == null || comment.getContent() == null
                    || comment.getContent().trim().isEmpty()) {
                return Result.error("参数不完整");
            }
            if (comment.getParentId() == null) {
                comment.setParentId(0L);
            }
            comment.setUserId(userId);
            commentMapper.insert(comment);
            dreamMapper.refreshCommentCount(comment.getDreamId());
            return Result.success("评论成功", comment);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("评论失败: " + e.getMessage());
        }
    }

    // 获取
    @GetMapping("/dream/{dreamId}")
    public Result<PageResult<Comment>> getCommentsByDream(
            @PathVariable Long dreamId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            List<Comment> all = commentMapper.findByDreamId(dreamId);
            if (all == null) {
                all = Collections.emptyList();
            }
            dreamMapper.refreshCommentCount(dreamId);

            // Build two-level tree: top-level comments + replies.
            Map<Long, List<Comment>> repliesMap = all.stream()
                    .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                    .collect(Collectors.groupingBy(Comment::getParentId));

            List<Comment> topLevel = all.stream()
                    .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                    .collect(Collectors.toList());

            for (Comment c : topLevel) {
                List<Comment> replies = repliesMap.get(c.getId());
                c.setReplies(replies == null ? new ArrayList<>() : replies);
            }

            int start = Math.max((pageNum - 1) * pageSize, 0);
            int end = Math.min(start + pageSize, topLevel.size());
            List<Comment> page = start >= topLevel.size() ? Collections.emptyList() : topLevel.subList(start, end);

            PageResult<Comment> result = new PageResult<>((long) topLevel.size(), pageNum, pageSize, page);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取评论失败: " + e.getMessage());
        }
    }

    // 获取用户
    @GetMapping("/user/{userId}")
    public Result<List<Comment>> getCommentsByUser(@PathVariable Long userId) {
        try {
            return Result.success(commentMapper.findByUserId(userId));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取用户评论失败: " + e.getMessage());
        }
    }

    // 删除
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或登录已过期");
            }
            Comment existing = commentMapper.findById(id);
            if (existing == null) {
                return Result.error("评论不存在");
            }
            if (!isAdmin(request) && !userId.equals(existing.getUserId())) {
                return Result.error(403, "无权限删除该评论");
            }

            commentMapper.deleteByParentId(id);
            int affected = commentMapper.delete(id);
            if (affected <= 0) {
                return Result.error("删除失败");
            }

            dreamMapper.refreshCommentCount(existing.getDreamId());
            return Result.success("删除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
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

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("currentUserRole");
        return role instanceof String s && "ADMIN".equalsIgnoreCase(s.trim());
    }
}
