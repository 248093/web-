package top.lyh.entity.dto;

import lombok.Data;

@Data
public class CategoryQueryDto {
    
    /**
     * 分类名称（模糊查询）
     */
    private String name;
    
    /**
     * 父分类ID
     */
    private Long parentId;

    
    /**
     * 页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
}
