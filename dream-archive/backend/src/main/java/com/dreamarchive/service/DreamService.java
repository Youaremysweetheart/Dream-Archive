package com.dreamarchive.service;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.entity.Dream;

import java.util.List;

public interface DreamService {

    Dream createDream(Dream dream);

    Dream getDreamById(Long id, Long currentUserId);

    boolean updateDream(Dream dream);

    boolean deleteDream(Long id, Long userId);

    PageResult<Dream> getPublicDreams(Integer categoryId, String keyword, int pageNum, int pageSize, Long currentUserId);

    PageResult<Dream> getUserDreams(Long userId, int pageNum, int pageSize);

    List<Dream> getHotDreams(int limit);

    boolean toggleLike(Long dreamId, Long userId);

    boolean analyzeDream(Long dreamId);

    int analyzeDreamsBatch(int limit);
}
