package com.dreamarchive.controller;

import com.dreamarchive.common.Result;
import com.dreamarchive.entity.DreamCategory;
import com.dreamarchive.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/category")
@CrossOrigin
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取所有分类
     * GET /api/category/list
     */
    @GetMapping("/list")
    public Result<List<DreamCategory>> getAllCategories() {
        try {
            System.out.println("=== 获取分类列表 ===");
            List<DreamCategory> categories = categoryService.getAllCategories();
            System.out.println("返回分类数量: " + categories.size());
            return Result.success(categories);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(" 获取分类失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有分类（包含梦境数量）
     * GET /api/category/list/count
     */
    @GetMapping("/list/count")
    public Result<List<DreamCategory>> getAllCategoriesWithCount() {
        try {
            System.out.println("=== 获取分类列表（含数量）===");
            List<DreamCategory> categories = categoryService.getAllCategoriesWithCount();
            System.out.println("返回分类数量: " + categories.size());
            return Result.success(categories);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取分类失败: " + e.getMessage());
        }
    }

    /**
     * 根据 ID 获取分类
     * GET /api/category/{id}
     */
    @GetMapping("/{id}")
    public Result<DreamCategory> getCategoryById(@PathVariable Integer id) {
        try {
            System.out.println("=== 获取分类详情 ID=" + id + " ===");
            DreamCategory category = categoryService.getCategoryById(id);
            if (category == null) {
                return Result.error("未找到分类");
            }
            return Result.success(category);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取分类失败: " + e.getMessage());
        }
    }

    /**
     * 创建分类
     * POST /api/category/create
     */
    @PostMapping("/create")
    public Result<Void> createCategory(@RequestBody DreamCategory category) {
        try {
            System.out.println("===  创建分类 ===");
            System.out.println("分类名称: " + category.getName());
            boolean success = categoryService.createCategory(category);
            return success ? Result.success("创建成功，", null) : Result.error("创建失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类
     * PUT /api/category/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Integer id, @RequestBody DreamCategory category) {
        try {
            System.out.println("=== 更新分类 ID=" + id + " ===");
            category.setId(id);
            boolean success = categoryService.updateCategory(category);
            return success ? Result.success("更新成功", null) : Result.error("更新失败 ");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     *  删除分类（逻辑删除）
     * DELETE /api/category/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Integer id) {
        try {
            System.out.println("=== 删除分类 ID=" + id + " ===");
            boolean success = categoryService.deleteCategory(id);
            return success ? Result.success(" 删除成功,", null) : Result.error(" 删除失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
