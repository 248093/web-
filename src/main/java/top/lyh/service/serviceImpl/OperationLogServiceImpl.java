package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.lyh.entity.pojo.OperationLog;
import top.lyh.mapper.OperationLogMapper;
import top.lyh.service.OperationLogService;

/**
 * 操作日志Service实现类
 */
@Slf4j
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> 
    implements OperationLogService {

    @Override
    public void saveOperationLog(OperationLog operationLog) {
        try {
            this.save(operationLog);
            log.info("操作日志保存成功: 用户={}, 操作={}, 接口={}", 
                operationLog.getUserName(), operationLog.getOperation(), operationLog.getApiUrl());
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }
}
