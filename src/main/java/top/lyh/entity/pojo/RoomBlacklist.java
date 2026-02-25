package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 直播间黑名单实体类
 */
@Data
@TableName("room_blacklist")
public class RoomBlacklist {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 直播间ID
     */
    private Long liveRoomId;

    /**
     * 被拉黑的用户ID
     */
    private Long userId;

    /**
     * 操作人ID（主播ID）
     */
    private Long operatorId;

    /**
     * 拉黑原因
     */
    private String reason;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
