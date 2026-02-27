package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import top.lyh.common.PageResult;
import top.lyh.entity.dto.LiveRoomQueryDto;
import top.lyh.entity.vo.LiveRoomDetailVo;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.mapper.LiveRoomMapper;
import top.lyh.service.LiveRoomService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
public class LiveRoomServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveRoomService {
    
    @Autowired
    private LiveRoomMapper liveRoomMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Value("${live.srs.http-flv-url}")
    private String httpFlvUrl;
    
    @Value("${live.srs.hls-url}")
    private String hlsUrl;
    @Value("${live.srs.server-url}")
    private String srsServerUrl;

    @Value("${live.push.key-check-enabled}")
    private boolean keyCheckEnabled;

//    @Value("${live.push.auth-expire}")
//    private long authExpire;

    @Value("${live.push.auth-key}")
    private String authKey;
    
    /**
     * 创建直播间
     */
    @Override
    @Transactional
    public LiveRoom createLiveRoom(LiveRoom liveRoom) {
        LambdaQueryWrapper<LiveRoom> queryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<LiveRoom> liveRoomLambdaQueryWrapper = queryWrapper.eq(LiveRoom::getUserId, liveRoom.getUserId());
        List<LiveRoom> liveRoom1 = liveRoomMapper.selectList(liveRoomLambdaQueryWrapper);
        if (liveRoom1.size() > 0) {
            throw new RuntimeException("当前用户已创建直播间");
        }
        // 生成推流密钥
        String streamKey = generateStreamKey(liveRoom.getUserId());
        liveRoom.setStreamKey(streamKey);
        
        // 构建播放地址（推流地址由LiveStreamService处理）
        liveRoom.setHlsUrl(hlsUrl + "/" + streamKey + ".m3u8");
        liveRoom.setFlvUrl(httpFlvUrl + "/" + streamKey + ".flv");
        liveRoom.setStreamUrl(srsServerUrl + "/" + streamKey);
        // 设置初始状态
        liveRoom.setStatus(0); // 未开播
        liveRoom.setViewCount(0L);
        liveRoom.setAmountCount(BigDecimal.valueOf(0.0));
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
    public PageResult<LiveRoomDetailVo> getActiveLiveRooms(LiveRoomQueryDto query) {
        try {
            // 计算分页参数
            int offset = (query.getPage() - 1) * query.getSize();
            List<LiveRoomDetailVo> records = liveRoomMapper.selectByCondition(query, query.getSize(), offset);
            Long total  = liveRoomMapper.countByCondition(query);
            // 调用Mapper查询
            return new PageResult<>(records,total,query.getPage(),query.getSize());
        } catch (Exception e) {
            log.error("查询活跃直播间失败", e);
            throw new RuntimeException("查询直播间列表失败", e);
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
            liveRoom.setUpdatedAt(LocalDateTime.now());
            
            int result = liveRoomMapper.updateById(liveRoom);
            return result > 0;
        } catch (Exception e) {
            log.error("删除直播间失败，roomId: {}", roomId, e);
            throw new RuntimeException("删除直播间失败", e);
        }
    }

}