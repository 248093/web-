package top.lyh.controller;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.SrsCallbackDto;
import top.lyh.service.LiveStreamService;

import java.nio.charset.StandardCharsets;
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
    public ResultDTO onPublish(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_publish回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());

        String param = callbackDto.getParam();
        Map<String, String> paramMap = HttpUtil.decodeParamMap(param, StandardCharsets.UTF_8);
        String token = paramMap.get("auth_key");
        String expire = paramMap.get("expire");
        callbackDto.setToken(token);
        callbackDto.setExpire(expire);

        // 验证推流密钥
        boolean valid = liveStreamService.validateStreamKey(
                callbackDto.getStream(),
                callbackDto.getToken(),
                callbackDto.getExpire());

        if (!valid) {
            log.warn("推流密钥验证失败: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());
            return ResultDTO.error(ResponseCodeEnum.FORBIDDEN, "Forbidden");
        }

        // 处理流发布事件
        liveStreamService.handleStreamPublish(callbackDto.getApp(), callbackDto.getStream());

        return ResultDTO.success("Success");
    }

    /**
     * 处理SRS on_unpublish回调
     * 当推流结束时，SRS会调用此接口
     */
    @PostMapping("/on_unpublish")
    public ResultDTO onUnpublish(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_unpublish回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());

        // 处理流关闭事件
        liveStreamService.handleStreamClose(callbackDto.getApp(), callbackDto.getStream());

        return ResultDTO.success("Success");
    }

    /**
     * 处理SRS on_play回调
     * 当播放流开始时，SRS会调用此接口
     */
    @PostMapping("/on_play")
    public ResultDTO onPlay(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_play回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());

        return ResultDTO.success("Success");
    }

    /**
     * 处理SRS on_stop回调
     * 当播放流结束时，SRS会调用此接口
     */
    @PostMapping("/on_stop")
    public ResultDTO onStop(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_stop回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());

        return ResultDTO.success("Success");
    }

    /**
     * 处理SRS on_dvr回调
     * 当DVR录制文件关闭时，SRS会调用此接口
     */
    @PostMapping("/on_dvr")
    public ResultDTO onDvr(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_dvr回调: app={}, stream={}, file={}",
                callbackDto.getApp(), callbackDto.getStream(), callbackDto.getFile());

        return ResultDTO.success("Success");
    }
}
