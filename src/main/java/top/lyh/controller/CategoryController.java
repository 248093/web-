package top.lyh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.ResultDTO;
import top.lyh.entity.vo.CategoryCountVO;
import top.lyh.entity.vo.CategoryVO;
import top.lyh.service.CategoryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取所有分类（树形结构）
     */
    @GetMapping("/tree")
    public ResultDTO getCategoryTree() {
        List<CategoryVO> tree = categoryService.getCategoryTree();
        return ResultDTO.success(tree);
    }

    /**
     * 获取所有一级分类
     */
    @GetMapping("/root")
    public ResultDTO getRootCategories() {
        List<CategoryVO> list = categoryService.getRootCategories();
        return ResultDTO.success(list);
    }

    /**
     * 获取子分类
     */
    @GetMapping("/sub/{parentId}")
    public ResultDTO getSubCategories(@PathVariable Long parentId) {
        List<CategoryVO> list = categoryService.getSubCategories(parentId);
        return ResultDTO.success(list);
    }

    /**
     * 获取各分类下的直播间数量（Map格式）
     */
    @GetMapping("/room-count")
    public ResultDTO getCategoryRoomCount() {
        Map<Long, Integer> countMap = categoryService.getCategoryRoomCount();
        return ResultDTO.success(countMap);
    }

    /**
     * 获取分类统计列表（包含ID、名称、数量）
     * 前端最需要的接口
     */
    @GetMapping("/count-list")
    public ResultDTO getCategoryCountList() {
        List<CategoryCountVO> list = categoryService.getCategoryCountList();
        return ResultDTO.success(list);
    }

    /**
     * 添加分类（管理员接口）
     */
    @PostMapping("/add")
    public ResultDTO addCategory(@RequestBody CategoryVO categoryVO) {
        // 这里需要实现添加逻辑
        return ResultDTO.success();
    }

    /**
     * 更新分类（管理员接口）
     */
    @PutMapping("/update/{id}")
    public ResultDTO updateCategory(@PathVariable Long id, @RequestBody CategoryVO categoryVO) {
        // 这里需要实现更新逻辑
        return ResultDTO.success();
    }

    /**
     * 删除分类（管理员接口）
     */
    @DeleteMapping("/delete/{id}")
    public ResultDTO deleteCategory(@PathVariable Long id) {
        // 这里需要实现删除逻辑
        return ResultDTO.success();
    }
}