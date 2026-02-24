package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.LiveRoomQueryDto;
import top.lyh.entity.vo.LiveRoomDetailVo;
import top.lyh.entity.pojo.LiveRoom;

import java.util.List;

public interface LiveRoomService extends IService<LiveRoom> {
    
    /**
     * 创建直播间
     */
    LiveRoom createLiveRoom(LiveRoom liveRoom);
    
    /**
     * 根据ID获取直播间
     */
    LiveRoom getById(Long roomId);
    
    /**
     * 分页查询直播间列表
     */
    PageResult<LiveRoomDetailVo> getActiveLiveRooms(LiveRoomQueryDto query);

    
    /**
     * 更新直播间状态
     */
    boolean updateStatus(Long roomId, Integer status);

    
    /**
     * 获取直播间详情VO
     */
    LiveRoomDetailVo getLiveRoomDetail(Long roomId);
    /**
     * 根据streamKey查找直播间
     */
    LiveRoom findByStreamKey(String streamKey);
}