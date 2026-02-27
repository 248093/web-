package top.lyh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.lyh.anno.LogAnnotation;
import top.lyh.common.PageResult;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.LiveRecordingQueryDto;
import top.lyh.entity.dto.LiveRoomQueryDto;
import top.lyh.entity.pojo.LiveRecording;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.entity.pojo.LiveStream;
import top.lyh.entity.pojo.SysUser;
import top.lyh.entity.vo.LiveRoomDetailVo;
import top.lyh.service.*;
import top.lyh.utils.AliOSSUtils;
import top.lyh.utils.RedisUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    @Autowired
    private AliOSSUtils aliOSSUtils;

    /**
     * 创建直播间
     */
    @RequiresRoles(value = {"ADMIN", "HOST"}, logical = Logical.OR)
    @PostMapping("/room")
    @LogAnnotation(value = "创建直播间", recordParams = false, recordResult = true)
    public ResultDTO createLiveRoom(@RequestBody LiveRoom liveRoom) {
        try {
            if (liveRoom.getId()== null) {
                LiveRoom createdRoom = liveRoomService.createLiveRoom(liveRoom);
                return ResultDTO.success("创建直播间成功", createdRoom);
            }else {
                boolean b = liveRoomService.updateLiveRoom(liveRoom);
                return b ? ResultDTO.success("更新直播间信息成功", liveRoom) : ResultDTO.error(ResponseCodeEnum.ERROR, "更新直播间信息失败");
            }
        } catch (Exception e) {
            log.error("创建直播间异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, e.getMessage());
        }
    }

    /**
     * 获取直播间详情
     */
    @GetMapping("/room/{roomId}")
    @LogAnnotation(value = "获取直播间详情", recordParams = false, recordResult = true)
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
    // 获得推流地址
    @GetMapping("/room/get/streamUrl")
    @LogAnnotation(value = "获取推流地址", recordParams = false, recordResult = true)
    @RequiresRoles(value = {"ADMIN", "HOST"}, logical = Logical.OR)
    public ResultDTO getPushUrl() {
        Subject subject = SecurityUtils.getSubject();
        log.info("getPushUrl - Subject是否存在: {}", subject != null);
        log.info("getPushUrl - Subject是否已认证: {}", subject.isAuthenticated());
        log.info("getPushUrl - Principal: {}", subject.getPrincipal());
        SysUser sysUser=(SysUser) subject.getPrincipal();
        log.info("用户Id: {}", sysUser);
        Long userId = sysUser.getId();
        LambdaQueryWrapper<LiveRoom> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LiveRoom::getUserId, userId);
            LiveRoom liveRoom = liveRoomService.getOne(queryWrapper);
            log.info("直播间: {}", liveRoom.getStreamUrl());
        if (liveRoom == null) {
            return ResultDTO.error(ResponseCodeEnum.NOT_FOUND, "直播间不存在");
        }
        return ResultDTO.success("获取成功！",liveRoom.getStreamUrl());
    }



    /**
     * 开始直播
     */
    @PostMapping("/room/{roomId}/start")
    @RequiresRoles(value = {"ADMIN", "HOST"}, logical = Logical.OR)
    @LogAnnotation(value = "开始直播", recordParams = false, recordResult = true)
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
    @LogAnnotation(value = "结束直播", recordParams = false, recordResult = true)
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
            redisTemplate.incr(key, 1);
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
    @RequiresRoles("ADMIN")
    @PostMapping("/room/admin/recordings")
    @LogAnnotation(value = "获取直播回放列表", recordParams = false, recordResult = true)
    public ResultDTO getRecordings(
            @RequestBody LiveRecordingQueryDto liveRecordingQueryDto) {
        try {
            PageResult<LiveRecording> recordings = recordingService.getRecordings(liveRecordingQueryDto.getLiveRecording(),
                    liveRecordingQueryDto.getPage(),
                    liveRecordingQueryDto.getSize());
            return ResultDTO.success("获取直播回放列表成功", recordings);
        } catch (Exception e) {
            log.error("获取直播回放列表异常", e);
            return ResultDTO.error(ResponseCodeEnum.ERROR, "获取直播回放列表异常");
        }
    }
    @PostMapping("/uploadAvatar")
    @LogAnnotation(value = "上传直播封面头像", recordParams = false, recordResult = true)
    public ResultDTO uploadAvatar(@RequestParam MultipartFile file) {
        try {
            String url = aliOSSUtils.upload(file, "liveCover");
            return ResultDTO.success("上传成功", url);
        } catch (Exception e) {
            log.error("上传失败", e);
            return ResultDTO.error("上传失败");
        }
    }
    /**
     * 封禁直播间
     */
    @RequiresRoles("ADMIN")
    @PutMapping("/ban")
    @Transactional
    @LogAnnotation(value = "封禁直播间", recordParams = false, recordResult = true)
    public ResultDTO updateLiveRoomStatus(@RequestParam Long liveRoomId, @RequestParam Integer status) {
        try {
            LiveRoom liveRoom = new LiveRoom();
            liveRoom.setId(liveRoomId);
            liveRoom.setStatus(status);
            LambdaQueryWrapper<LiveRoom> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LiveRoom::getId, liveRoomId);
            LambdaQueryWrapper<LiveStream> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(LiveStream::getRoomId, liveRoomId);
            LiveStream one = liveStreamService.getOne(queryWrapper1);
            if (one != null){
                one.setStatus(status);
                liveStreamService.update(one, queryWrapper1);
            }
            boolean result = liveRoomService.update(liveRoom, queryWrapper);
            return result ? ResultDTO.success("操作成功") : ResultDTO.error("操作失败");
        } catch (Exception e) {
            log.error("封禁用户异常", e);
            return ResultDTO.error("操作失败：" + e.getMessage());
        }
    }
}
