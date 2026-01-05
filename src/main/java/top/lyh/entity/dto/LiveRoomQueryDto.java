package top.lyh.entity.dto;

import lombok.Data;

@Data
public class LiveRoomQueryDto {

    /**
     * 直播间ID
     */
    private Long id;

    /**
     * 直播间标题（模糊查询）
     */
    private String title;

    /**
     * 主播用户ID
     */
    private Long userId;

    /**
     * 主播用户名（模糊查询）
     */
    private String userName;

    /**
     * 直播状态：0-未开播 1-直播中 2-直播结束
     */
    private Integer status;

    /**
     * 排序字段：
     * - score: 综合评分 = view_count * 5 + like_count * 1.5
     * - view_count: 观看人数
     * - like_count: 点赞数
     * - start_time: 开播时间
     */
    private String orderBy = "score";

    /**
     * 排序方式：ASC, DESC
     */
    private String orderType = "DESC";

    /**
     * 是否只查询启用状态的用户
     */
    private Boolean enabledOnly = true;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;
}