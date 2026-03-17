package com.dreamarchive.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAvatar implements Serializable {
    private Long id;// 头像ID
    private Long userId;// 用户ID
    private String avatarUrl;// 头像URL
    private Integer isCurrent;// 是否当前头像
    private LocalDateTime createTime;// 创建时间
}
