package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.GiftTransactionQueryDto;
import top.lyh.entity.pojo.GiftTransaction;
import top.lyh.mapper.GiftTransactionMapper;
import top.lyh.service.GiftTransactionService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class GiftTransactionServiceImpl extends ServiceImpl<GiftTransactionMapper, GiftTransaction> implements GiftTransactionService {

    @Override
    public GiftTransaction getByTransactionNo(String transactionNo) {
        LambdaQueryWrapper<GiftTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GiftTransaction::getTransactionNo, transactionNo);
        return this.getOne(wrapper);
    }

    @Override
    public PageResult<GiftTransaction> getByPage(GiftTransactionQueryDto giftTransaction) {
        // 2. 创建 MyBatis-Plus 分页对象
        Page<GiftTransaction> page = new Page<>(
                giftTransaction.getPage(),
                giftTransaction.getSize()
        );

        // 3. 构建查询条件
        LambdaQueryWrapper<GiftTransaction> wrapper = new LambdaQueryWrapper<>();

        // 用户ID条件
        if (giftTransaction.getUserId() != null) {
            wrapper.eq(GiftTransaction::getUserId, giftTransaction.getUserId());
        }

        // 状态条件
        if (giftTransaction.getStatus() != null) {
            wrapper.eq(GiftTransaction::getStatus, giftTransaction.getStatus());
        }

        // 支付类型条件
        if (giftTransaction.getPaymentType() != null) {
            wrapper.eq(GiftTransaction::getPaymentType, giftTransaction.getPaymentType());
        }
        if (giftTransaction.getTransactionNo() != null){
            wrapper.eq(GiftTransaction::getTransactionNo, giftTransaction.getTransactionNo());
        }
        // 4. 排序
        // 注意：这里应该始终按创建时间倒序排序
        wrapper.orderByDesc(GiftTransaction::getCreatedAt);

        // 5. 执行分页查询
        Page<GiftTransaction> pageData = this.page(page, wrapper);
         log.info("分页查询结果：{}", pageData.getRecords());

        // 6. 创建并返回 PageResult
        PageResult<GiftTransaction> result = new PageResult<>(
                pageData.getRecords(),
                pageData.getTotal(),
                (int) pageData.getCurrent(),
                (int) pageData.getSize()
        );

        return result;
    }
    /**
     * 创建订单（插入数据）
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(GiftTransaction transaction) {
        // 设置枚举值
        transaction.setPaymentType(GiftTransaction.PaymentType.alipay);
        transaction.setStatus(GiftTransaction.TransactionStatus.pending);
        transaction.setCreatedAt(LocalDateTime.now());

        // 插入数据库
        this.save(transaction);
        log.info("订单创建成功：{}", transaction.getTransactionNo());
    }

    /**
     * 支付成功更新订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String transactionNo, String alipayTradeNo) {
        // 创建更新对象
        GiftTransaction transaction = new GiftTransaction();
        transaction.setTransactionNo(transactionNo);
        transaction.setStatus(GiftTransaction.TransactionStatus.success);
        // 注意：如果数据库有支付宝交易号字段，需要添加到实体类中
        // transaction.setAlipayTradeNo(alipayTradeNo);

        // 更新条件
        LambdaQueryWrapper<GiftTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GiftTransaction::getTransactionNo, transactionNo);

        this.update(transaction, wrapper);
        log.info("订单支付成功更新：{}", transactionNo);
    }
}
