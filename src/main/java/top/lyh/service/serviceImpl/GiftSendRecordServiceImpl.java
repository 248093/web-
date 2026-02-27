package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.GiftSendRecordQueryDto;
import top.lyh.entity.pojo.GiftSendRecord;
import top.lyh.entity.pojo.SysUser;
import top.lyh.entity.vo.GiftSendRecordVO;
import top.lyh.mapper.GiftSendRecordMapper;
import top.lyh.service.GiftSendRecordService;
import top.lyh.service.SysUserService;

import java.math.BigDecimal;

@Service
@Slf4j
public class GiftSendRecordServiceImpl extends ServiceImpl<GiftSendRecordMapper, GiftSendRecord> 
    implements GiftSendRecordService {

    @Autowired
    private GiftSendRecordMapper giftSendRecordMapper;
    @Autowired
    private SysUserService sysUserService;

    @Override
    public PageResult<GiftSendRecordVO> queryGiftSendRecords(GiftSendRecordQueryDto queryDto) {
        try {
            // 参数校验和默认值设置
            if (queryDto == null) {
                queryDto = new GiftSendRecordQueryDto();
            }
            
            if (queryDto.getPage() == null || queryDto.getPage() < 1) {
                queryDto.setPage(1);
            }
            if (queryDto.getPageSize() == null || queryDto.getPageSize() < 1) {
                queryDto.setPageSize(10);
            }

            log.info("礼物赠送记录分页查询，参数: {}", queryDto);
            
            // 创建分页对象
            Page<GiftSendRecordVO> page = new Page<>(queryDto.getPage(), queryDto.getPageSize());
            
            // 执行分页查询
            IPage<GiftSendRecordVO> resultPage = giftSendRecordMapper.selectByCondition(page, queryDto);
            
            PageResult<GiftSendRecordVO> pageResult = new PageResult<>(
                resultPage.getRecords(),
                resultPage.getTotal(),
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize()
            );
            
            log.info("礼物赠送记录查询完成，返回记录数: {}", pageResult.getRecords().size());
            return pageResult;
            
        } catch (Exception e) {
            log.error("礼物赠送记录分页查询异常，参数: {}", queryDto, e);
            throw new RuntimeException("查询礼物赠送记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean refund(Long recordId) {
            log.info("开始执行礼物赠送记录退款，参数: {}", recordId);

            // 获取礼物赠送记录
            GiftSendRecord record = this.getById(recordId);
            if (record == null) {
                log.warn("礼物赠送记录不存在，参数: {}", recordId);
                return false;
            }
            SysUser refundUser = sysUserService.getById(record.getSenderId());
            refundUser.setAccountMoney(refundUser.getAccountMoney().add(record.getTotalPrice()));
            sysUserService.updateById(refundUser);
            boolean b = removeById(recordId);
            return  b;
    }

}
