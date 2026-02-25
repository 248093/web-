package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.RoomBlacklistQueryDto;
import top.lyh.entity.pojo.RoomBlacklist;
import top.lyh.entity.vo.RoomBlacklistDetailVo;
import top.lyh.mapper.RoomBlacklistMapper;
import top.lyh.service.RoomBlacklistService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 直播间黑名单Service实现类
 */
@Slf4j
@Service
public class RoomBlacklistServiceImpl extends ServiceImpl<RoomBlacklistMapper, RoomBlacklist> 
    implements RoomBlacklistService {

    @Override
    public boolean blacklistUser(Long liveRoomId, Long userId, Long operatorId, String reason) {
        try {
            // 检查是否已经拉黑
            if (isUserBlacklisted(liveRoomId, userId)) {
                log.warn("用户 {} 已经被直播间 {} 拉黑", userId, liveRoomId);
                return false;
            }
            
            // 创建拉黑记录
            RoomBlacklist blacklist = new RoomBlacklist();
            blacklist.setLiveRoomId(liveRoomId);
            blacklist.setUserId(userId);
            blacklist.setOperatorId(operatorId);
            blacklist.setReason("禁言");
            blacklist.setCreateTime(LocalDateTime.now());
            blacklist.setReason(reason != null ? reason : "违规行为");
            
            boolean result = this.save(blacklist);
            if (result) {
                log.info("用户 {} 被直播间 {} 拉黑成功", userId, liveRoomId);
            }
            return result;
        } catch (Exception e) {
            log.error("拉黑用户失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean unblacklistUser(Long liveRoomId, Long userId) {
        try {
            LambdaQueryWrapper<RoomBlacklist> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(RoomBlacklist::getLiveRoomId, liveRoomId)
                       .eq(RoomBlacklist::getUserId, userId);
            
            boolean result = this.remove(queryWrapper);
            if (result) {
                log.info("用户 {} 从直播间 {} 黑名单中移除成功", userId, liveRoomId);
            }
            return result;
        } catch (Exception e) {
            log.error("取消拉黑用户失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isUserBlacklisted(Long liveRoomId, Long userId) {
        LambdaQueryWrapper<RoomBlacklist> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoomBlacklist::getLiveRoomId, liveRoomId)
                   .eq(RoomBlacklist::getUserId, userId);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public PageResult<RoomBlacklistDetailVo> getBlacklistDetailPage(RoomBlacklistQueryDto queryDto) {
        try {
            // 设置默认分页参数
            if (queryDto.getPage() == null) queryDto.setPage(1);
            if (queryDto.getSize() == null) queryDto.setSize(10);

            // 创建分页对象
            Page<RoomBlacklistDetailVo> page = new Page<>(queryDto.getPage(), queryDto.getSize());

            // 执行分页查询
            IPage<RoomBlacklistDetailVo> result = getBaseMapper().selectBlacklistDetailPage(page, queryDto);

            // 转换为PageResult
            return new PageResult<>(
                    result.getRecords(),
                    result.getTotal(),
                    queryDto.getPage(),
                    queryDto.getSize()
            );

        } catch (Exception e) {
            log.error("分页查询黑名单详情失败", e);
            throw new RuntimeException("查询失败", e);
        }
    }
}
