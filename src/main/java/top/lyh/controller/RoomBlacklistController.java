package top.lyh.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.PageResult;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.RoomBlacklistQueryDto;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.entity.pojo.RoomBlacklist;
import top.lyh.entity.vo.RoomBlacklistDetailVo;
import top.lyh.service.LiveRoomService;
import top.lyh.service.RoomBlacklistService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 直播间黑名单控制器
 */
@RestController
@RequestMapping("/api/room/blacklist")
@Slf4j
public class RoomBlacklistController {

    @Autowired
    private RoomBlacklistService roomBlacklistService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private LiveRoomService liveRoomService;


    @PostMapping("/page")
    public ResultDTO getBlacklistPage(@RequestBody RoomBlacklistQueryDto queryDto) {
        try {
            PageResult<RoomBlacklistDetailVo> result = roomBlacklistService.getBlacklistDetailPage(queryDto);
            return ResultDTO.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询黑名单详情异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "查询失败");
        }
    }

    /**
     * 拉黑并踢出用户（一体化操作）
     */
    @PostMapping("/add-and-kick")
    public ResultDTO blacklistAndKick(@RequestParam Long liveRoomId,
                                      @RequestParam Long userId,
                                      @RequestParam Long operatorId,
                                      @RequestParam(required = false) String reason) {
        try {
            LiveRoom liveRoom = liveRoomService.getById(liveRoomId);
            if (liveRoom.getUserId() != operatorId){
                return ResultDTO.error("您没有权限进行此操作");
            }
            // 执行拉黑操作
            boolean blacklisted = roomBlacklistService.blacklistUser(liveRoomId, userId, operatorId, reason);
            if (!blacklisted) {
                return ResultDTO.error("用户已在黑名单中");
            }

            // 立即踢出用户
            kickUserImmediately(liveRoomId, userId);

            log.info("用户 {} 被直播间 {} 拉黑并踢出", userId, liveRoomId);
            return ResultDTO.success("拉黑并踢出成功");

        } catch (Exception e) {
            log.error("拉黑并踢出失败: {}", e.getMessage(), e);
            return ResultDTO.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 简单的踢人实现
     */
    private void kickUserImmediately(Long liveRoomId, Long userId) {
        try {
            // 构造踢人消息
            Map<String, Object> kickMessage = new HashMap<>();
            kickMessage.put("type", "KICKED");
            kickMessage.put("roomId", liveRoomId);
            kickMessage.put("message", "您已被主播拉黑并踢出直播间");
            kickMessage.put("timestamp", System.currentTimeMillis());

            // 发送给指定用户
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/kick",
                    kickMessage
            );

            log.info("已发送踢人通知给用户: {}", userId);
        } catch (Exception e) {
            log.error("发送踢人通知失败: {}", e.getMessage(), e);
        }
    }



    /**
     * 取消拉黑
     */
    @DeleteMapping("/remove")
    public ResultDTO unblacklistUser(@RequestParam Long liveRoomId,
                                    @RequestParam Long userId) {
        try {
            boolean result = roomBlacklistService.unblacklistUser(liveRoomId, userId);
            if (result) {
                return ResultDTO.success("取消拉黑成功");
            } else {
                return ResultDTO.error("取消拉黑失败");
            }
        } catch (Exception e) {
            log.error("取消拉黑异常: {}", e.getMessage(), e);
            return ResultDTO.error("取消拉黑失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否被拉黑
     */
    @GetMapping("/check")
    public ResultDTO isUserBlacklisted(@RequestParam Long liveRoomId,
                                      @RequestParam(required = false) Long userId) {
        try {
            boolean isBlacklisted = roomBlacklistService.isUserBlacklisted(liveRoomId, userId);
            return ResultDTO.success("查询成功", isBlacklisted);
        } catch (Exception e) {
            log.error("检查黑名单异常: {}", e.getMessage(), e);
            return ResultDTO.error("查询失败: " + e.getMessage());
        }
    }
}
