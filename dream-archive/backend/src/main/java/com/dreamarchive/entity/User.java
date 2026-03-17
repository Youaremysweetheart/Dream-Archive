package com.dreamarchive.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;// 用户ID
    private String username;// 用户名
    private String password;// 密码
    private String email;
    private String role;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 扩展字段（用于关联查询，不在数据库表中）
    private Integer dreamCount;      // 梦境数量
    private Integer commentCount;    // 评论数量
    private Integer likeCount;       // 获赞数量

    // Getter和Setter方法会由Lombok自动生成

    /**
     * 设置梦境数量
     * 注意：方法名必须是 setDreamCount，不是 setDreamCountInt
     */
    public void setDreamCount(int count) {
        this.dreamCount = count;
    }

    public Integer getDreamCount() {
        return this.dreamCount;
    }
}