// 创建 DTO 类
package top.lyh.entity.dto;

import lombok.Data;
import top.lyh.entity.pojo.LiveRecording;

@Data
public class LiveRecordingQueryDto {
    private LiveRecording liveRecording;
    private int page = 1;
    private int size = 10;
}