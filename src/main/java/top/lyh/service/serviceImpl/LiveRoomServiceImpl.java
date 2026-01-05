package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import top.lyh.entity.dto.LiveRoomQueryDto;
import top.lyh.entity.vo.LiveRoomDetailVo;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.mapper.LiveRoomMapper;
import top.lyh.service.LiveRoomService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class LiveRoomServiceImpl implements LiveRoomService {
    
    @Autowired
    private LiveRoomMapper liveRoomMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Value("${live.srs.http-flv-url}")
    private String httpFlvUrl;
    
    @Value("${live.srs.hls-url}")
    private String hlsUrl;
    
    @Value("${live.push.key-check-enabled}")
    private boolean keyCheckEnabled;
    
    @Value("${live.push.auth-expire}")
    private long authExpire;
    
    @Value("${live.push.auth-key}")
    private String authKey;
    
    /**
     * 创建直播间
     */
    @Override
    @Transactional
    public LiveRoom createLiveRoom(LiveRoom liveRoom) {
        // 生成推流密钥
        String streamKey = generateStreamKey(liveRoom.getUserId());
        liveRoom.setStreamKey(streamKey);
        
        // 构建播放地址（推流地址由LiveStreamService处理）
        liveRoom.setHlsUrl(hlsUrl + "/" + streamKey + ".m3u8");
        liveRoom.setFlvUrl(httpFlvUrl + "/" + streamKey + ".flv");
        
        // 设置初始状态
        liveRoom.setStatus(0); // 未开播
        liveRoom.setViewCount(0L);
        liveRoom.setLikeCount(0L);
        liveRoom.setCreatedAt(LocalDateTime.now());
        liveRoom.setUpdatedAt(LocalDateTime.now());
        
        // 保存到数据库
        liveRoomMapper.insert(liveRoom);
        
        log.info("创建直播间成功，roomId: {}, userId: {}", liveRoom.getId(), liveRoom.getUserId());
        return liveRoom;
    }
    
    /**
     * 生成推流密钥
     */
    private String generateStreamKey(Long userId) {
        // 生成基于用户ID和时间戳的唯一密钥
        String baseKey = userId + "_" + System.currentTimeMillis();
        return DigestUtils.md5DigestAsHex(baseKey.getBytes());
    }

    /**
     * 根据ID获取直播间
     */
    @Override
    public LiveRoom getById(Long roomId) {
        return liveRoomMapper.selectById(roomId);
    }
    
    /**
     * 分页查询直播间列表
     */
    @Override
    public List<LiveRoomDetailVo> getActiveLiveRooms(LiveRoomQueryDto query) {
        try {
            // 计算分页参数
            int offset = (query.getPage() - 1) * query.getSize();
            
            // 调用Mapper查询
            return liveRoomMapper.selectByCondition(query, query.getSize(), offset);
        } catch (Exception e) {
            log.error("查询活跃直播间失败", e);
            throw new RuntimeException("查询直播间列表失败", e);
        }
    }
    
    /**
     * 获取热门直播间（使用综合评分排序）
     */
    @Override
    public List<LiveRoom> getHotLiveRooms(int limit) {
        try {
            QueryWrapper<LiveRoom> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", 1) // 直播中
                    .orderByDesc("view_count") // 按观看人数排序
                    .last("LIMIT " + limit);
            
            return liveRoomMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("获取热门直播间失败", e);
            throw new RuntimeException("获取热门直播间失败", e);
        }
    }
    
    /**
     * 更新直播间状态
     */
    @Override
    @Transactional
    public boolean updateStatus(Long roomId, Integer status) {
        try {
            LiveRoom liveRoom = new LiveRoom();
            liveRoom.setId(roomId);
            liveRoom.setStatus(status);
            liveRoom.setUpdatedAt(LocalDateTime.now());
            
            // 如果是开始直播，设置开始时间
            if (status == 1) {
                liveRoom.setStartTime(LocalDateTime.now());
            }
            // 如果是结束直播，设置结束时间
            else if (status == 2) {
                liveRoom.setEndTime(LocalDateTime.now());
            }
            
            int result = liveRoomMapper.updateById(liveRoom);
            return result > 0;
        } catch (Exception e) {
            log.error("更新直播间状态失败，roomId: {}, status: {}", roomId, status, e);
            throw new RuntimeException("更新直播间状态失败", e);
        }
    }
    
    /**
     * 增加直播间观看人数
     */
    @Override
    @Transactional
    public void incrementViewCount(Long roomId) {
        try {
            // 使用Redis进行计数
            String key = "live:room:" + roomId + ":view_count";
            redisTemplate.opsForValue().increment(key);
            
            // 定期同步到数据库（减少数据库压力）
            if (Math.random() < 0.1) {  // 10%概率同步
                String countStr = redisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    long count = Long.parseLong(countStr);
                    
                    LiveRoom room = new LiveRoom();
                    room.setId(roomId);
                    room.setViewCount(count);
                    room.setUpdatedAt(LocalDateTime.now());
                    
                    liveRoomMapper.updateById(room);
                    
                    log.debug("同步观看人数到数据库，roomId: {}, count: {}", roomId, count);
                }
            }
        } catch (Exception e) {
            log.error("增加观看人数失败，roomId: {}", roomId, e);
        }
    }
    
    /**
     * 增加直播间点赞数
     */
    @Override
    @Transactional
    public void incrementLikeCount(Long roomId) {
        try {
            // 使用Redis进行计数
            String key = "live:room:" + roomId + ":like_count";
            redisTemplate.opsForValue().increment(key);
            
            // 定期同步到数据库
            if (Math.random() < 0.2) {  // 20%概率同步（点赞比观看频率低）
                String countStr = redisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    long count = Long.parseLong(countStr);
                    
                    LiveRoom room = new LiveRoom();
                    room.setId(roomId);
                    room.setLikeCount(count);
                    room.setUpdatedAt(LocalDateTime.now());
                    
                    liveRoomMapper.updateById(room);
                    
                    log.debug("同步点赞数到数据库，roomId: {}, count: {}", roomId, count);
                }
            }
        } catch (Exception e) {
            log.error("增加点赞数失败，roomId: {}", roomId, e);
        }
    }
    
    /**
     * 获取直播间详情VO
     */
    @Override
    public LiveRoomDetailVo getLiveRoomDetail(Long roomId) {
        try {
            LiveRoomQueryDto query = new LiveRoomQueryDto();
            query.setId(roomId);
            
            List<LiveRoomDetailVo> result = liveRoomMapper.selectByCondition(query, 10, 0);
            
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            log.error("获取直播间详情失败，roomId: {}", roomId, e);
            throw new RuntimeException("获取直播间详情失败", e);
        }
    }
    
    /**
     * 根据streamKey查找直播间
     */
    public LiveRoom findByStreamKey(String streamKey) {
        try {
            QueryWrapper<LiveRoom> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("stream_key", streamKey);
            
            return liveRoomMapper.selectOne(queryWrapper);
        } catch (Exception e) {
            log.error("根据streamKey查找直播间失败，streamKey: {}", streamKey, e);
            return null;
        }
    }
    
    /**
     * 更新直播间信息
     */
    @Transactional
    public boolean updateLiveRoom(LiveRoom liveRoom) {
        try {
            liveRoom.setUpdatedAt(LocalDateTime.now());
            int result = liveRoomMapper.updateById(liveRoom);
            return result > 0;
        } catch (Exception e) {
            log.error("更新直播间信息失败，roomId: {}", liveRoom.getId(), e);
            throw new RuntimeException("更新直播间信息失败", e);
        }
    }
    
    /**
     * 批量获取直播间
     */
    public List<LiveRoom> batchGetByIds(List<Long> roomIds) {
        try {
            QueryWrapper<LiveRoom> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", roomIds);
            
            return liveRoomMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("批量获取直播间失败，roomIds: {}", roomIds, e);
            throw new RuntimeException("批量获取直播间失败", e);
        }
    }
    
    /**
     * 删除直播间（软删除）
     */
    @Transactional
    public boolean deleteLiveRoom(Long roomId) {
        try {
            LiveRoom liveRoom = new LiveRoom();
            liveRoom.setId(roomId);
            liveRoom.setStatus(3); // 假设3表示已删除
            liveRoom.setUpdatedAt(LocalDateTime.now());
            
            int result = liveRoomMapper.updateById(liveRoom);
            return result > 0;
        } catch (Exception e) {
            log.error("删除直播间失败，roomId: {}", roomId, e);
            throw new RuntimeException("删除直播间失败", e);
        }
    }
}