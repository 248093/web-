package top.lyh.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.PageResult;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.LiveRoomQueryDto;
import top.lyh.entity.pojo.LiveRecording;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.entity.pojo.SysUser;
import top.lyh.entity.vo.LiveRoomDetailVo;
import top.lyh.service.LiveRecordingService;
import top.lyh.service.LiveRoomService;
import top.lyh.service.LiveStreamService;
import top.lyh.service.SysUserService;
import top.lyh.utils.RedisUtil;

import java.util.List;

@RestController
@RequestMapping("/api/live")
@Slf4j
public class LiveController {

    @Autowired
    private LiveStreamService liveStreamService;

    @Autowired
    private LiveRecordingService recordingService;

    @Autowired
    private LiveRoomService liveRoomService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private RedisUtil redisTemplate;

    /**
     * 创建直播间
     */
    @RequiresRoles(value = {"ADMIN", "HOST"}, logical = Logical.OR)
    @PostMapping("/room")
    public ResultDTO createLiveRoom(@RequestBody LiveRoom liveRoom) {
        try {
            LiveRoom createdRoom = liveRoomService.createLiveRoom(liveRoom);
            return ResultDTO.success("创建直播间成功", createdRoom);
        } catch (Exception e) {
            log.error("创建直播间异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "创建直播间异常");
        }
    }

    /**
     * 获取直播间详情
     */
    @GetMapping("/room/{roomId}")
    public ResultDTO getLiveRoom(@PathVariable Long roomId) {
        try {
            LiveRoom liveRoom = liveRoomService.getById(roomId);
            if (liveRoom == null) {
                return ResultDTO.error(ResponseCodeEnum.NOT_FOUND, "直播间不存在");
            }
            SysUser host = sysUserService.getById(liveRoom.getUserId());
            LiveRoomDetailVo liveRoomDetailVo = new LiveRoomDetailVo();
            liveRoomDetailVo.setAnchorAvatar(host.getAvatar());
            BeanUtils.copyProperties(liveRoom, liveRoomDetailVo);
            return ResultDTO.success("获取直播间详情成功", liveRoomDetailVo);
        } catch (Exception e) {
            log.error("获取直播间详情异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "获取直播间详情异常");
        }
    }

    /**
     * 开始直播
     */
    @RequiresRoles(value = {"ADMIN", "HOST"}, logical = Logical.OR)
    @PostMapping("/room/{roomId}/start")
    public ResultDTO startLiveStream(@PathVariable Long roomId) {
        log.info("直播间Id: {}", roomId);
        try {
            LiveRoom liveRoom = liveStreamService.startLiveStream(roomId);
            return ResultDTO.success("开始直播成功", liveRoom);
        } catch (IllegalArgumentException e) {
            return ResultDTO.error(ResponseCodeEnum.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("开始直播异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "开始直播异常");
        }
    }

    /**
     * 结束直播
     */
    @RequiresRoles(value = {"ADMIN", "HOST"}, logical = Logical.OR)
    @PostMapping("/room/{roomId}/end")
    public ResultDTO endLiveStream(@PathVariable Long roomId) {
        try {
            LiveRoom liveRoom = liveStreamService.endLiveStream(roomId);
            return ResultDTO.success("结束直播成功", liveRoom);
        } catch (IllegalArgumentException e) {
            return ResultDTO.error(ResponseCodeEnum.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("结束直播异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "结束直播异常");
        }
    }

    /**
     * GET分页方式查询（参数通过URL拼接，不是标准JSON）
     */
    @PostMapping("/rooms")
    public ResultDTO getLiveRooms(@RequestBody LiveRoomQueryDto query) {
        try {
            if (query.getPage() == null) query.setPage(1);
            if (query.getSize() == null) query.setSize(12);

            PageResult<LiveRoomDetailVo> rooms = liveRoomService.getActiveLiveRooms(query);
            return ResultDTO.success("获取直播间列表成功", rooms);
        } catch (Exception e) {
            log.error("获取直播间列表异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "获取直播间列表异常");
        }
    }

    /**
     * 增加直播间在线人数
     */
    @PostMapping("/room/{roomId}/online/increment")
    public ResultDTO incrementOnlineCount(@PathVariable Long roomId) {
        try {
            String key = "live:room:" + roomId + ":online_count";
            redisTemplate.incr( key, 1);
            return ResultDTO.success("在线人数增加成功");
        } catch (Exception e) {
            log.error("增加在线人数失败，roomId: {}", roomId, e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "增加在线人数失败");
        }
    }

    /**
     * 减少直播间在线人数
     */
    @PostMapping("/room/{roomId}/online/decrement")
    public ResultDTO decrementOnlineCount(@PathVariable Long roomId) {
        log.info("直播间Id: {}", roomId);
        try {
            String key = "live:room:" + roomId + ":online_count";
            Object currentCount = redisTemplate.get(key);
            // 一行搞定：任何类型都转成字符串再解析为Long
            long count = currentCount != null ? Long.parseLong(currentCount.toString()) : 0L;

            if (count > 0) {
                redisTemplate.decr(key, 1);
            }

            return ResultDTO.success("在线人数减少成功");
        } catch (Exception e) {
            log.error("减少在线人数失败，roomId: {}", roomId, e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "减少在线人数失败");
        }
    }
    /**
     * 获取直播间在线人数
     */
    @GetMapping("/room/{roomId}/online/count")
    public ResultDTO getOnlineCount(@PathVariable Long roomId) {
        try {
            String key = "live:room:" + roomId + ":online_count";
            Object value = redisTemplate.get(key);
            long count = value != null ? Long.parseLong(value.toString()) : 0L;
            return ResultDTO.success("获取在线人数成功", count);
        } catch (Exception e) {
            log.error("获取在线人数失败，roomId: {}", roomId, e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "获取在线人数失败");
        }
    }



    /**
     * 开始录制直播
     */
    @PostMapping("/room/{roomId}/record/start")
    public ResultDTO startRecording(@PathVariable Long roomId) {
        try {
            LiveRecording recording = recordingService.startRecording(roomId);
            return ResultDTO.success("开始录制成功", recording);
        } catch (IllegalArgumentException e) {
            return ResultDTO.error(ResponseCodeEnum.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("开始录制直播异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "开始录制直播异常");
        }
    }

    /**
     * 停止录制直播
     */
    @PostMapping("/record/{recordingId}/stop")
    public ResultDTO stopRecording(@PathVariable Long recordingId) {
        try {
            LiveRecording recording = recordingService.stopRecording(recordingId);
            return ResultDTO.success("停止录制成功", recording);
        } catch (IllegalArgumentException e) {
            return ResultDTO.error(ResponseCodeEnum.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("停止录制直播异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "停止录制直播异常");
        }
    }

    /**
     * 获取直播回放列表
     */
    @GetMapping("/room/{roomId}/recordings")
    public ResultDTO getRecordings(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<LiveRecording> recordings = recordingService.getRecordings(roomId, page, size);
            return ResultDTO.success("获取直播回放列表成功", recordings);
        } catch (Exception e) {
            log.error("获取直播回放列表异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "获取直播回放列表异常");
        }
    }
}
