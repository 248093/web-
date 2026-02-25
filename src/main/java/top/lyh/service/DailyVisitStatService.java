package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.entity.pojo.DailyVisitStat;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日访问统计Service接口
 */
public interface DailyVisitStatService extends IService<DailyVisitStat> {
    
    /**
     * 更新今日访问统计
     * @param userId 用户ID（可为空，表示匿名访问）
     */
    void updateTodayVisitStat(Long userId);
    
    /**
     * 获取指定日期范围的访问统计数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计数据列表
     */
    List<DailyVisitStat> getVisitStatsByDateRange(LocalDate startDate, LocalDate endDate);

}
