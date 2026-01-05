package top.lyh.service;

import top.lyh.entity.dto.LiveRoomQueryDto;
import top.lyh.entity.vo.LiveRoomDetailVo;
import top.lyh.entity.pojo.LiveRoom;

import java.util.List;

public interface LiveRoomService {
    
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
    List<LiveRoomDetailVo> getActiveLiveRooms(LiveRoomQueryDto query);
    
    /**
     * 获取热门直播间
     */
    List<LiveRoom> getHotLiveRooms(int limit);
    
    /**
     * 更新直播间状态
     */
    boolean updateStatus(Long roomId, Integer status);
    
    /**
     * 增加直播间观看人数
     */
    void incrementViewCount(Long roomId);
    
    /**
     * 增加直播间点赞数
     */
    void incrementLikeCount(Long roomId);
    
    /**
     * 获取直播间详情VO
     */
    LiveRoomDetailVo getLiveRoomDetail(Long roomId);
    /**
     * 根据streamKey查找直播间
     */
    LiveRoom findByStreamKey(String streamKey);
}