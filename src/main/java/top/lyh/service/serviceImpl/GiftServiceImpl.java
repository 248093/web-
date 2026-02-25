package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.entity.pojo.*;
import top.lyh.entity.vo.DailyIncomeVo;
import top.lyh.exceptionHandler.BaseException;
import top.lyh.mapper.*;
import org.springframework.stereotype.Service;
import top.lyh.service.GiftService;
import top.lyh.service.GiftTransactionService;
import top.lyh.utils.AliOSSUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author lyh
* @description 针对表【gift】的数据库操作Service实现
* @createDate 2026-02-12 20:38:30
*/
@Service
@Slf4j
public class GiftServiceImpl extends ServiceImpl<GiftMapper, Gift>
    implements GiftService {
    @Autowired
    private AliOSSUtils aliOSSUtils;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private GiftSendRecordMapper giftSendRecordMapper;
    @Autowired
    private LiveRoomMapper liveRoomMapper;
    @Autowired
    private ContributionRankMapper contributionRankMapper;
    @Autowired
    private GiftTransactionMapper giftTransactionMapper;
    @Override
    public boolean saveOrEditGift(Gift gift, MultipartFile file) {
        try {
            String upload = aliOSSUtils.upload(file,"giftIcon");
            gift.setImageUrl(upload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (gift.getId() == null) {
            List<Gift> allGifts = queryGift(null);
            if (allGifts.size() >= 50) {
                // 礼物数量超过限制，抛出异常或返回错误信息
                throw new IllegalStateException("礼物数量已达到上限（50条），无法继续添加！");
            }
            return save(gift); // 保存新礼物
        }else {
            return updateById(gift);
        }
    }

    @Override
    public boolean deleteGift(Long id) {
        return removeById(id);
    }

    @Override
    public List<Gift> queryGift(Long id) {
        if (id == null) {
            List<Gift> allGifts = list(); // list() 查询所有数据
            return allGifts.isEmpty() ? null : allGifts; // 返回第一条数据或 null
        }
        return List.of(getById(id)); // 正常根据 ID 查询
    }

    @Override
    @Transactional
    public boolean sendGift(GiftSendRecord giftSendRecord) {
        // 送礼物的用户
        Long senderId = giftSendRecord.getSenderId();
        // 接收礼物的主播
        Long receiverId = giftSendRecord.getReceiverId();
        // 直播间ID
        Long liveRoomId = giftSendRecord.getLiveRoomId();
        // 礼物价值
        BigDecimal totalPrice = giftSendRecord.getTotalPrice();

        // 查询用户并判断是否有足够的余额，并且减去余额
        try {
            // 1. 查询发送者用户信息
            SysUser sysUser = sysUserMapper.selectById(senderId);
            if (sysUser == null) {
                throw new BaseException(ResponseCodeEnum.NOT_FOUND, "发送者用户不存在");
            }
            if (senderId.equals(receiverId)) {
                throw new BaseException(ResponseCodeEnum.BAD_REQUEST, "不能给自己送礼物");
            }
            // 2. 检查余额是否充足
            if (sysUser.getAccountMoney().compareTo(totalPrice) < 0) {
                throw new BaseException(ResponseCodeEnum.BAD_REQUEST, "账户余额不足，无法发送礼物");
            }

            // 3. 扣减发送者余额
            BigDecimal newSenderMoney = sysUser.getAccountMoney().subtract(totalPrice);
            sysUser.setAccountMoney(newSenderMoney);
            sysUserMapper.updateById(sysUser);

            // 4. 增加接收者（主播）余额
            SysUser receiverUser = sysUserMapper.selectById(receiverId);
            if (receiverUser == null) {
                throw new BaseException(ResponseCodeEnum.NOT_FOUND, "接收者用户不存在");
            }

            BigDecimal newReceiverMoney = receiverUser.getAccountMoney().add(totalPrice);
            receiverUser.setAccountMoney(newReceiverMoney);
            sysUserMapper.updateById(receiverUser);

            // 5. 更新直播间总收益（amount_count字段）
            LiveRoom liveRoom = liveRoomMapper.selectById(liveRoomId);
            if (liveRoom == null) {
                throw new BaseException(ResponseCodeEnum.NOT_FOUND, "直播间不存在");
            }

            BigDecimal newAmountCount = liveRoom.getAmountCount().add(totalPrice);
            liveRoom.setAmountCount(newAmountCount);
            liveRoomMapper.updateById(liveRoom);

            // 6. 更新贡献排行榜（contribution_rank表）
            // 先查询是否存在该用户的贡献记录
            ContributionRank contributionRank = contributionRankMapper.selectOne(
                    new LambdaQueryWrapper<ContributionRank>()
                            .eq(ContributionRank::getSenderId, senderId)
                            .eq(ContributionRank::getLiveRoomId, liveRoomId)
            );

            if (contributionRank == null) {
                // 如果不存在，则创建新的贡献记录
                contributionRank = new ContributionRank();
                contributionRank.setSenderId(senderId);
                contributionRank.setLiveRoomId(liveRoomId);
                contributionRank.setTotalAmount(totalPrice);
                contributionRank.setCreatedAt(new Date());
                contributionRankMapper.insert(contributionRank);
            } else {
                // 如果已存在，则更新
                BigDecimal newTotalAmount = contributionRank.getTotalAmount().add(totalPrice);
                contributionRank.setTotalAmount(newTotalAmount);
                contributionRankMapper.updateById(contributionRank);
            }

            // 7. 保存礼物发送记录
            giftSendRecord.setCreatedAt(LocalDateTime.now());
            giftSendRecordMapper.insert(giftSendRecord);

            return true;
        } catch (BaseException e) {
            // BaseException 会被 BaseExceptionHandler 捕获并返回给前端
            throw e;
        } catch (Exception e) {
            // 其他异常包装为 BaseException
            throw new BaseException(ResponseCodeEnum.ERROR, "发送礼物失败: " + e.getMessage());
        }
    }
    @Override
    public BigDecimal getTotalIncomeByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<GiftTransaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(GiftTransaction::getAmount);
        queryWrapper.eq(GiftTransaction::getStatus, GiftTransaction.TransactionStatus.success);
        queryWrapper.ge(GiftTransaction::getCreatedAt, startDate);
        queryWrapper.le(GiftTransaction::getCreatedAt, endDate);

        List<GiftTransaction> transactions = giftTransactionMapper.selectList(queryWrapper);

        return transactions.stream()
                .map(GiftTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    @Override
    public List<DailyIncomeVo> getTotalDailyIncomeStats(LocalDateTime startDate, LocalDateTime endDate) {
        // 查询指定时间范围内的所有成功交易
        LambdaQueryWrapper<GiftTransaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                GiftTransaction::getCreatedAt,
                GiftTransaction::getAmount
        );
        queryWrapper.eq(GiftTransaction::getStatus, GiftTransaction.TransactionStatus.success);
        queryWrapper.ge(GiftTransaction::getCreatedAt, startDate);
        queryWrapper.le(GiftTransaction::getCreatedAt, endDate);
        queryWrapper.orderByAsc(GiftTransaction::getCreatedAt);

        List<GiftTransaction> transactions = giftTransactionMapper.selectList(queryWrapper);

        // 按日期分组求和
        Map<String, BigDecimal> dailyMap = transactions.stream()
                .filter(t -> t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate().toString(),
                        Collectors.mapping(
                                GiftTransaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // 生成日期范围内的所有日期
        List<DailyIncomeVo> result = generateDailyIncomeList(dailyMap, startDate, endDate);

        log.info("查询系统每日收入: 日期范围={}至{}, 共{}天",
                startDate, endDate, result.size());

        return result;
    }
    /**
     * 生成日期范围内的每日收入列表
     */
    private List<DailyIncomeVo> generateDailyIncomeList(Map<String, BigDecimal> dailyMap,
                                                        LocalDateTime startDate,
                                                        LocalDateTime endDate) {
        List<DailyIncomeVo> result = new ArrayList<>();
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();

        while (!current.isAfter(end)) {
            DailyIncomeVo vo = new DailyIncomeVo();
            vo.setDate(current.toString());
            vo.setIncome(dailyMap.getOrDefault(current.toString(), BigDecimal.ZERO));
            result.add(vo);
            current = current.plusDays(1);
        }

        return result;
    }

    @Override
    public BigDecimal getRoomIncomeByDateRange(Long liveRoomId, LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<GiftSendRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(GiftSendRecord::getTotalPrice);
        queryWrapper.eq(GiftSendRecord::getLiveRoomId, liveRoomId);
        queryWrapper.ge(GiftSendRecord::getCreatedAt, startDate);
        queryWrapper.le(GiftSendRecord::getCreatedAt, endDate);

        List<GiftSendRecord> records = giftSendRecordMapper.selectList(queryWrapper);

        return records.stream()
                .map(GiftSendRecord::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    @Override
    public List<DailyIncomeVo> getRoomDailyIncomeStats(Long liveRoomId, LocalDateTime startDate, LocalDateTime endDate) {
        // 查询指定时间范围内的所有送礼记录
        LambdaQueryWrapper<GiftSendRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                GiftSendRecord::getCreatedAt,
                GiftSendRecord::getTotalPrice
        );
        queryWrapper.eq(GiftSendRecord::getLiveRoomId, liveRoomId);
        queryWrapper.ge(GiftSendRecord::getCreatedAt, startDate);
        queryWrapper.le(GiftSendRecord::getCreatedAt, endDate);
        queryWrapper.orderByAsc(GiftSendRecord::getCreatedAt);

        List<GiftSendRecord> records = giftSendRecordMapper.selectList(queryWrapper);

        // 按日期分组求和
        Map<String, BigDecimal> dailyMap = records.stream()
                .filter(r -> r.getTotalPrice() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate().toString(),
                        Collectors.mapping(
                                GiftSendRecord::getTotalPrice,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // 生成日期范围内的所有日期
        List<DailyIncomeVo> result = generateDailyIncomeList(dailyMap, startDate, endDate);

        log.info("查询直播间每日收入: 房间ID={}, 日期范围={}至{}, 共{}天",
                liveRoomId, startDate, endDate, result.size());

        return result;
    }
}




