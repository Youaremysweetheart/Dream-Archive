package com.dreamarchive.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment implements Serializable {

    private Long id;// 评论ID
    private Long dreamId;// 梦境ID
    private Long userId;// 用户ID
    private Long parentId;// 父级评论ID
    private String content;// 评论内容
    private Integer likeCount;// 点赞数
    private Integer status;// 状态
    private LocalDateTime createTime;// 创建时间

    private String username;// 用户名
    private String userAvatar;// 用户头像
    private List<Comment> replies;// 回复
    private Boolean isLiked;// 是否点赞
}
