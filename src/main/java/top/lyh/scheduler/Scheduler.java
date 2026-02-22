package top.lyh.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.lyh.mapper.ContributionRankMapper;

import java.time.LocalDate;

@Component
public class Scheduler {

    @Autowired
    private ContributionRankMapper contributionMapper;

    /**
     * 每天凌晨2点执行，删除7天前的贡献记录
     * cron = 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 1 ? * MON")
    public void cleanupExpiredContributions() {
        int deletedCount = contributionMapper.deleteExpiredContributions();
        System.out.println("已删除 " + deletedCount + " 条7天前的贡献记录");
    }

}