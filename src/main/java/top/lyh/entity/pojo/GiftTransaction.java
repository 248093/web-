package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gift_transaction")  // 指定数据库表名
public class GiftTransaction {   // 类名改为与业务相关

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)  // 自增主键
    private Long id;


    /**
     * 交易号
     */
    private String transactionNo;

    /**
     * 支付类型
     */
    @NotNull(message = "支付类型不能为空")
    private PaymentType paymentType;  // 使用枚举类型

    /**
     * 金额
     */
    // 最小1元钱
    @DecimalMin(value = "1.00", message = "金额必须大于等于1元")
    private BigDecimal amount;

    /**
     * 状态
     */
    @NotNull(message = "状态不能为空")
    private TransactionStatus status;  // 使用枚举类型

    /**
     * 用户ID
     */
    private Long userId;  // 添加用户ID字段

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)  // 插入时自动填充
    private LocalDateTime createdAt;

    // 支付宝支付相关扩展字段
    @TableField(exist = false)  // 非数据库字段
    private String traceNo;

    @TableField(exist = false)
    private String alipayTraceNo;

    @TableField(exist = false)
    private String subject;

    /**
     * 支付类型枚举
     */
    public enum PaymentType {
        coin, point, wechat, alipay
    }

    /**
     * 交易状态枚举
     */
    public enum TransactionStatus {
        pending, success, failed
    }
}