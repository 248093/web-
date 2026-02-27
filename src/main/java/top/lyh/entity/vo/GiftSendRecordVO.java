package top.lyh.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GiftSendRecordVO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 发送者用户名
     */
    private String senderName;
    
    /**
     * 接收者ID
     */
    private Long receiverId;
    
    /**
     * 接收者用户名（主播名）
     */
    private String receiverName;
    
    /**
     * 直播间ID
     */
    private Long liveRoomId;
    
    /**
     * 直播间标题
     */
    private String liveRoomTitle;
    
    /**
     * 礼物ID
     */
    private Long giftId;
    
    /**
     * 礼物名称
     */
    private String giftName;
    
    /**
     * 礼物图片
     */
    private String giftImageUrl;
    
    /**
     * 数量
     */
    private Integer quantity;
    
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    
    /**
     * 总价格
     */
    private BigDecimal totalPrice;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 格式化的时间显示
     */
    private String createdAtFormatted;
}
