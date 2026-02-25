package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.RoomBlacklistQueryDto;
import top.lyh.entity.pojo.RoomBlacklist;
import top.lyh.entity.vo.RoomBlacklistDetailVo;

import java.util.List;

/**
 * 直播间黑名单Service接口
 */
public interface RoomBlacklistService extends IService<RoomBlacklist> {
    
    /**
     * 拉黑用户
     * @param liveRoomId 直播间ID
     * @param userId 被拉黑的用户ID
     * @param operatorId 操作人ID
     * @param reason 拉黑原因
     * @return 是否成功
     */
    boolean blacklistUser(Long liveRoomId, Long userId, Long operatorId, String reason);
    
    /**
     * 取消拉黑
     * @param liveRoomId 直播间ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unblacklistUser(Long liveRoomId, Long userId);
    
    /**
     * 检查用户是否被拉黑
     * @param liveRoomId 直播间ID
     * @param userId 用户ID
     * @return 是否被拉黑
     */
    boolean isUserBlacklisted(Long liveRoomId, Long userId);
    
    /**
     * 获取直播间黑名单列表
     * @param liveRoomId 直播间ID
     * @return 黑名单列表
     */
    /**
     * 分页查询黑名单详情
     */
    PageResult<RoomBlacklistDetailVo> getBlacklistDetailPage(RoomBlacklistQueryDto queryDto);
}
