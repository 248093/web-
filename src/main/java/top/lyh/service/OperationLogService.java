package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.entity.pojo.OperationLog;

/**
 * 操作日志Service接口
 */
public interface OperationLogService extends IService<OperationLog> {
    
    /**
     * 保存操作日志
     * @param operationLog 操作日志对象
     */
    void saveOperationLog(OperationLog operationLog);
}
