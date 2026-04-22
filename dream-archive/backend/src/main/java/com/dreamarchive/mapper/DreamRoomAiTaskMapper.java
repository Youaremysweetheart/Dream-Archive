package com.dreamarchive.mapper;

import com.dreamarchive.entity.DreamRoomAiTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DreamRoomAiTaskMapper {

    @Insert("INSERT INTO dream_room_ai_task (task_id, dream_room_id, user_id, dream_post_id, dream_post_content, " +
            "dream_room_status, question, answer, is_violation, task_type, task_status, retry_count, last_error) " +
            "VALUES (#{taskId}, #{dreamRoomId}, #{userId}, #{dreamPostId}, #{dreamPostContent}, #{dreamRoomStatus}, " +
            "#{question}, #{answer}, #{isViolation}, #{taskType}, #{taskStatus}, #{retryCount}, #{lastError})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DreamRoomAiTask task);

    @Select("SELECT id, task_id, dream_room_id, user_id, dream_post_id, dream_post_content, dream_room_status, " +
            "question, answer, is_violation, task_type, task_status, retry_count, last_error, create_time, update_time " +
            "FROM dream_room_ai_task WHERE task_status = 0 ORDER BY id ASC LIMIT #{limit}")
    List<DreamRoomAiTask> findPending(@Param("limit") int limit);

    @Update("UPDATE dream_room_ai_task SET task_status = 1, update_time = NOW() WHERE id = #{id} AND task_status = 0")
    int markProcessing(@Param("id") Long id);

    @Update("UPDATE dream_room_ai_task SET task_status = 2, answer = #{answer}, is_violation = #{isViolation}, " +
            "update_time = NOW() WHERE id = #{id}")
    int markSuccess(@Param("id") Long id,
                    @Param("answer") String answer,
                    @Param("isViolation") Integer isViolation);

    @Update("UPDATE dream_room_ai_task SET task_status = #{taskStatus}, retry_count = #{retryCount}, " +
            "last_error = #{lastError}, update_time = NOW() WHERE id = #{id}")
    int updateRetry(@Param("id") Long id,
                    @Param("taskStatus") Integer taskStatus,
                    @Param("retryCount") Integer retryCount,
                    @Param("lastError") String lastError);

    @Select("SELECT COUNT(*) FROM dream_room_ai_task WHERE dream_room_id = #{dreamRoomId} AND task_type = #{taskType} " +
            "AND task_status IN (0, 1)")
    int countActiveByRoomAndType(@Param("dreamRoomId") String dreamRoomId, @Param("taskType") Integer taskType);
}
