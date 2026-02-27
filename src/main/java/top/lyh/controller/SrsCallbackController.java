package top.lyh.controller;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.lyh.entity.dto.SrsCallbackDto;
import top.lyh.service.LiveStreamService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/srs/callback")
@Slf4j
public class SrsCallbackController {

    @Autowired
    private LiveStreamService liveStreamService;

    /**
     * 处理SRS on_publish回调
     * 当推流开始时，SRS会调用此接口
     */
    @PostMapping("/on_publish")
    public Map<String, Object> onPublish(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_publish回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());

        Map<String, Object> response = new HashMap<>();

        try {
            String param = callbackDto.getParam();
            Map<String, String> paramMap = HttpUtil.decodeParamMap(param, StandardCharsets.UTF_8);
            String token = paramMap.get("auth_key");
            String expire = paramMap.get("expire");

            // 验证推流密钥
            boolean valid = liveStreamService.validateStreamKey(
                    callbackDto.getStream(),
                    token,
                    expire);

            if (!valid) {
                log.warn("推流密钥验证失败: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());
                response.put("code", -1);
                response.put("message", "Forbidden");
                return response;
            }

            // 处理流发布事件
            liveStreamService.handleStreamPublish(callbackDto.getApp(), callbackDto.getStream());

            // SRS期望的成功返回格式
            response.put("code", 0);
            response.put("message", "success");

        } catch (Exception e) {
            log.error("处理on_publish回调异常", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }

        return response;
    }

    /**
     * 处理SRS on_unpublish回调
     * 当推流结束时，SRS会调用此接口
     */
    @PostMapping("/on_unpublish")
    public Map<String, Object> onUnpublish(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_unpublish回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());

        Map<String, Object> response = new HashMap<>();

        try {
            // 处理流关闭事件
            liveStreamService.handleStreamClose(callbackDto.getApp(), callbackDto.getStream());

            response.put("code", 0);
            response.put("message", "success");
        } catch (Exception e) {
            log.error("处理on_unpublish回调异常", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }

        return response;
    }

    /**
     * 处理SRS on_play回调
     * 当播放流开始时，SRS会调用此接口
     */
    @PostMapping("/on_play")
    public Map<String, Object> onPlay(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_play回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());

        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        return response;
    }

    /**
     * 处理SRS on_stop回调
     * 当播放流结束时，SRS会调用此接口
     */
    @PostMapping("/on_stop")
    public Map<String, Object> onStop(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_stop回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());

        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        return response;
    }

    /**
     * 处理SRS on_dvr回调
     * 当DVR录制文件关闭时，SRS会调用此接口
     */
    @PostMapping("/on_dvr")
    public Map<String, Object> onDvr(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_dvr回调: app={}, stream={}, file={}",
                callbackDto.getApp(), callbackDto.getStream(), callbackDto.getFile());

        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        return response;
    }
}