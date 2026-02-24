package top.lyh.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    
    /**
     * 子分类列表（用于树形结构）
     */
    private List<CategoryVO> children;
    
    /**
     * 直播间数量（前端展示用）
     */
    private Integer roomCount;
}