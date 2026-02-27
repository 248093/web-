package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.GiftTransactionQueryDto;
import top.lyh.entity.pojo.GiftTransaction;

import java.util.List;

public interface GiftTransactionService extends IService<GiftTransaction> {
    // 根据订单号查询
    GiftTransaction getByTransactionNo(String transactionNo);
    // 分页动态查询
    PageResult<GiftTransaction> getByPage(GiftTransactionQueryDto giftTransaction);
}
