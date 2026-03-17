package com.dreamarchive.service.impl;

import com.dreamarchive.entity.DreamCategory;
import com.dreamarchive.mapper.DreamCategoryMapper;
import com.dreamarchive.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类Service实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private DreamCategoryMapper dreamCategoryMapper;

    @Override
    public List<DreamCategory> getAllCategories() {
        System.out.println("=== CategoryService: 获取所有分类 ===");
        List<DreamCategory> categories = dreamCategoryMapper.findAll();
        System.out.println("分类数量: " + (categories != null ? categories.size() : 0));
        return categories;
    }

    @Override
    public List<DreamCategory> getAllCategoriesWithCount() {
        System.out.println("=== CategoryService: 获取所有分类（含梦境数量）===");
        List<DreamCategory> categories = dreamCategoryMapper.findAllWithCount();
        System.out.println("分类数量: " + (categories != null ? categories.size() : 0));
        if (categories != null) {
            for (DreamCategory category : categories) {
                System.out.println("  - " + category.getName() + ": " + category.getDreamCount() + "个梦境");
            }
        }
        return categories;
    }

    @Override
    public DreamCategory getCategoryById(Integer id) {
        System.out.println("=== CategoryService: 获取分类 ID=" + id + " ===");
        DreamCategory category = dreamCategoryMapper.findById(id);
        System.out.println("找到分类: " + (category != null ? category.getName() : "null"));
        return category;
    }

    @Override
    public boolean createCategory(DreamCategory category) {
        System.out.println("=== CategoryService: 创建分类 ===");
        System.out.println("分类名称: " + category.getName());
        int rows = dreamCategoryMapper.insert(category);
        boolean success = rows > 0;
        System.out.println("创建结果: " + (success ? "成功" : "失败"));
        return success;
    }

    @Override
    public boolean updateCategory(DreamCategory category) {
        System.out.println("=== CategoryService: 更新分类 ID=" + category.getId() + " ===");
        int rows = dreamCategoryMapper.update(category);
        boolean success = rows > 0;
        System.out.println("更新结果: " + (success ? "成功" : "失败"));
        return success;
    }

    @Override
    public boolean deleteCategory(Integer id) {
        System.out.println("=== CategoryService: 删除分类 ID=" + id + " ===");
        int rows = dreamCategoryMapper.delete(id);
        boolean success = rows > 0;
        System.out.println("删除结果: " + (success ? "成功" : "失败"));
        return success;
    }
}