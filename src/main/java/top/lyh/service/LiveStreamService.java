package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.entity.pojo.LiveStream;

import java.util.Map;

public interface LiveStreamService extends IService<LiveStream> {

    /**
     * 开始直播
     */
    LiveRoom startLiveStream(Long roomId);

    /**
     * 结束直播
     */
    LiveRoom endLiveStream(Long roomId);

    /**
     * 处理SRS回调 - 流发布
     */
    void handleStreamPublish(String app, String stream);

    /**
     * 处理SRS回调 - 流关闭
     */
    void handleStreamClose(String app, String stream);

    /**
     * 获取SRS服务器信息
     */
    Map<String, Object> getSrsServerInfo();
    boolean validateStreamKey(String streamKey, String token, String expire);
}