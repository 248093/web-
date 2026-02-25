package top.lyh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.ResultDTO;
import top.lyh.entity.pojo.DailyVisitStat;
import top.lyh.entity.pojo.OperationLog;
import top.lyh.service.DailyVisitStatService;
import top.lyh.service.OperationLogService;

import java.time.LocalDate;
import java.util.List;

/**
 * 日志相关控制器
 */
@RestController
@RequestMapping("/api/log")
@Slf4j
public class LogController {

    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private DailyVisitStatService dailyVisitStatService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/operations")
    public ResultDTO getOperationLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        
        if (userName != null && !userName.isEmpty()) {
            queryWrapper.like(OperationLog::getUserName, userName);
        }
        if (operation != null && !operation.isEmpty()) {
            queryWrapper.like(OperationLog::getOperation, operation);
        }
        if (startDate != null) {
            queryWrapper.ge(OperationLog::getCreateTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            queryWrapper.le(OperationLog::getCreateTime, endDate.plusDays(1).atStartOfDay());
        }
        
        queryWrapper.orderByDesc(OperationLog::getCreateTime);
        
        Page<OperationLog> result = operationLogService.page(page, queryWrapper);
        return ResultDTO.success(result);
    }

    /**
     * 查询访问统计
     */
    @GetMapping("/visit-stats")
    public ResultDTO getVisitStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<DailyVisitStat> stats = dailyVisitStatService.getVisitStatsByDateRange(startDate, endDate);
        return ResultDTO.success(stats);
    }

}
