package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 
 * @TableName contribution_rank
 */
@TableName(value ="contribution_rank")
@Data
public class ContributionRank {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    private Long liveRoomId;

    /**
     * 
     */
    private Long senderId;

    /**
     * 累计贡献金额
     */
    private BigDecimal totalAmount;


    /**
     * 
     */
    private Date createdAt;
}