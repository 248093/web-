package top.lyh.service;

import org.springframework.web.multipart.MultipartFile;
import top.lyh.entity.pojo.Gift;
import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.entity.pojo.GiftSendRecord;
import top.lyh.entity.vo.DailyIncomeVo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
* @author lyh
* @description 针对表【gift】的数据库操作Service
* @createDate 2026-02-12 20:38:30
*/
public interface GiftService extends IService<Gift> {
    // 增加修改礼物
    boolean saveOrEditGift(Gift gift, MultipartFile file);
    // 删除礼物
    boolean deleteGift(Long id);
    // 查询礼物
    List<Gift> queryGift(Long id);

    boolean sendGift(GiftSendRecord giftSendRecord) throws Exception;
    /**
     * 获取直播间指定时间范围内的总收入
     */
    BigDecimal getRoomIncomeByDateRange(Long liveRoomId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取直播间每日收入统计
     */
    List<DailyIncomeVo> getRoomDailyIncomeStats(Long liveRoomId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取系统指定时间范围内的总收入
     */
    BigDecimal getTotalIncomeByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取系统每日收入统计
     */
    List<DailyIncomeVo> getTotalDailyIncomeStats(LocalDateTime startDate, LocalDateTime endDate);
}
