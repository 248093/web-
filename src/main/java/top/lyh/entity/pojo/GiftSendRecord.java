package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gift_send_record")  // 指定表名为 gift_send_record
public class GiftSendRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 礼物ID
     */
    private Long giftId;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 总价格
     */
    private BigDecimal totalPrice;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
