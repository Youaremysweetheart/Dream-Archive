package com.dreamarchive.mapper;

import com.dreamarchive.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("SELECT c.*, u.username, u.avatar as user_avatar " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.id = #{id}")
    Comment findById(Long id);

    @Select("SELECT c.*, u.username, u.avatar as user_avatar " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.dream_id = #{dreamId} " +
            "ORDER BY c.create_time DESC")
    List<Comment> findByDreamId(Long dreamId);

    @Select("SELECT c.*, u.username, u.avatar as user_avatar, d.title as dream_title " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "LEFT JOIN dream d ON c.dream_id = d.id " +
            "WHERE c.user_id = #{userId} " +
            "ORDER BY c.create_time DESC")
    List<Comment> findByUserId(Long userId);

    @Insert("INSERT INTO comment (dream_id, user_id, parent_id, content) VALUES (#{dreamId}, #{userId}, #{parentId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    @Delete("DELETE FROM comment WHERE id = #{id}")
    int delete(Long id);

    @Delete("DELETE FROM comment WHERE parent_id = #{parentId}")
    int deleteByParentId(Long parentId);

    @Select("SELECT COUNT(*) FROM comment WHERE dream_id = #{dreamId}")
    int countByDreamId(Long dreamId);

    @Select("SELECT COUNT(*) FROM comment WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM comment")
    int countAllComments();

    @Select("SELECT COUNT(*) FROM comment WHERE DATE(create_time) = CURDATE()")
    int countTodayComments();
}
