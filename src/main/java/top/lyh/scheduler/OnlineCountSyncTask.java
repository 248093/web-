package top.lyh.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.mapper.LiveRoomMapper;
import top.lyh.service.LiveRoomService;
import top.lyh.utils.RedisUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OnlineCountSyncTask {

    @Autowired
    private RedisUtil redisTemplate;

    @Autowired
    private LiveRoomMapper liveRoomMapper;

    @Autowired
    private LiveRoomService liveRoomService; // 注入Service用于批量操作

    // 定时任务：每3分钟执行一次
    @Scheduled(fixedRate = 18000)
    public void syncOnlineCountToDatabase() {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int zeroCount = 0;

        try {
            // 1. 获取所有直播间ID
            LambdaQueryWrapper<LiveRoom> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(LiveRoom::getId);
            List<Long> roomIds = liveRoomMapper.selectList(queryWrapper).stream()
                    .map(LiveRoom::getId)
                    .collect(Collectors.toList());

            if (roomIds.isEmpty()) {
                log.info("没有直播间需要同步");
                return;
            }

            log.info("开始同步 {} 个直播间的在线人数", roomIds.size());

            // 2. 构建批量更新数据
            List<LiveRoom> updateList = new ArrayList<>();

            for (Long roomId : roomIds) {
                String key = "live:room:" + roomId + ":online_count";
                Object countObj = redisTemplate.get(key);

                long onlineCount = 0;
                if (countObj instanceof Integer) {
                    onlineCount = ((Integer) countObj).longValue();
                } else if (countObj instanceof Long) {
                    onlineCount = (Long) countObj;
                } else if (countObj instanceof String) {
                    onlineCount = Long.parseLong((String) countObj);
                }

                // 创建要更新的实体对象
                LiveRoom room = new LiveRoom();
                room.setId(roomId);
                room.setViewCount(Long.valueOf(onlineCount)); // 根据你的实体类类型转换

                updateList.add(room);

                if (onlineCount == 0) {
                    zeroCount++;
                } else {
                    successCount++;
                }
            }

            // 3. 使用MyBatis-Plus的批量更新功能
            if (!updateList.isEmpty()) {
                // saveOrUpdateBatch 会自动判断是插入还是更新
                // 这里因为是更新已存在的记录，所以会执行更新操作
                boolean result = liveRoomService.saveOrUpdateBatch(updateList);

                if (result) {
                    log.info("批量更新成功：总房间 {} 个，其中非零在线 {} 个，零在线 {} 个，耗时：{}ms",
                            roomIds.size(), successCount, zeroCount,
                            System.currentTimeMillis() - startTime);
                } else {
                    log.error("批量更新失败");
                }
            }

        } catch (Exception e) {
            log.error("定时任务同步在线人数失败，耗时：{}ms",
                    System.currentTimeMillis() - startTime, e);
        }
    }
}