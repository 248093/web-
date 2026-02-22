package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.entity.pojo.GiftTransaction;

import java.util.List;

public interface GiftTransactionService extends IService<GiftTransaction> {
    // 根据订单号查询
    GiftTransaction getByTransactionNo(String transactionNo);
}
