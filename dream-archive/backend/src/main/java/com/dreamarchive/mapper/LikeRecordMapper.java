package com.dreamarchive.mapper;

import org.apache.ibatis.annotations.*;

/**
 * 点赞记录Mapper
 */
@Mapper
public interface LikeRecordMapper {
    
    /**
     * 添加点赞记录
     */
    @Insert("INSERT INTO like_record(user_id, target_id, target_type) " +
            "VALUES(#{userId}, #{targetId}, #{targetType})")
    int insert(@Param("userId") Long userId,
               @Param("targetId") Long targetId,
               @Param("targetType") Integer targetType);
    
    /**
     * 删除点赞记录
     */
    @Delete("DELETE FROM like_record WHERE user_id=#{userId} " +
            "AND target_id=#{targetId} AND target_type=#{targetType}")
    int delete(@Param("userId") Long userId,
               @Param("targetId") Long targetId,
               @Param("targetType") Integer targetType);
    
    /**
     * 检查是否已点赞
     */
    @Select("SELECT COUNT(*) > 0 FROM like_record " +
            "WHERE user_id=#{userId} AND target_id=#{targetId} AND target_type=#{targetType}")
    boolean exists(@Param("userId") Long userId,
                   @Param("targetId") Long targetId,
                   @Param("targetType") Integer targetType);
}
