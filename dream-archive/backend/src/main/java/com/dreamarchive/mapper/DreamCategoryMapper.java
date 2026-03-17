package com.dreamarchive.mapper;

import com.dreamarchive.entity.DreamCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 梦境分类Mapper
 */
@Mapper
public interface DreamCategoryMapper {
    
    /**
     * 查询所有分类
     */
    @Select("SELECT * FROM dream_category WHERE status = 1 ORDER BY sort_order")
    List<DreamCategory> findAll();
    
    /**
     * 根据ID查询分类
     */
    @Select("SELECT * FROM dream_category WHERE id = #{id}")
    DreamCategory findById(Integer id);
    
    /**
     * 插入分类
     */
    @Insert("INSERT INTO dream_category(name, description, icon, color, sort_order) " +
            "VALUES(#{name}, #{description}, #{icon}, #{color}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DreamCategory category);
    
    /**
     * 更新分类
     */
    @Update("UPDATE dream_category SET name=#{name}, description=#{description}, " +
            "icon=#{icon}, color=#{color}, sort_order=#{sortOrder} WHERE id=#{id}")
    int update(DreamCategory category);
    
    /**
     * 删除分类（逻辑删除）
     */
    @Update("UPDATE dream_category SET status=0 WHERE id=#{id}")
    int delete(Integer id);
    
    /**
     * 查询分类及梦境数量
     */
    @Select("SELECT dc.*, COUNT(d.id) as dream_count " +
            "FROM dream_category dc " +
            "LEFT JOIN dream d ON dc.id = d.category_id AND d.status = 1 AND d.is_public = 1 " +
            "WHERE dc.status = 1 " +
            "GROUP BY dc.id " +
            "ORDER BY dc.sort_order")
    List<DreamCategory> findAllWithCount();
}
