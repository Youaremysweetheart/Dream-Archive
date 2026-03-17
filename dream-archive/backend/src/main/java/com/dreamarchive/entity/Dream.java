package com.dreamarchive.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dream implements Serializable {

    private Long id;
    private Long userId;
    private Integer categoryId;
    private String title;
    private String content;
    private String tags;
    private String images;
    private Integer moodScore;
    private Integer isPublic;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer status;
    private LocalDate dreamDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Integer analysisLabel;
    private String analysisLabelName;
    private Double analysisConfidence;
    private String analysisIntensity;
    private String analysisFeedback;
    private LocalDateTime analysisUpdatedAt;

    // extra fields
    private String username;
    private String userAvatar;
    private String categoryName;
    private String categoryColor;
    private Boolean isLiked;

    // transient fields
    private List<String> imageUrls;
}