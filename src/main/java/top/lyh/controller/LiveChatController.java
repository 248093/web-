package top.lyh.controller;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.mapper.LiveRoomMapper;
import top.lyh.utils.SensitiveWordFilter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class LiveChatController {
    
    @Autowired
    private LiveRoomMapper liveRoomMapper;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
 
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    /**
     * 发送聊天消息
     */
    @MessageMapping("/chat/{roomId}")
    @RequiresAuthentication
    public void sendMessage(@DestinationVariable Long roomId, ChatMessage message) {
        try {
            // 检查直播间是否存在
            LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
            if (liveRoom == null || liveRoom.getStatus() != 1) {
                log.warn("直播间不存在或未开播，roomId: {}", roomId);
                return;
            }

            // 敏感词过滤
            if (message.getContent() != null) {
                // 检查是否包含敏感词
                if (sensitiveWordFilter.containsSensitiveWord(message.getContent())) {
                    log.warn("消息包含敏感词，已被过滤，用户ID: {}, 内容: {}",
                            message.getUserId(), message.getContent());
                    // 可以选择拒绝发送或替换敏感词
                    message.setContent(sensitiveWordFilter.filterSensitiveWords(message.getContent()));
                }

                // 内容长度检查
                if (message.getContent().length() > 25) {
                    message.setContent(message.getContent().substring(0, 25));
                }
            }

            // 设置消息时间
            message.setTimestamp(LocalDateTime.now());
            log.info("发送聊天消息: roomId={}, userId={}, content={}",
                    roomId, message.getUserId(), message.getContent());

            // 发送消息到订阅该直播间的所有客户端
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);

        } catch (Exception e) {
            log.error("发送聊天消息异常，roomId: {}, userId: {}", roomId, message.getUserId(), e);
        }
    }


    @Data
    public static class ChatMessage {
        private String username;
        private Long userId;
        @Size(max = 25, message = "内容长度不能超过25个字符")
        private String content;
        private LocalDateTime timestamp;
        private Integer type;
    }
}