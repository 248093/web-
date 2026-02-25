package top.lyh.entity.dto;

import lombok.Data;

/**
 * 直播间黑名单查询条件DTO
 */
@Data
public class RoomBlacklistQueryDto {
    
    /**
     * 直播间ID（可选）
     */
    private Long liveRoomId;
    
    /**
     * 用户ID（可选）
     */
    private Long userId;
    
    /**
     * 操作人ID（可选）
     */
    private Long operatorId;
    
    /**
     * 用户名模糊查询（可选）
     */
    private String userName;
    
    /**
     * 手机号模糊查询（可选）
     */
    private String userPhone;
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 排序字段
     */
    private String orderBy = "createTime";
    
    /**
     * 排序方式
     */
    private String orderType = "DESC";
}
