package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sensitive_word")
public class SensitiveWord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 敏感词内容
     */
    private String word;
    
    /**
     * 敏感词类型：1-政治敏感 2-色情低俗 3-暴力恐怖 4-其他
     */
    private Integer type;
    
    /**
     * 替换字符，默认为***
     */
    private String replaceChar;
    
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
