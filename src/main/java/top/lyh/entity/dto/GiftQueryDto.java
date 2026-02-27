package top.lyh.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GiftQueryDto {
    private Long id;

    /**
     * 礼物名称
     */
    private String name;

    /**
     * 礼物图标
     */
    private String imageUrl;

    private Integer page;

    private Integer pageSize;

}