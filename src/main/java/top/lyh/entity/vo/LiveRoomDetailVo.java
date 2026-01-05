package top.lyh.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LiveRoomDetailVo {

    /**
     * 直播间ID
     */
    private Long id;

    /**
     * 直播间标题
     */
    private String title;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 主播用户ID
     */
    private Long userId;

    /**
     * 主播用户名
     */
    private String userName;

    /**
     * HLS播放地址
     */
    private String hlsUrl;

    /**
     * 直播状态：0-未开播 1-直播中 2-直播结束
     */
    private Integer status;

    /**
     * 观看人数
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 开播时间
     */
    private LocalDateTime startTime;
}