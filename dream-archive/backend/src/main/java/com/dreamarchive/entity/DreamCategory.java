package com.dreamarchive.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 梦境分类实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DreamCategory implements Serializable {

    private Integer id;                 // 分类ID
    private String name;                // 分类名称
    private String description;         // 分类描述
    private String icon;                // 图标
    private String color;               // 标签颜色
    private Integer sortOrder;          // 排序
    private Integer status;             // 状态：1启用 0禁用
    private LocalDateTime createTime;   // 创建时间

    // 额外字段（不存数据库）
    private Long dreamCount;            // 该分类下的梦境数量
}
