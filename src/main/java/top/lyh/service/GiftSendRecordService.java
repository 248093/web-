package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.GiftSendRecordQueryDto;
import top.lyh.entity.pojo.GiftSendRecord;
import top.lyh.entity.vo.GiftSendRecordVO;

import java.math.BigDecimal;

public interface GiftSendRecordService extends IService<GiftSendRecord> {
    
    /**
     * 动态分页查询礼物赠送记录
     */
    PageResult<GiftSendRecordVO> queryGiftSendRecords(GiftSendRecordQueryDto queryDto);

    boolean refund(Long recordId);
}
