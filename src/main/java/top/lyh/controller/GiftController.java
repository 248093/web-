package top.lyh.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.lyh.common.PageResult;
import top.lyh.common.ResultDTO;
import top.lyh.entity.pojo.Gift;
import top.lyh.entity.pojo.GiftSendRecord;
import top.lyh.entity.pojo.GiftTransaction;
import top.lyh.entity.vo.ContributionRankVo;
import top.lyh.entity.vo.DailyIncomeVo;
import top.lyh.service.ContributionRankService;
import top.lyh.service.GiftService;
import top.lyh.service.GiftTransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/gift")
public class GiftController {
    @Autowired
    private GiftService giftService;
    @Autowired
    private ContributionRankService contributionRankService;
    @Autowired
    private GiftTransactionService giftTransactionService;
    @GetMapping("/list")
    public ResultDTO gitfList(@Validated Long id) {
        List<Gift> gift = giftService.queryGift(id);
        return ResultDTO.success(gift);
    }
    @PostMapping("/saveOrEdit")
    public ResultDTO saveOrEditGift(@Validated @ModelAttribute Gift gift, @RequestParam("file") MultipartFile file) {
        boolean b = giftService.saveOrEditGift(gift, file);
        return b ? ResultDTO.success() : ResultDTO.error("保存失败！");
    }

    @DeleteMapping("/delete")
    public ResultDTO deleteGift(@Validated Long id) {
        boolean b = giftService.deleteGift(id);
        return b ? ResultDTO.success() : ResultDTO.error("删除失败！");
    }
    @GetMapping("/weeklyRank")
    public ResultDTO weeklyRank(@RequestParam Long liveRoomId) {
        List<ContributionRankVo> weeklyContributionRank = contributionRankService.getWeeklyContributionRank(liveRoomId);
        return ResultDTO.success(weeklyContributionRank);
    }
    @PostMapping("/sendGift")
    public ResultDTO sendGift(@RequestBody GiftSendRecord giftSendRecord) throws Exception {
        boolean b = giftService.sendGift(giftSendRecord);
        return b ? ResultDTO.success("发送成功!") : ResultDTO.error("发送失败！");
    }
    /**
     * 获取管理员总收入（从交易记录中统计）
     */
    /**
     * 获取系统总收入
     */
    @GetMapping("/admin/income")
    public ResultDTO getTotalIncome(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        try {
            // 处理时间参数
            LocalDateTime[] dates = processDateParams(startDate, endDate);
            startDate = dates[0];
            endDate = dates[1];

            // 查询总收入
            BigDecimal totalIncome = giftService.getTotalIncomeByDateRange(startDate, endDate);

            Map<String, Object> result = new HashMap<>();
            result.put("totalIncome", totalIncome);
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
            result.put("queryTime", LocalDateTime.now().toString());

            log.info("管理员查询总收入: 时间范围={}至{}, 收入={}", startDate, endDate, totalIncome);
            return ResultDTO.success("查询成功", result);

        } catch (Exception e) {
            log.error("查询总收入失败: {}", e.getMessage(), e);
            return ResultDTO.error("查询失败: " + e.getMessage());
        }
    }
    /**
     * 获取系统每日收入统计
     */
    @GetMapping("/daily-income")
    public ResultDTO getTotalDailyIncome(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        try {
            // 处理时间参数
            LocalDateTime[] dates = processDateParams(startDate, endDate);
            startDate = dates[0];
            endDate = dates[1];

            // 查询每日收入
            List<DailyIncomeVo> dailyIncome = giftService.getTotalDailyIncomeStats(startDate, endDate);

            Map<String, Object> result = new HashMap<>();
            result.put("dailyIncome", dailyIncome);
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
            result.put("totalDays", dailyIncome.size());

            log.info("管理员查询每日收入: 时间范围={}至{}, 共{}天",
                    startDate, endDate, dailyIncome.size());
            return ResultDTO.success("查询成功", result);

        } catch (Exception e) {
            log.error("查询每日收入失败: {}", e.getMessage(), e);
            return ResultDTO.error("查询失败: " + e.getMessage());
        }
    }


    /**
     * 获取指定直播间的总收入统计
     *
     */
    @GetMapping("/room/income")
    public ResultDTO getRoomIncome(
            @RequestParam Long liveRoomId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        try {
            // 参数验证
            if (liveRoomId == null) {
                return ResultDTO.error("直播间ID不能为空");
            }

            // 处理时间参数
            LocalDateTime[] dates = processDateParams(startDate, endDate);
            startDate = dates[0];
            endDate = dates[1];

            // 查询总收入
            BigDecimal roomIncome = giftService.getRoomIncomeByDateRange(liveRoomId, startDate, endDate);

            Map<String, Object> result = new HashMap<>();
            result.put("liveRoomId", liveRoomId);
            result.put("roomIncome", roomIncome);
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
            result.put("queryTime", LocalDateTime.now().toString());

            log.info("查询直播间收入: 房间ID={}, 时间范围={}至{}, 收入={}",
                    liveRoomId, startDate, endDate, roomIncome);
            return ResultDTO.success("查询成功", result);

        } catch (Exception e) {
            log.error("查询直播间收入失败: {}", e.getMessage(), e);
            return ResultDTO.error("查询失败: " + e.getMessage());
        }
    }
    /**
     * 获取直播间每日收入统计
     */
    @GetMapping("/room/daily-income")
    public ResultDTO getRoomDailyIncome(
            @RequestParam Long liveRoomId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        try {
            // 参数验证
            if (liveRoomId == null) {
                return ResultDTO.error("直播间ID不能为空");
            }

            // 处理时间参数
            LocalDateTime[] dates = processDateParams(startDate, endDate);
            startDate = dates[0];
            endDate = dates[1];

            // 查询每日收入
            List<DailyIncomeVo> dailyIncome = giftService.getRoomDailyIncomeStats(liveRoomId, startDate, endDate);

            Map<String, Object> result = new HashMap<>();
            result.put("liveRoomId", liveRoomId);
            result.put("dailyIncome", dailyIncome);
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
            result.put("totalDays", dailyIncome.size());

            log.info("查询直播间每日收入: 房间ID={}, 时间范围={}至{}, 共{}天",
                    liveRoomId, startDate, endDate, dailyIncome.size());
            return ResultDTO.success("查询成功", result);

        } catch (Exception e) {
            log.error("查询直播间每日收入失败: {}", e.getMessage(), e);
            return ResultDTO.error("查询失败: " + e.getMessage());
        }
    }
       @PostMapping("/transactions/page")
    public ResultDTO getTransactionPage(@RequestBody GiftTransaction giftTransaction) {
        log.info("分页动态查询：{}", giftTransaction);
        PageResult<GiftTransaction> pageResult = giftTransactionService.getByPage(giftTransaction);
        return ResultDTO.success(pageResult);
    }
    /**
     * 处理时间参数
     */
    private LocalDateTime[] processDateParams(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null && endDate == null) {
            // 默认查询最近30天
            endDate = LocalDateTime.now();
            startDate = endDate.minusDays(30).toLocalDate().atStartOfDay();
        } else if (startDate == null) {
            // 只有结束时间，查询从结束时间往前30天
            startDate = endDate.minusDays(30).toLocalDate().atStartOfDay();
        } else if (endDate == null) {
            // 只有开始时间，查询从开始时间到现在
            endDate = LocalDateTime.now();
        }
        return new LocalDateTime[]{startDate, endDate};
    }

}
