package com.dreamarchive.mapper;

import com.dreamarchive.entity.Dream;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DreamMapper {

    String SELECT_BASE = "SELECT d.id, d.user_id, d.category_id, d.title, d.content, d.tags, d.images, " +
            "d.mood_score, d.is_public, d.view_count, d.like_count, " +
            "(SELECT COUNT(*) FROM comment cm WHERE cm.dream_id = d.id) AS comment_count, " +
            "d.status, d.dream_date, d.create_time, d.update_time, " +
            "d.analysis_label, d.analysis_label_name, d.analysis_confidence, d.analysis_intensity, d.analysis_feedback, d.analysis_updated_at, " +
            "u.username, u.avatar as user_avatar, c.name as category_name, c.color as category_color ";

    @Select(SELECT_BASE +
            "FROM dream d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN dream_category c ON d.category_id = c.id " +
            "WHERE d.id = #{id}")
    Dream findById(Long id);

    @Select(SELECT_BASE +
            "FROM dream d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN dream_category c ON d.category_id = c.id " +
            "WHERE d.user_id = #{userId} AND DATE(d.create_time) = CURDATE() AND d.status = 1 " +
            "ORDER BY d.create_time DESC LIMIT 1")
    Dream findTodayLatestByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            SELECT_BASE +
            "FROM dream d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN dream_category c ON d.category_id = c.id " +
            "WHERE d.is_public = 1 " +
            "<if test='categoryId != null'> AND d.category_id = #{categoryId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (d.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR d.content LIKE CONCAT('%', #{keyword}, '%') " +
            "OR d.tags LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY d.create_time DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<Dream> findPublicPage(@Param("categoryId") Integer categoryId,
                               @Param("keyword") String keyword,
                               @Param("offset") int offset,
                               @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM dream d WHERE d.is_public = 1 " +
            "<if test='categoryId != null'> AND d.category_id = #{categoryId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (d.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR d.content LIKE CONCAT('%', #{keyword}, '%') " +
            "OR d.tags LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    int countPublic(@Param("categoryId") Integer categoryId,
                    @Param("keyword") String keyword);

    @Select(SELECT_BASE +
            "FROM dream d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN dream_category c ON d.category_id = c.id " +
            "WHERE d.user_id = #{userId} " +
            "ORDER BY d.create_time DESC " +
            "LIMIT #{offset}, #{size}")
    List<Dream> findByUserId(@Param("userId") Long userId,
                             @Param("offset") int offset,
                             @Param("size") int size);

    @Select("SELECT COUNT(*) FROM dream WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    @Select(SELECT_BASE +
            "FROM dream d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN dream_category c ON d.category_id = c.id " +
            "WHERE d.is_public = 1 " +
            "ORDER BY d.like_count DESC, d.view_count DESC, d.create_time DESC " +
            "LIMIT #{limit}")
    List<Dream> findHot(int limit);

    @Insert("INSERT INTO dream (user_id, category_id, title, content, tags, images, mood_score, is_public, dream_date) " +
            "VALUES (#{userId}, #{categoryId}, #{title}, #{content}, #{tags}, #{images}, #{moodScore}, #{isPublic}, #{dreamDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Dream dream);

    @Update("UPDATE dream SET category_id = #{categoryId}, title = #{title}, " +
            "content = #{content}, tags = #{tags}, images = #{images}, mood_score = #{moodScore}, " +
            "is_public = #{isPublic}, dream_date = #{dreamDate} WHERE id = #{id}")
    int update(Dream dream);

    @Update("UPDATE dream SET analysis_label = #{analysisLabel}, analysis_label_name = #{analysisLabelName}, " +
            "analysis_confidence = #{analysisConfidence}, analysis_intensity = #{analysisIntensity}, " +
            "analysis_feedback = #{analysisFeedback}, analysis_updated_at = NOW() WHERE id = #{id}")
    int updateAnalysis(Dream dream);

    @Delete("DELETE FROM dream WHERE id = #{id}")
    int delete(Long id);    // 删除 dreams

    @Update("UPDATE dream SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);

    @Update("UPDATE dream SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(Long id);

    @Update("UPDATE dream SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrementLikeCount(Long id);

    @Update("UPDATE dream d SET d.comment_count = (SELECT COUNT(*) FROM comment c WHERE c.dream_id = d.id) WHERE d.id = #{dreamId}")
    int refreshCommentCount(Long dreamId);

    @Select(SELECT_BASE +
            "FROM dream d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN dream_category c ON d.category_id = c.id " +
            "WHERE d.is_public = 1 ORDER BY d.create_time DESC")
    List<Dream> findPublicDreams();

    @Select("SELECT COUNT(*) FROM dream")
    int countAllDreams();

    @Select("SELECT COUNT(*) FROM dream WHERE DATE(create_time) = CURDATE()")
    int countTodayDreams();

    @Select("SELECT COUNT(*) FROM dream WHERE is_public = 1")
    int countPublicDreams();

    @Select("SELECT COUNT(*) FROM dream WHERE is_public = 0")
    int countPrivateDreams();

    @Select(SELECT_BASE +
            "FROM dream d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN dream_category c ON d.category_id = c.id " +
            "WHERE d.category_id = #{categoryId} AND d.is_public = 1 " +
            "ORDER BY d.create_time DESC")
    List<Dream> findByCategoryId(Long categoryId);

    @Select("SELECT id FROM dream WHERE status = 1 AND (analysis_label IS NULL OR analysis_label_name IS NULL) " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<Long> findIdsForAnalysis(@Param("limit") int limit);

    @Select(SELECT_BASE +
            "FROM dream d " +
            "LEFT JOIN user u ON d.user_id = u.id " +
            "LEFT JOIN dream_category c ON d.category_id = c.id " +
            "ORDER BY d.create_time DESC LIMIT #{offset}, #{size}")
    List<Dream> findAllForAdmin(@Param("offset") int offset, @Param("size") int size);
}
