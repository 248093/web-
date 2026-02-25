package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.lyh.entity.pojo.DailyVisitStat;
import top.lyh.mapper.DailyVisitStatMapper;
import top.lyh.service.DailyVisitStatService;
import top.lyh.service.SysUserService;
import top.lyh.utils.RedisUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 每日访问统计Service实现类
 */
@Slf4j
@Service
public class DailyVisitStatServiceImpl extends ServiceImpl<DailyVisitStatMapper, DailyVisitStat> 
    implements DailyVisitStatService {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private SysUserService sysUserService; // 需要注入用户服务
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String VISIT_COUNT_KEY_PREFIX = "visit:count:";
    private static final String VISIT_USERS_KEY_PREFIX = "visit:users:";
    private static final String NEW_USERS_KEY_PREFIX = "visit:new_users:";

    @Override
    public void updateTodayVisitStat(Long userId) {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            String visitCountKey = VISIT_COUNT_KEY_PREFIX + today;
            String visitUsersKey = VISIT_USERS_KEY_PREFIX + today;
            String newUsersKey = NEW_USERS_KEY_PREFIX + today;
            
            // 增加访问次数
            redisUtil.incr( visitCountKey,1);
            redisUtil.expire(visitCountKey, 1500); // 过期时间为第二天凌晨
            
            // 记录访问用户    （去重）
            if (userId != null) {
                redisUtil.sAdd(visitUsersKey, userId.toString());
                redisUtil.expire(visitUsersKey, 1500);
                // 判断是否为新用户（首次访问）
                String userFirstVisitKey = "user:first_visit:" + userId;
                Boolean isFirstVisit = redisUtil.set(userFirstVisitKey, "1",18000);
                if (Boolean.TRUE.equals(isFirstVisit)) {
                    // 是新用户，记录到新增用户统计中
                    redisUtil.sAdd(newUsersKey, userId.toString());
                    redisUtil.expire(newUsersKey, 1500);
                    redisUtil.expire(userFirstVisitKey, 18000); // 保留30天标识
                    log.debug("记录新用户访问: 用户ID={}", userId);
                }
            }
            
            log.debug("更新今日访问统计: 日期={}, 用户ID={}", today, userId);
        } catch (Exception e) {
            log.error("更新访问统计失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<DailyVisitStat> getVisitStatsByDateRange(LocalDate startDate, LocalDate endDate) {
        // 如果没有传入时间参数，默认查询当天数据
        if (startDate == null && endDate == null) {
            startDate = LocalDate.now().atStartOfDay().toLocalDate();
            endDate = LocalDate.now();
        } else if (startDate == null) {
            startDate = endDate; // 如果只有结束时间，查询那一天的数据
        } else if (endDate == null) {
            endDate = startDate; // 如果只有开始时间，查询那一天的数据
        }

        LambdaQueryWrapper<DailyVisitStat> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(DailyVisitStat::getStatDate,
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER));
        queryWrapper.orderByAsc(DailyVisitStat::getStatDate);
        return this.list(queryWrapper);
    }


}
