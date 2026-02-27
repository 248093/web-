package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.CategoryQueryDto;
import top.lyh.entity.pojo.Category;
import top.lyh.entity.vo.CategoryVO;
import top.lyh.entity.vo.CategoryCountVO;

import java.util.List;
import java.util.Map;

public interface CategoryService extends IService<Category> {

    /**
     * 获取所有分类（树形结构）
     */
    List<CategoryVO> getCategoryTree();

    /**
     * 动态分页查询分类列表
     */
    PageResult<Category> getCategoryByPage(CategoryQueryDto queryDto);
    /**
     * 获取所有一级分类
     */
    List<CategoryVO> getRootCategories();

    /**
     * 获取子分类
     */
    List<CategoryVO> getSubCategories(Long parentId);

    /**
     * 获取各分类下的直播间数量
     */
    Map<Long, Integer> getCategoryRoomCount();

    /**
     * 获取分类统计列表（包含ID、名称、数量）
     */
    List<CategoryCountVO> getCategoryCountList();
}