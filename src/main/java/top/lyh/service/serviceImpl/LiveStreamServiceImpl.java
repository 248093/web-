package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
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
import top.lyh.entity.pojo.SysUser;
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
public class LiveStreamServiceImpl extends ServiceImpl<LiveStreamMapper, LiveStream> implements LiveStreamService{

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

        // 修复：先查询是否存在相同stream_id的记录，避免重复插入
        QueryWrapper<LiveStream> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("stream_id", liveRoom.getStreamKey());
        LiveStream existingStream = liveStreamMapper.selectOne(queryWrapper);

        if (existingStream == null) {
            // 只有不存在时才创建新记录
            LiveStream liveStream = new LiveStream();
            liveStream.setRoomId(roomId);
            liveStream.setStreamId(liveRoom.getStreamKey());
            liveStream.setProtocol("rtmp");
            liveStream.setStatus(1);
            liveStream.setCreatedAt(LocalDateTime.now());
            liveStream.setUpdatedAt(LocalDateTime.now());
            liveStreamMapper.insert(liveStream);
        } else {
            // 如果已存在，只更新状态
            existingStream.setStatus(1);
            existingStream.setUpdatedAt(LocalDateTime.now());
            liveStreamMapper.updateById(existingStream);
        }

        // 更新Redis缓存中的活跃直播间
        redisTemplate.opsForSet().add("live:active_rooms", String.valueOf(roomId));

        return liveRoom;
    }

    /**
     * 结束直播
     */
    @Override
    @Transactional
    public LiveRoom endLiveStream(Long roomId) {
        // 参数校验
        if (roomId == null) {
            throw new IllegalArgumentException("直播间ID不能为空");
        }

        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        if (liveRoom == null) {
            log.warn("结束直播失败：直播间不存在，roomId={}", roomId);
            throw new IllegalArgumentException("直播间不存在");
        }

        // 检查直播间状态
        if (liveRoom.getStatus() != 1) {
            log.warn("结束直播失败：直播间状态异常，当前状态={}，roomId={}", liveRoom.getStatus(), roomId);
            throw new IllegalArgumentException("直播间未处于直播状态");
        }

        try {
            // 更新直播间状态为已结束
            liveRoom.setStatus(0); // 0表示已结束
            liveRoom.setEndTime(LocalDateTime.now());
            liveRoomMapper.updateById(liveRoom);
            log.info("直播间状态已更新为结束：roomId={}", roomId);

            // 更新直播流状态
            QueryWrapper<LiveStream> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("room_id", roomId).eq("status", 1);

            LiveStream liveStream = liveStreamMapper.selectOne(queryWrapper);
            if (liveStream != null) {
                liveStream.setStatus(0); // 0表示已结束
                liveStream.setUpdatedAt(LocalDateTime.now());
                liveStreamMapper.updateById(liveStream);
                log.info("直播流状态已更新为结束：streamId={}", liveStream.getStreamId());
            } else {
                log.warn("未找到对应的直播流记录：roomId={}", roomId);
            }

            // 从Redis中移除活跃直播间
            redisTemplate.opsForSet().remove("live:active_rooms", String.valueOf(roomId));
            log.info("已从活跃直播间列表中移除：roomId={}", roomId);

            log.info("直播结束处理完成：roomId={}", roomId);
            return liveRoom;

        } catch (Exception e) {
            log.error("结束直播过程发生异常：roomId={}", roomId, e);
            throw new RuntimeException("结束直播失败：" + e.getMessage());
        }
    }

    /**
     * 校验推流密钥
     */
    @Override
    public boolean validateStreamKey(String streamKey, String token, String expire) {
        if (!keyCheckEnabled) {
            log.info("推流密钥验证已关闭，允许所有推流");
            return true;
        }

        try {
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
            LiveRoom liveRoom = liveRoomService.findByStreamKey(stream);

            if (liveRoom != null && liveRoom.getStatus() != 1) {
                // 开始直播
                LiveRoom started = startLiveStream(liveRoom.getId());
                if (!ObjectUtils.isEmpty(started)) {  // 修复：正确判断返回值
                    try {
                        // 新增：自动开始录制（添加异常处理）
                        LiveRecording recording = liveRecordingService.startRecording(liveRoom.getId());
                        log.info("直播流发布成功并开始录制: app={}, stream={}, roomId={}, recordingId={}",
                                app, stream, liveRoom.getId(), recording.getId());
                    } catch (Exception e) {
                        log.error("自动开始录制失败，但直播正常开始: roomId={}", liveRoom.getId(), e);
                        // 录制失败不应该影响直播流程
                    }
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
            LiveRoom liveRoom = liveRoomService.findByStreamKey(stream);
            if (!ObjectUtils.isEmpty(liveRoom) && liveRoom.getStatus() == 1) {
                log.info("处理流关闭回调: app={}, stream={}", app, stream);
                // 结束直播
                LiveRoom ended = endLiveStream(liveRoom.getId());
                if (!ObjectUtils.isEmpty(ended)) {  // 修复：正确判断返回值
                    // 停止录制
                    QueryWrapper<LiveRecording> recordingQuery = new QueryWrapper<>();
                    recordingQuery.eq("room_id", liveRoom.getId())
                            .eq("status", 0); // 0表示"录制中"
                    LiveRecording recording = liveRecordingMapper.selectOne(recordingQuery);

                    if (recording != null) {
                        try {
                            liveRecordingService.stopRecording(recording.getId());
                            log.info("推流结束，自动停止录制: roomId={}, recordingId={}",
                                    liveRoom.getId(), recording.getId());
                        } catch (Exception e) {
                            log.error("停止录制失败: roomId={}, recordingId={}",
                                    liveRoom.getId(), recording.getId(), e);
                        }
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