package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.lyh.entity.pojo.Category;
import top.lyh.entity.vo.CategoryCountVO;
import top.lyh.entity.vo.CategoryVO;
import top.lyh.mapper.CategoryMapper;
import top.lyh.mapper.LiveRoomMapper;
import top.lyh.service.CategoryService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private LiveRoomMapper liveRoomMapper;

    @Override
    public List<CategoryVO> getCategoryTree() {
        // 查询所有分类
        List<Category> allCategories = list();
        
        // 转换为VO并构建树形结构
        Map<Long, CategoryVO> voMap = new HashMap<>();
        List<CategoryVO> rootList = new ArrayList<>();
        
        // 先转换为VO并放入Map
        for (Category category : allCategories) {
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(category, vo);
            vo.setChildren(new ArrayList<>());
            voMap.put(category.getId(), vo);
            
            if (category.getParentId() == null) {
                rootList.add(vo);
            }
        }
        
        // 构建父子关系
        for (Category category : allCategories) {
            if (category.getParentId() != null) {
                CategoryVO parent = voMap.get(category.getParentId());
                if (parent != null) {
                    parent.getChildren().add(voMap.get(category.getId()));
                }
            }
        }
        
        // 对每个节点的子列表进行排序
        rootList.forEach(this::sortChildren);
        
        // 获取分类统计并填充
        List<CategoryCountVO> counts = liveRoomMapper.selectCategoryRoomCount();
        Map<Long, Integer> countMap = counts.stream()
                .collect(Collectors.toMap(CategoryCountVO::getId, CategoryCountVO::getCount));
        
        // 填充数量
        voMap.values().forEach(vo -> vo.setRoomCount(countMap.getOrDefault(vo.getId(), 0)));
        
        return rootList;
    }

    private void sortChildren(CategoryVO vo) {
        if (vo.getChildren() != null && !vo.getChildren().isEmpty()) {
            vo.getChildren().sort(Comparator.comparing(CategoryVO::getSortOrder));
            vo.getChildren().forEach(this::sortChildren);
        }
    }

    @Override
    public List<CategoryVO> getRootCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Category::getParentId)
               .orderByAsc(Category::getSortOrder);
        
        List<Category> categories = list(wrapper);
        List<CategoryVO> vos = categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 填充数量
        List<CategoryCountVO> counts = liveRoomMapper.selectCategoryRoomCount();
        Map<Long, Integer> countMap = counts.stream()
                .collect(Collectors.toMap(CategoryCountVO::getId, CategoryCountVO::getCount));
        
        vos.forEach(vo -> vo.setRoomCount(countMap.getOrDefault(vo.getId(), 0)));
        
        return vos;
    }

    @Override
    public List<CategoryVO> getSubCategories(Long parentId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getParentId, parentId)
               .orderByAsc(Category::getSortOrder);
        
        List<Category> categories = list(wrapper);
        List<CategoryVO> vos = categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 填充数量
        List<CategoryCountVO> counts = liveRoomMapper.selectCategoryRoomCount();
        Map<Long, Integer> countMap = counts.stream()
                .collect(Collectors.toMap(CategoryCountVO::getId, CategoryCountVO::getCount));
        
        vos.forEach(vo -> vo.setRoomCount(countMap.getOrDefault(vo.getId(), 0)));
        
        return vos;
    }

    @Override
    public Map<Long, Integer> getCategoryRoomCount() {
        List<CategoryCountVO> list = liveRoomMapper.selectCategoryRoomCount();
        Map<Long, Integer> result = new HashMap<>();
        
        for (CategoryCountVO vo : list) {
            result.put(vo.getId(), vo.getCount());
        }
        
        return result;
    }

    @Override
    public List<CategoryCountVO> getCategoryCountList() {
        return liveRoomMapper.selectCategoryRoomCount();
    }

    private CategoryVO convertToVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}