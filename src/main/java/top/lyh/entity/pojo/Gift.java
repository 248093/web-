package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 
 * @TableName gift
 */
@TableName(value ="gift")
@Data
public class Gift {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 礼物名称
     */
    @NotBlank
    private String name;

    /**
     * 礼物价格
     */
    @NotNull
    private BigDecimal price;

    /**
     * 礼物图标
     */
    private String imageUrl;

    /**
     * 
     */
    private Date createdAt;
}