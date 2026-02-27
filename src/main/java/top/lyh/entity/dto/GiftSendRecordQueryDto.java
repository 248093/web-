package top.lyh.entity.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GiftSendRecordQueryDto {
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 发送者用户名（模糊查询）
     */
    private String senderName;
    
    /**
     * 接收者ID
     */
    private Long receiverId;
    
    /**
     * 接收者用户名（模糊查询）
     */
    private String receiverName;
    
    /**
     * 直播间ID
     */
    private Long liveRoomId;
    
    /**
     * 礼物ID
     */
    private Long giftId;
    
    /**
     * 礼物名称（模糊查询）
     */
    private String giftName;
    
    /**
     * 创建时间开始
     */
    private LocalDateTime createdAtStart;
    
    /**
     * 创建时间结束
     */
    private LocalDateTime createdAtEnd;
    
    /**
     * 页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
