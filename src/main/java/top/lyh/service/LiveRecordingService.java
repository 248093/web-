package top.lyh.service;

import top.lyh.entity.pojo.LiveRecording;
import java.util.List;

/**
 * 直播录制相关服务接口
 */
public interface LiveRecordingService {

    /**
     * 开始直播录制（需直播间已开播）
     * @param roomId 直播间ID
     * @return 创建成功的录制任务信息（含录制文件名、状态等）
     */
    LiveRecording startRecording(Long roomId);

    /**
     * 停止直播录制（需录制任务处于「录制中」状态）
     * @param recordingId 录制任务ID
     * @return 更新状态后的录制任务信息
     */
    LiveRecording stopRecording(Long recordingId);

    /**
     * 获取直播间的直播回放列表（分页查询，仅返回「可用」状态的回放）
     * @param roomId 直播间ID
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 分页后的直播回放列表
     */
    List<LiveRecording> getRecordings(Long roomId, int page, int size);
}