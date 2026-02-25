package top.lyh.entity.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 每日收入VO
 */
@Data
public class DailyIncomeVo {
    /**
     * 日期 (yyyy-MM-dd)
     */
    private String date;
    
    /**
     * 当日收入
     */
    private BigDecimal income;
}