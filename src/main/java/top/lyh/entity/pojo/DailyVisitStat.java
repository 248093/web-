package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 每日访问统计实体类
 */
@Data
@TableName("daily_visit_stat")
public class DailyVisitStat {

    /**
     * 统计ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期（格式：yyyy-MM-dd）
     */
    private String statDate;

    /**
     * 访问用户数（去重后的独立用户数）
     */
    private Integer userCount;

    /**
     * 总访问次数
     */
    private Integer visitCount;

    /**
     * 新增用户数
     */
    private Integer newUserCount;

    /**
     * 统计创建时间
     */
    private Date createTime;

    /**
     * 统计更新时间
     */
    private Date updateTime;
}
