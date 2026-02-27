package top.lyh.entity.dto;

import lombok.Data;
import top.lyh.entity.pojo.GiftTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GiftTransactionQueryDto {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 交易状态
     */
    private GiftTransaction.TransactionStatus status;
    
    /**
     * 支付类型
     */
    private GiftTransaction.PaymentType paymentType;

    
    /**
     * 交易号（精确查询）
     */
    private String transactionNo;
    
    /**
     * 创建时间开始
     */
    private LocalDateTime createdAtStart;
    
    /**
     * 创建时间结束
     */
    private LocalDateTime createdAtEnd;

    
    /**
     * 页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 是否启用动态查询
     */
    private Boolean enableDynamicQuery = true;
}
