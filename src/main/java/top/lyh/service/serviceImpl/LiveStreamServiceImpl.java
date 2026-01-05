package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import top.lyh.entity.pojo.LiveRecording;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.entity.pojo.LiveStream;
import top.lyh.mapper.LiveRecordingMapper;
import top.lyh.mapper.LiveRoomMapper;
import top.lyh.mapper.LiveStreamMapper;
import top.lyh.service.LiveRecordingService;
import top.lyh.service.LiveRoomService;
import top.lyh.service.LiveStreamService;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class LiveStreamServiceImpl implements LiveStreamService {

    @Autowired
    private LiveRoomService liveRoomService;

    @Autowired
    private LiveStreamMapper liveStreamMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LiveRecordingService liveRecordingService;

    @Autowired
    private LiveRecordingMapper liveRecordingMapper;

    @Autowired
    private LiveRoomMapper liveRoomMapper;

    @Value("${live.srs.server-url}")
    private String srsServerUrl;

    @Value("${live.srs.api-url}")
    private String srsApiUrl;
    @Value("${live.push.auth-key}")
    private String authKey;
    @Value("${live.push.key-check-enabled}")
    private boolean keyCheckEnabled;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * 开始直播
     */
    @Override
    @Transactional
    public LiveRoom startLiveStream(Long roomId) {
        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        if (liveRoom == null) {
            throw new IllegalArgumentException("直播间不存在");
        }

        // 更新直播间状态为直播中
        liveRoom.setStatus(1);
        liveRoom.setStartTime(LocalDateTime.now());
        liveRoomMapper.updateById(liveRoom);
        QueryWrapper<LiveStream> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("stream_id", liveRoom.getStreamKey());
        LiveStream liveStream1 = liveStreamMapper.selectOne(queryWrapper);
        LiveStream liveStream = new LiveStream();
        if (liveStream1 == null){
            // 创建直播流记录
            liveStream.setRoomId(roomId);
            liveStream.setStreamId(liveRoom.getStreamKey());
            liveStream.setProtocol("rtmp");
            liveStream.setStatus(1);
            liveStream.setCreatedAt(LocalDateTime.now());
            liveStream.setUpdatedAt(LocalDateTime.now());
            liveStreamMapper.insert(liveStream);
        }else {
            liveStream.setStatus(1);
            liveStream.setUpdatedAt(LocalDateTime.now());
            liveStreamMapper.updateById(liveStream);
        }

        // 更新Redis缓存中的活跃直播间
        redisTemplate.opsForSet().add("live:active_rooms", String.valueOf(roomId));

        return liveRoom;
    }
    /**
     * 结束直播
     */

    /**
     * 结束直播
     */
    @Override
    @Transactional
    public LiveRoom endLiveStream(Long roomId) {
        LiveRoom liveRoom = liveRoomService.getById(roomId);
        if (liveRoom == null || liveRoom.getStatus() != 1) {
            throw new IllegalArgumentException("直播间不存在或未开播");
        }

        // 更新直播间状态为已结束
        liveRoom.setStatus(2);
        liveRoom.setEndTime(LocalDateTime.now());
        liveRoomMapper.updateById(liveRoom);

        // 更新直播流状态
        QueryWrapper<LiveStream> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId).eq("status", 1);

        LiveStream liveStream = liveStreamMapper.selectOne(queryWrapper);
        if (liveStream != null) {
            liveStream.setStatus(2);
            liveStream.setUpdatedAt(LocalDateTime.now());
            liveStreamMapper.updateById(liveStream);
        }

        // 从Redis中移除活跃直播间
        redisTemplate.opsForSet().remove("live:active_rooms", String.valueOf(roomId));

        return liveRoom;
    }
    /**
     * 校验推流密钥
     */
    @Override
    public boolean validateStreamKey(String streamKey, String token, String expire) {
        if (!keyCheckEnabled) {
            return true;
        }

        try {
            long expireTimestamp = Long.parseLong(expire);
            long currentTime = System.currentTimeMillis() / 1000;

            // 检查是否过期
            if (currentTime > expireTimestamp) {
                return false;
            }

            // 验证token
            String authString = streamKey + "-" + expire + "-" + authKey;
            String calculatedToken = DigestUtils.md5DigestAsHex(authString.getBytes());

            return calculatedToken.equals(token);

        } catch (Exception e) {
            log.error("验证推流密钥异常", e);
            return false;
        }
    }


    /**
     * 处理SRS回调 - 流发布
     */
    @Override
    public void handleStreamPublish(String app, String stream) {
        log.info("处理流发布回调: app={}, stream={}", app, stream);
        try {
            // 查找对应的直播间
            var liveRoom = liveRoomService.findByStreamKey(stream);
            if (liveRoom != null && liveRoom.getStatus() != 1) {
                // 开始直播
                LiveRoom started = startLiveStream(liveRoom.getId());
                if (!ObjectUtils.isEmpty(started)) {
                    // 新增：自动开始录制
                    LiveRecording recording = liveRecordingService.startRecording(liveRoom.getId());
                    log.info("直播流发布成功: app={}, stream={}, roomId={}", app, stream, liveRoom.getId());
                }
            }
        } catch (Exception e) {
            log.error("处理流发布回调异常", e);
        }
    }

    /**
     * 处理SRS回调 - 流关闭
     */
    @Override
    public void handleStreamClose(String app, String stream) {
        try {
            // 查找对应的直播间
            var liveRoom = liveRoomService.findByStreamKey(stream);
            if (liveRoom != null && liveRoom.getStatus() == 1) {
                // 结束直播
                LiveRoom ended = endLiveStream(liveRoom.getId());
                if (ObjectUtils.isEmpty(ended)) {
                    // 停止录制
                    QueryWrapper<LiveRecording> recordingQuery = new QueryWrapper<>();
                    recordingQuery.eq("room_id", liveRoom.getId())
                            .eq("status", 0); // 假设 0 表示"录制中"
                    LiveRecording recording = liveRecordingMapper.selectOne(recordingQuery);

                    if (recording != null) {
                        liveRecordingService.stopRecording(recording.getId());
                        log.info("推流结束，自动停止录制: roomId={}, recordingId={}",
                                liveRoom.getId(), recording.getId());
                    }

                    log.info("直播流关闭: app={}, stream={}, roomId={}", app, stream, liveRoom.getId());
                }
            }
        } catch (Exception e) {
            log.error("处理流关闭回调异常", e);
        }
    }

    /**
     * 获取SRS服务器信息
     */
    @Override
    public Map<String, Object> getSrsServerInfo() {
        try {
            String url = srsApiUrl + "/v1/summaries";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("获取SRS服务器信息异常", e);
            return Map.of();
        }
    }
}