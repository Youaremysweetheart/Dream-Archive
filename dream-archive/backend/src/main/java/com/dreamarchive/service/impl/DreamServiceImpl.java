package com.dreamarchive.service.impl;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.entity.Dream;
import com.dreamarchive.mapper.DreamMapper;
import com.dreamarchive.mapper.LikeRecordMapper;
import com.dreamarchive.service.DreamService;
import com.dreamarchive.service.SentimentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DreamServiceImpl implements DreamService {

    @Autowired
    private DreamMapper dreamMapper;

    @Autowired
    private LikeRecordMapper likeRecordMapper;

    @Autowired
    private SentimentClient sentimentClient;

    @Override
    @Transactional
    public Dream createDream(Dream dream) {
        syncImagesToField(dream);
        dreamMapper.insert(dream);
        tryAnalyzeAndSave(dream);
        fillImageUrls(dream);
        return dream;
    }

    @Override
    public Dream getDreamById(Long id, Long currentUserId) {
        Dream dream = dreamMapper.findById(id);
        if (dream == null) {
            return null;
        }

        dreamMapper.incrementViewCount(id); //浏览量自增+1

        if (currentUserId != null) {
            boolean isLiked = likeRecordMapper.exists(currentUserId, id, 1);
            dream.setIsLiked(isLiked);
        }

        fillImageUrls(dream);
        return dream;
    }

    @Override
    public boolean updateDream(Dream dream) {
        syncImagesToField(dream);
        boolean updated = dreamMapper.update(dream) > 0;
        if (updated) {
            tryAnalyzeAndSave(dream);
        }
        return updated;
    }

    @Override
    public boolean deleteDream(Long id, Long userId) {
        Dream dream = dreamMapper.findById(id);
        if (dream == null || !dream.getUserId().equals(userId)) {
            return false;
        }
        return dreamMapper.delete(id) > 0;
    }

    @Override
    public PageResult<Dream> getPublicDreams(Integer categoryId, String keyword,
                                             int pageNum, int pageSize, Long currentUserId) {
        int offset = (pageNum - 1) * pageSize;
        List<Dream> dreams = dreamMapper.findPublicPage(categoryId, keyword, offset, pageSize);
        long total = dreamMapper.countPublic(categoryId, keyword);

        if (currentUserId != null) {
            for (Dream dream : dreams) {
                boolean isLiked = likeRecordMapper.exists(currentUserId, dream.getId(), 1);
                dream.setIsLiked(isLiked);
            }
        }

        fillImageUrls(dreams);
        return new PageResult<>(total, pageNum, pageSize, dreams);
    }

    @Override
    public PageResult<Dream> getUserDreams(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Dream> dreams = dreamMapper.findByUserId(userId, offset, pageSize);
        long total = dreamMapper.countByUserId(userId);

        fillImageUrls(dreams);
        return new PageResult<>(total, pageNum, pageSize, dreams);
    }

    @Override
    public List<Dream> getHotDreams(int limit) {
        List<Dream> dreams = dreamMapper.findHot(limit);
        fillImageUrls(dreams);
        return dreams;
    }

    @Override
    @Transactional
    public boolean toggleLike(Long dreamId, Long userId) {
        boolean exists = likeRecordMapper.exists(userId, dreamId, 1);

        if (exists) {
            likeRecordMapper.delete(userId, dreamId, 1);
            dreamMapper.decrementLikeCount(dreamId);
            return false;
        } else {
            likeRecordMapper.insert(userId, dreamId, 1);
            dreamMapper.incrementLikeCount(dreamId);
            return true;
        }
    }

    @Override
    public boolean analyzeDream(Long dreamId) {
        if (dreamId == null) return false;
        Dream dream = dreamMapper.findById(dreamId);
        if (dream == null) return false;
        tryAnalyzeAndSave(dream);
        return dream.getAnalysisLabelName() != null;
    }

    @Override
    public int analyzeDreamsBatch(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<Long> ids = dreamMapper.findIdsForAnalysis(safeLimit);
        int success = 0;
        for (Long id : ids) {
            if (id == null) continue;
            Dream dream = dreamMapper.findById(id);
            if (dream == null) continue;
            tryAnalyzeAndSave(dream);
            if (dream.getAnalysisLabelName() != null) {
                success++;
            }
        }
        return success;
    }

    private void syncImagesToField(Dream dream) {
        if (dream == null) return;
        List<String> urls = dream.getImageUrls();
        if (urls == null || urls.isEmpty()) {
            dream.setImages(null);
            return;
        }
        List<String> clean = new ArrayList<>();
        for (String u : urls) {
            if (u != null && !u.isBlank()) {
                clean.add(u.trim());
            }
        }
        dream.setImages(clean.isEmpty() ? null : String.join(",", clean));
    }

    private void fillImageUrls(List<Dream> dreams) {
        if (dreams == null) return;
        for (Dream dream : dreams) {
            fillImageUrls(dream);
        }
    }

    private void fillImageUrls(Dream dream) {
        if (dream == null) return;
        String images = dream.getImages();
        if (images == null || images.isBlank()) {
            dream.setImageUrls(Collections.emptyList());
            return;
        }

        String raw = images.trim();
        List<String> urls = new ArrayList<>();

        // Compatible with JSON-like history: ["/uploads/a.png","/uploads/b.png"]
        if (raw.startsWith("[") && raw.endsWith("]")) {
            raw = raw.substring(1, raw.length() - 1);
        }

        String[] arr = raw.split(",");
        for (String s : arr) {
            if (s == null) continue;
            String t = s.trim();
            if (t.startsWith("\"")) t = t.substring(1);
            if (t.endsWith("\"")) t = t.substring(0, t.length() - 1);
            if (!t.isBlank()) {
                urls.add(t);
            }
        }

        dream.setImageUrls(urls.isEmpty() ? Collections.emptyList() : urls);
    }

    private void tryAnalyzeAndSave(Dream dream) {
        if (dream == null || dream.getId() == null) return;
        String text = buildAnalysisText(dream);
        if (text.isBlank()) return;

        sentimentClient.analyze(text).ifPresent(data -> {
            dream.setAnalysisLabel(data.getLabel());
            dream.setAnalysisLabelName(data.getLabelName());
            dream.setAnalysisConfidence(data.getConfidence());
            dream.setAnalysisIntensity(data.getIntensity());
            dream.setAnalysisFeedback(data.getFeedback());
            dream.setAnalysisUpdatedAt(LocalDateTime.now());
            dreamMapper.updateAnalysis(dream);
        });
    }

    private String buildAnalysisText(Dream dream) {
        StringBuilder sb = new StringBuilder();
        if (dream.getTitle() != null && !dream.getTitle().isBlank()) {
            sb.append(dream.getTitle().trim());
        }
        if (dream.getContent() != null && !dream.getContent().isBlank()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(dream.getContent().trim());
        }
        return sb.toString().trim();
    }
}
