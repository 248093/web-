package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lyh.entity.pojo.Category;

import java.util.List;
import java.util.Map;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 获取所有一级分类（parent_id为null）
     */
    @Select("SELECT * FROM category WHERE parent_id IS NULL ORDER BY sort_order")
    List<Category> selectRootCategories();

    /**
     * 获取子分类
     */
    @Select("SELECT * FROM category WHERE parent_id = #{parentId} ORDER BY sort_order")
    List<Category> selectSubCategories(@Param("parentId") Long parentId);

    /**
     * 获取各分类下的直播间数量
     */
    @Select("SELECT c.id, COUNT(lr.id) as room_count " +
            "FROM category c " +
            "LEFT JOIN live_room lr ON c.id = lr.category_id AND lr.status = 1 " +
            "GROUP BY c.id")
    List<Map<String, Object>> selectCategoryRoomCount();
}