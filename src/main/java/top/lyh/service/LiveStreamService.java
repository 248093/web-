package top.lyh.service;

import top.lyh.entity.pojo.LiveRoom;
import java.util.List;
import java.util.Map;

/**
 * 直播流相关服务接口
 */
public interface LiveStreamService {

    /**
     * 创建直播间
     * @param liveRoom 直播间信息（包含用户ID、直播间标题等）
     * @return 创建成功后的直播间完整信息（含推流地址、播放地址等）
     */
    LiveRoom createLiveRoom(LiveRoom liveRoom);

    /**
     * 开始直播
     * @param roomId 直播间ID
     * @return 更新状态后的直播间信息
     */
    LiveRoom startLiveStream(Long roomId);

    /**
     * 结束直播
     * @param roomId 直播间ID
     * @return 更新状态后的直播间信息
     */
    LiveRoom endLiveStream(Long roomId);

    /**
     * 获取当前活跃的直播间列表（分页）
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 活跃直播间列表（状态为「直播中」）
     */
    List<LiveRoom> getActiveLiveRooms(int page, int size);

    /**
     * 获取热门直播间列表（按观看人数排序）
     * @param limit 限制返回条数
     * @return 热门直播间列表
     */
    List<LiveRoom> getHotLiveRooms(int limit);

    /**
     * 增加直播间观看人数（Redis计数，定期同步数据库）
     * @param roomId 直播间ID
     */
    void incrementViewCount(Long roomId);

    /**
     * 校验推流密钥合法性（SRS回调时使用）
     * @param streamKey 推流密钥
     * @param token 客户端携带的鉴权token
     * @param expire 过期时间戳（秒级）
     * @return true=鉴权通过，false=鉴权失败（过期/篡改）
     */
    boolean validateStreamKey(String streamKey, String token, String expire);

    /**
     * 处理SRS回调 - 流发布成功（客户端开始推流）
     * @param app SRS应用名（如配置中的「live」）
     * @param stream 推流密钥（对应直播间的streamKey）
     */
    void handleStreamPublish(String app, String stream);

    /**
     * 处理SRS回调 - 流关闭（客户端停止推流）
     * @param app SRS应用名（如配置中的「live」）
     * @param stream 推流密钥（对应直播间的streamKey）
     */
    void handleStreamClose(String app, String stream);

    /**
     * 获取SRS服务器状态信息（通过SRS HTTP API）
     * @return SRS服务器摘要信息（如连接数、流数量等），异常时返回空Map
     */
    Map<String, Object> getSrsServerInfo();
}