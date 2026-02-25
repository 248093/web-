package top.lyh.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.lyh.entity.pojo.DailyVisitStat;
import top.lyh.service.DailyVisitStatService;
import top.lyh.utils.RedisUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;

/**
 * 访问统计同步任务
 */
@Slf4j
@Component
public class VisitStatSyncTask {

    @Autowired
    private DailyVisitStatService dailyVisitStatService;
    
    @Autowired
    private RedisUtil redisUtil;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String VISIT_COUNT_KEY_PREFIX = "visit:count:";
    private static final String VISIT_USERS_KEY_PREFIX = "visit:users:";
    private static final String NEW_USERS_KEY_PREFIX = "visit:new_users:";

    /**
     * 每天凌晨1点执行统计同步
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void syncYesterdayVisitStat() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String dateStr = yesterday.format(DATE_FORMATTER);
            
            // 从Redis获取昨日统计数据
            String visitCountKey = VISIT_COUNT_KEY_PREFIX + dateStr;
            String visitUsersKey = VISIT_USERS_KEY_PREFIX + dateStr;
            String newUsersKey = NEW_USERS_KEY_PREFIX + dateStr;
            
            Object visitCountObj = redisUtil.get(visitCountKey);
            Set<Object> visitUsersSet = redisUtil.sMembers(visitUsersKey);
            Set<Object> newUsersSet = redisUtil.sMembers(newUsersKey);
            
            int visitCount = visitCountObj != null ? Integer.parseInt(visitCountObj.toString()) : 0;
            int userCount = visitUsersSet != null ? visitUsersSet.size() : 0;
            int newUserCount = newUsersSet != null ? newUsersSet.size() : 0;
            
            // 保存到数据库
            DailyVisitStat stat = new DailyVisitStat();
            stat.setStatDate(dateStr);
            stat.setVisitCount(visitCount);
            stat.setUserCount(userCount);
            stat.setNewUserCount(newUserCount);
            stat.setCreateTime(new Date());
            stat.setUpdateTime(new Date());
            
            dailyVisitStatService.save(stat);
            
            log.info("同步昨日访问统计完成: 日期={}, 访问次数={}, 用户数={}, 新用户数={}", 
                dateStr, visitCount, userCount, newUserCount);
                
        } catch (Exception e) {
            log.error("同步访问统计失败: {}", e.getMessage(), e);
        }
    }
}
