package top.lyh.entity.vo;

import lombok.Data;

@Data
public class CategoryCountVO {
    /**
     * 分类ID
     */
    private Long id;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 直播间数量
     */
    private Integer count;
}