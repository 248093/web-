package top.lyh.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.lyh.anno.LogAnnotation;
import top.lyh.common.PageResult;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.GiftQueryDto;
import top.lyh.entity.dto.GiftSendRecordQueryDto;
import top.lyh.entity.dto.GiftTransactionQueryDto;
import top.lyh.entity.pojo.Gift;
import top.lyh.entity.pojo.GiftSendRecord;
import top.lyh.entity.pojo.GiftTransaction;
import top.lyh.entity.vo.ContributionRankVo;
import top.lyh.entity.vo.DailyIncomeVo;
import top.lyh.entity.vo.GiftSendRecordVO;
import top.lyh.service.ContributionRankService;
import top.lyh.service.GiftSendRecordService;
import top.lyh.service.GiftService;
import top.lyh.service.GiftTransactionService;
import top.lyh.utils.AliOSSUtils;

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
    @Autowired
    private GiftSendRecordService giftSendRecordService;
    @Autowired
    private AliOSSUtils aliOSSUtils;

    @GetMapping("/list")
    @LogAnnotation(value = "根据id查询礼物", recordParams = false, recordResult = true)
    public ResultDTO gitfList(@Validated Long id) {
        List<Gift> gift = giftService.queryGift(id);
        return ResultDTO.success(gift);
    }

    @PostMapping("/list")
    @LogAnnotation(value = "分页查询礼物列表")
    public ResultDTO gitfList(@RequestBody GiftQueryDto gift) {
        PageResult<Gift> giftPageResult = giftService.queryGiftByPage(gift);
        return ResultDTO.success(giftPageResult);
    }

    @PostMapping("/saveOrEdit")
    @RequiresRoles("ADMIN")
    @LogAnnotation(value = "保存或更新礼物信息")
    public ResultDTO saveOrEditGift(@RequestBody Gift gift) {
        boolean b = giftService.saveOrEditGift(gift);
        return b ? ResultDTO.success() : ResultDTO.error("保存失败！");
    }

    @PostMapping("/uploadAvatar")
    @RequiresRoles("ADMIN")
    @LogAnnotation(value = "上传礼物封面", recordParams = false, recordResult = true)
    public ResultDTO uploadAvatar(@RequestParam MultipartFile file) {
        try {
            String url = aliOSSUtils.upload(file, "giftIcon");
            return ResultDTO.success("上传成功", url);
        } catch (Exception e) {
            log.error("上传失败", e);
            return ResultDTO.error("上传失败");
        }
    }

    @DeleteMapping("/delete/{id}")
    @RequiresRoles("ADMIN")
    @LogAnnotation(value = "删除礼物")
    public ResultDTO deleteGift(@PathVariable @Validated Long id) {
        boolean b = giftService.deleteGift(id);
        return b ? ResultDTO.success() : ResultDTO.error("删除失败！");
    }

    @GetMapping("/weeklyRank")
    @LogAnnotation(value = "查询本周贡献排行榜")
    public ResultDTO weeklyRank(@RequestParam Long liveRoomId) {
        List<ContributionRankVo> weeklyContributionRank = contributionRankService.getWeeklyContributionRank(liveRoomId);
        return ResultDTO.success(weeklyContributionRank);
    }

    @PostMapping("/sendGift")
    @RequiresAuthentication
    @LogAnnotation(value = "发送礼物")
    public ResultDTO sendGift(@RequestBody GiftSendRecord giftSendRecord) throws Exception {
        boolean b = giftService.sendGift(giftSendRecord);
        return b ? ResultDTO.success("发送成功!") : ResultDTO.error("发送失败！");
    }

    @PostMapping("/send-records/page")
    @LogAnnotation(value = "分页查询礼物赠送记录", recordParams = true, recordResult = true)
    @RequiresAuthentication
    public ResultDTO queryGiftSendRecords(@RequestBody GiftSendRecordQueryDto queryDto) {
        try {
            PageResult<GiftSendRecordVO> result = giftSendRecordService.queryGiftSendRecords(queryDto);
            return ResultDTO.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询礼物赠送记录异常", e);
            return ResultDTO.error("查询失败：" + e.getMessage());
        }
    }

    // 退回金额账户
    @PutMapping("/refund")
    @RequiresRoles("ADMIN")
    @LogAnnotation(value = "退回金额账户")
    public ResultDTO refund(@RequestParam Long recordId) {
        boolean refund = giftSendRecordService.refund(recordId);
        return refund ? ResultDTO.success("退回成功") : ResultDTO.error("退回失败");
    }

    /**
     * 获取系统总收入
     */
    @GetMapping("/admin/income")
    @LogAnnotation(value = "获取系统总收入")
    @RequiresRoles("ADMIN")
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
    @GetMapping("/admin/daily-income")
    @LogAnnotation(value = "获取系统每日收入统计")
    @RequiresRoles("ADMIN")
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
     */
    @GetMapping("/room/income")
    @LogAnnotation(value = "获取指定直播间的总收入统计")
    @RequiresRoles(value = {"ADMIN", "HOST"} , logical = Logical.OR)
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
    @RequiresRoles(value = {"ADMIN", "HOST"}, logical = Logical.OR)
    @LogAnnotation(value = "获取直播间每日收入统计")
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
    @LogAnnotation(value = "分页查询礼物赠送记录", recordParams = true, recordResult = true)
    @RequiresRoles("ADMIN")
    public ResultDTO getTransactionPage(@RequestBody GiftTransactionQueryDto giftTransaction) {
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
