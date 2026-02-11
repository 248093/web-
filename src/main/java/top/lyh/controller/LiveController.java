package top.lyh.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.LiveRoomQueryDto;
import top.lyh.entity.pojo.LiveRecording;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.entity.vo.LiveRoomDetailVo;
import top.lyh.service.LiveRecordingService;
import top.lyh.service.LiveRoomService;
import top.lyh.service.LiveStreamService;

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
            return ResultDTO.success("获取直播间详情成功", liveRoom);
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
            if (query.getSize() == null) query.setSize(10);

            List<LiveRoomDetailVo> rooms = liveRoomService.getActiveLiveRooms(query);
            return ResultDTO.success("获取直播间列表成功", rooms);
        } catch (Exception e) {
            log.error("获取直播间列表异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "获取直播间列表异常");
        }
    }

    /**
     * 获取热门直播间
     */
    @GetMapping("/rooms/hot")
    public ResultDTO getHotLiveRooms(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<LiveRoom> rooms = liveRoomService.getHotLiveRooms(limit);
            return ResultDTO.success("获取热门直播间成功", rooms);
        } catch (Exception e) {
            log.error("获取热门直播间异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "获取热门直播间异常");
        }
    }

    /**
     * 增加观看人数
     */
    @PostMapping("/room/{roomId}/view")
    public ResultDTO incrementViewCount(@PathVariable Long roomId) {
        try {
            liveRoomService.incrementViewCount(roomId);
            return ResultDTO.success("增加观看人数成功");
        } catch (Exception e) {
            log.error("增加观看人数异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "增加观看人数异常");
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
