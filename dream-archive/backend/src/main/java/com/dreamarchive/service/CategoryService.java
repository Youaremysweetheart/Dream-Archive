package com.dreamarchive.service;

import com.dreamarchive.entity.DreamCategory;
import java.util.List;

/**
 * 分类Service接口
 */
public interface CategoryService {

    /**
     * 获取所有启用的分类
     * @return 分类列表
     */
    List<DreamCategory> getAllCategories();

    /**
     * 获取所有分类及其梦境数量
     * @return 分类列表（包含梦境数量）
     */
    List<DreamCategory> getAllCategoriesWithCount();

    /**
     * 根据ID获取分类
     * @param id 分类ID
     * @return 分类对象
     */
    DreamCategory getCategoryById(Integer id);

    /**
     * 创建分类
     * @param category 分类对象
     * @return 是否成功
     */
    boolean createCategory(DreamCategory category);

    /**
     * 更新分类
     * @param category 分类对象
     * @return 是否成功
     */
    boolean updateCategory(DreamCategory category);

    /**
     * 删除分类（逻辑删除）
     * @param id 分类ID
     * @return 是否成功
     */
    boolean deleteCategory(Integer id);
}