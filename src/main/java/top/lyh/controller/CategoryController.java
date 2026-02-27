package top.lyh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.lyh.anno.LogAnnotation;
import top.lyh.common.PageResult;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.CategoryQueryDto;
import top.lyh.entity.pojo.Category;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.entity.vo.CategoryCountVO;
import top.lyh.entity.vo.CategoryVO;
import top.lyh.service.CategoryService;
import top.lyh.service.LiveRoomService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private LiveRoomService liveRoomService; // 需要检查直播间是否使用该分类

    /**
     * 获取所有分类（树形结构）
     */
    @GetMapping("/tree")
    @LogAnnotation(value = "获取所有分类", recordParams = false, recordResult = true)
    public ResultDTO getCategoryTree() {
        List<CategoryVO> tree = categoryService.getCategoryTree();
        return ResultDTO.success(tree);
    }
    /**
     * 分页查询分类列表
     */
    @PostMapping("/page")
    @LogAnnotation(value = "分页查询分类列表", recordParams = true, recordResult = true)
    public ResultDTO getCategoryByPage(@RequestBody CategoryQueryDto queryDto) {
        try {
            PageResult<Category> result = categoryService.getCategoryByPage(queryDto);
            return ResultDTO.success("查询成功", result);
        } catch (Exception e) {
            return ResultDTO.error("查询失败：" + e.getMessage());
        }
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
    @RequiresRoles("ADMIN")
    @LogAnnotation(value = "添加分类", recordParams = true, recordResult = true)
    public ResultDTO addCategory(@RequestBody @Valid CategoryVO categoryVO) {
        try {
            // 参数校验
            if (categoryVO.getName() == null || categoryVO.getName().trim().isEmpty()) {
                return ResultDTO.error("分类名称不能为空");
            }

            // 检查同级分类名称是否重复
            LambdaQueryWrapper<Category> nameCheckWrapper = new LambdaQueryWrapper<>();
            nameCheckWrapper.eq(Category::getName, categoryVO.getName().trim())
                    .eq(Category::getParentId, categoryVO.getParentId() != null ? categoryVO.getParentId() : 0L);

            if (categoryService.count(nameCheckWrapper) > 0) {
                return ResultDTO.error("同级分类下已存在相同名称的分类");
            }

            // ✅ 新增：检查同级分类下排序权重是否重复
            if (categoryVO.getSortOrder() != null) {
                LambdaQueryWrapper<Category> sortCheckWrapper = new LambdaQueryWrapper<>();
                sortCheckWrapper.eq(Category::getSortOrder, categoryVO.getSortOrder())
                        .eq(Category::getParentId, categoryVO.getParentId() != null ? categoryVO.getParentId() : 0L);

                if (categoryService.count(sortCheckWrapper) > 0) {
                    return ResultDTO.error("同级分类下已存在相同的排序权重，请修改");
                }
            }

            // 构建分类实体
            Category category = new Category();
            category.setName(categoryVO.getName().trim());
            category.setParentId(categoryVO.getParentId() != null ? categoryVO.getParentId() : 0L);
            category.setSortOrder(categoryVO.getSortOrder() != null ? categoryVO.getSortOrder() : 0);
            category.setCreatedAt(LocalDateTime.now());

            // 保存分类
            boolean result = categoryService.save(category);

            if (result) {
                log.info("添加分类成功，分类ID: {}, 名称: {}", category.getId(), category.getName());
                return ResultDTO.success("添加成功", category);
            } else {
                return ResultDTO.error("添加失败");
            }

        } catch (Exception e) {
            log.error("添加分类异常，参数: {}", categoryVO, e);
            return ResultDTO.error("添加分类失败: " + e.getMessage());
        }
    }
    /**
     * 更新分类（管理员接口）
     */
    @PutMapping("/update/{id}")
    @RequiresRoles("ADMIN")
    @LogAnnotation(value = "更新分类", recordParams = true, recordResult = true)
    public ResultDTO updateCategory(@PathVariable Long id, @RequestBody @Valid CategoryVO categoryVO) {
        try {
            // 检查分类是否存在
            Category existingCategory = categoryService.getById(id);
            if (existingCategory == null) {
                return ResultDTO.error("分类不存在");
            }

            // 参数校验
            if (categoryVO.getName() == null || categoryVO.getName().trim().isEmpty()) {
                return ResultDTO.error("分类名称不能为空");
            }

            // 检查同级分类名称是否重复（排除自己）
            LambdaQueryWrapper<Category> nameCheckWrapper = new LambdaQueryWrapper<>();
            nameCheckWrapper.eq(Category::getName, categoryVO.getName().trim())
                    .eq(Category::getParentId, categoryVO.getParentId() != null ? categoryVO.getParentId() : 0L)
                    .ne(Category::getId, id);

            if (categoryService.count(nameCheckWrapper) > 0) {
                return ResultDTO.error("同级分类下已存在相同名称的分类");
            }

            // ✅ 新增：检查同级分类下排序权重是否重复（排除自己）
            if (categoryVO.getSortOrder() != null) {
                LambdaQueryWrapper<Category> sortCheckWrapper = new LambdaQueryWrapper<>();
                sortCheckWrapper.eq(Category::getSortOrder, categoryVO.getSortOrder())
                        .eq(Category::getParentId, categoryVO.getParentId() != null ? categoryVO.getParentId() : 0L)
                        .ne(Category::getId, id);

                if (categoryService.count(sortCheckWrapper) > 0) {
                    return ResultDTO.error("同级分类下已存在相同的排序权重，请修改");
                }
            }

            // 构建更新实体
            Category category = new Category();
            category.setId(id);
            category.setName(categoryVO.getName().trim());
            category.setParentId(categoryVO.getParentId() != null ? categoryVO.getParentId() : 0L);
            category.setSortOrder(categoryVO.getSortOrder() != null ? categoryVO.getSortOrder() : existingCategory.getSortOrder());
            category.setCreatedAt(LocalDateTime.now());

            // 不能将分类设置为自己或自己的子分类的父分类
            if (category.getParentId() != null && category.getParentId().equals(id)) {
                return ResultDTO.error("不能将分类设置为自己的父分类");
            }

            // 检查是否存在循环引用
            if (hasCircularReference(id, category.getParentId())) {
                return ResultDTO.error("检测到分类层级循环引用");
            }

            // 更新分类
            boolean result = categoryService.updateById(category);

            if (result) {
                log.info("更新分类成功，分类ID: {}, 名称: {}", id, category.getName());
                return ResultDTO.success("更新成功", category);
            } else {
                return ResultDTO.error("更新失败");
            }

        } catch (Exception e) {
            log.error("更新分类异常，ID: {}, 参数: {}", id, categoryVO, e);
            return ResultDTO.error("更新分类失败: " + e.getMessage());
        }
    }
    /**
     * 删除分类（管理员接口）
     */
    @DeleteMapping("/delete/{id}")
    @RequiresRoles("ADMIN")
    @LogAnnotation(value = "删除分类", recordParams = true, recordResult = true)
    public ResultDTO deleteCategory(@PathVariable Long id) {
        try {
            // 检查分类是否存在
            Category category = categoryService.getById(id);
            if (category == null) {
                return ResultDTO.error("分类不存在");
            }

            // 检查是否有子分类
            LambdaQueryWrapper<Category> childWrapper = new LambdaQueryWrapper<>();
            childWrapper.eq(Category::getParentId, id);
            if (categoryService.count(childWrapper) > 0) {
                return ResultDTO.error("该分类下存在子分类，不能直接删除");
            }

            // 检查是否有直播间使用该分类
            LambdaQueryWrapper<LiveRoom> roomWrapper = new LambdaQueryWrapper<>();
            roomWrapper.eq(LiveRoom::getCategoryId, id);
            if (liveRoomService.count(roomWrapper) > 0) {
                return ResultDTO.error("该分类已被直播间使用，不能删除");
            }

            // 执行删除
            boolean result = categoryService.removeById(id);

            if (result) {
                log.info("删除分类成功，分类ID: {}, 名称: {}", id, category.getName());
                return ResultDTO.success("删除成功");
            } else {
                return ResultDTO.error("删除失败");
            }

        } catch (Exception e) {
            log.error("删除分类异常，ID: {}", id, e);
            return ResultDTO.error("删除分类失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否存在循环引用
     */
    private boolean hasCircularReference(Long categoryId, Long newParentId) {
        if (newParentId == null || newParentId == 0) {
            return false;
        }

        // 获取新的父分类
        Category parentCategory = categoryService.getById(newParentId);
        if (parentCategory == null) {
            return false;
        }

        // 检查父分类的父分类是否是当前分类
        Long currentParentId = parentCategory.getParentId();
        while (currentParentId != null && currentParentId != 0) {
            if (currentParentId.equals(categoryId)) {
                return true; // 发现循环引用
            }
            Category nextParent = categoryService.getById(currentParentId);
            if (nextParent == null) {
                break;
            }
            currentParentId = nextParent.getParentId();
        }

        return false;
    }
}