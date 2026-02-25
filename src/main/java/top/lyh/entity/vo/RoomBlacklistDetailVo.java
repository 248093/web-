package top.lyh.entity.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 直播间黑名单详情VO
 */
@Data
public class RoomBlacklistDetailVo {
    
    /**
     * 黑名单记录ID
     */
    private Long id;
    
    /**
     * 直播间ID
     */
    private Long liveRoomId;
    
    /**
     * 直播间标题
     */
    private String roomTitle;
    
    /**
     * 被封禁用户ID
     */
    private Long userId;
    
    /**
     * 被封禁用户名
     */
    private String userName;
    
    /**
     * 被封禁用户真实姓名
     */
    private String userTrueName;
    
    /**
     * 被封禁用户手机号
     */
    private String userPhone;
    
    /**
     * 被封禁用户头像
     */
    private String userAvatar;
    
    /**
     * 操作人ID（主播ID）
     */
    private Long operatorId;
    
    /**
     * 操作人用户名
     */
    private String operatorName;
    
    /**
     * 操作人真实姓名
     */
    private String operatorTrueName;
    
    /**
     * 拉黑原因
     */
    private String reason;
    
    /**
     * 封禁时间
     */
    private LocalDateTime createTime;
    
    /**
     * 封禁时长（天数）
     */
    private Long banDurationDays;
}
