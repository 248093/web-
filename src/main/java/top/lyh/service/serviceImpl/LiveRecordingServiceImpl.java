package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lyh.entity.pojo.LiveRecording;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.mapper.LiveRecordingMapper;
import top.lyh.mapper.LiveRoomMapper;
import top.lyh.service.LiveRecordingService;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class LiveRecordingServiceImpl implements LiveRecordingService {
    
    @Autowired
    private LiveRoomMapper liveRoomMapper;
    
    @Autowired
    private LiveRecordingMapper recordingMapper;
    
    @Autowired
    private MinioClient minioClient;
    
    @Value("${minio.bucket}")
    private String minioBucket;
    
    @Value("${live.record.save-path}")
    private String recordSavePath;

    // 【新增】录制进程缓存：key=recordingId，value=FFmpeg进程（线程安全，仅新增这1个全局变量）
    private final Map<String, Process> recordingProcessCache = new ConcurrentHashMap<>();
    private final Map<String, Long> recordingProcessPidCache = new ConcurrentHashMap<>();
    
    /**
     * 开始录制直播
     */
    @Transactional
    public LiveRecording startRecording(Long roomId) {
        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        if (liveRoom == null || liveRoom.getStatus() != 1) {
            throw new IllegalArgumentException("直播间不存在或未开播");
        }
        
        // 创建录制记录
        LiveRecording recording = new LiveRecording();
        recording.setRoomId(roomId);
        recording.setFileName(liveRoom.getStreamKey() + "_" + System.currentTimeMillis() + ".mp4");
        recording.setStatus(0);  // 录制中
        recording.setStartTime(LocalDateTime.now());
        recording.setCreatedAt(LocalDateTime.now());
        recording.setUpdatedAt(LocalDateTime.now());
        
        recordingMapper.insert(recording);
        
        // 异步启动录制进程
        startRecordingProcess(liveRoom, recording);
        
        return recording;
    }
    
    /**
     * 停止录制直播
     */
    @Transactional
    public LiveRecording stopRecording(Long recordingId) {
        LiveRecording recording = recordingMapper.selectById(recordingId);
        if (recording == null || recording.getStatus() != 0) {
            throw new IllegalArgumentException("录制任务不存在或已结束");
        }
        
        // 更新录制状态
        recording.setStatus(1);  // 录制完成
        recording.setEndTime(LocalDateTime.now());
        recording.setUpdatedAt(LocalDateTime.now());
        log.info("录制实体"+recording.toString());
        recordingMapper.updateById(recording);
        stopRecordingProcess(recording);
        return recording;
    }
    
    /**
     * 获取直播回放列表
     */
    public List<LiveRecording> getRecordings(Long roomId, int page, int size) {
        Page<LiveRecording> pageParam = new Page<>(page, size);
        QueryWrapper<LiveRecording> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId)
                   .eq("status", 3)  // 可用状态
                   .orderByDesc("start_time");
        
        return recordingMapper.selectPage(pageParam, queryWrapper).getRecords();
    }

    /**
     * 启动直播录制进程
     *
     * @param liveRoom 直播间信息
     * @param recording 录制记录信息
     */
    private void startRecordingProcess(LiveRoom liveRoom, LiveRecording recording) {
        CompletableFuture.runAsync(() -> {
            Process process = null;
            try {
                File saveDir = new File(recordSavePath);
                if (!saveDir.exists()) {
                    saveDir.mkdirs();
                }

                String outputPath = recordSavePath + "/" + recording.getFileName();
                String inputUrl = liveRoom.getFlvUrl();

                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg",
                        "-i", inputUrl,
                        "-c:v", "copy",
                        "-c:a", "aac",
                        "-strict", "-2",
                        "-y",
                        "-timeout", "5000000",
                        outputPath
                );

                process = pb.start();
                String processKey = "live:recording:process:" + recording.getId();
                recordingProcessCache.put(processKey, process);

                // 仅保留PID缓存逻辑（Java 9+）
                if (System.getProperty("java.version").startsWith("9")) {
                    try {
                        long pid = process.pid();
                        recordingProcessPidCache.put(processKey, pid);
                        log.info("FFmpeg启动成功, recordingId={}, PID={}", recording.getId(), pid);
                    } catch (Exception e) {
                        log.warn("获取FFmpeg PID失败（Java版本可能低于9）", e);
                    }
                }

                boolean isExited = process.waitFor(1, TimeUnit.HOURS);
                int exitCode = process.exitValue();
                log.info("FFmpeg进程退出, recordingId={}, exitCode={}, isExited={}",
                        recording.getId(), exitCode, isExited);

                File recordFile = new File(outputPath);
                if (recordFile.exists() && recordFile.length() > 0) {
                    uploadRecording(recording, recordFile);
                } else {
                    log.error("录制文件为空, recordingId={}", recording.getId());
                    updateRecordingStatus(recording.getId(), 4);
                }

            } catch (Exception e) {
                log.error("录制异常", e);
                updateRecordingStatus(recording.getId(), 4);
            } finally {
                if (process != null) {
                    String processKey = "live:recording:process:" + recording.getId();
                    recordingProcessCache.remove(processKey);
                    recordingProcessPidCache.remove(processKey);
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                }
            }
        });
    }

    /**
     * 停止录制进程（仅保留PID停止方案）
     */
    private void stopRecordingProcess(LiveRecording recording) {
        log.info("Windows下手动停止录制, recordingId={}", recording.getId());

        try {
            String processKey = "live:recording:process:" + recording.getId();
            Long ffmpegPid = recordingProcessPidCache.get(processKey);

            // 仅保留PID停止逻辑
            if (ffmpegPid != null) {
                ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(ffmpegPid));
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Process killProcess = pb.start();
                boolean isKilled = killProcess.waitFor(5, TimeUnit.SECONDS);

                if (isKilled && killProcess.exitValue() == 0) {
                    log.info("通过PID停止FFmpeg成功, recordingId={}, PID={}", recording.getId(), ffmpegPid);
                    Thread.sleep(800);
                    uploadRecording(recording, new File(recordSavePath + "/" + recording.getFileName()));
                    return;
                }
            }

            // PID停止失败时直接标记为失败（无兜底逻辑）
            log.error("PID停止失败, recordingId={}", recording.getId());
            updateRecordingStatus(recording.getId(), 4);

        } catch (Exception e) {
            log.error("停止录制进程异常, recordingId={}", recording.getId(), e);
            updateRecordingStatus(recording.getId(), 4);
        } finally {
            String processKey = "live:recording:process:" + recording.getId();
            recordingProcessCache.remove(processKey);
            recordingProcessPidCache.remove(processKey);
        }
    }

    // 提取统一的状态更新方法（与启动方法复用）
    private void updateRecordingStatus(Long recordingId, int status) {
        LiveRecording update = new LiveRecording();
        update.setId(recordingId);
        update.setStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        recordingMapper.updateById(update);
    }
    
    /**
     * 上传录制文件到MinIO
     */
    private void uploadRecording(LiveRecording recording, File file) {
        try {
            // 设置状态为处理中
            LiveRecording processingRecording = new LiveRecording();
            processingRecording.setId(recording.getId());
            processingRecording.setStatus(2);  // 处理中
            processingRecording.setUpdatedAt(LocalDateTime.now());
            recordingMapper.updateById(processingRecording);

            // 获取文件元数据
            long fileSize = file.length();

            // 使用FFmpeg获取视频时长
            String[] cmd = {
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.getAbsolutePath()
            };

            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String durationStr = reader.readLine();
            int duration = (int) Float.parseFloat(durationStr);

            // 上传到MinIO
            String objectName = "recordings/" + recording.getFileName();
            minioClient.uploadObject(
                UploadObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(objectName)
                    .filename(file.getAbsolutePath())
                    .contentType("video/mp4")
                    .build()
            );

            // 构建访问URL
            String fileUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .bucket(minioBucket)
                    .object(objectName)
                    .method(Method.GET)
                    .build()
            );

            // 更新录制记录
            LiveRecording updatedRecording = new LiveRecording();
            updatedRecording.setId(recording.getId());
            updatedRecording.setFileUrl(fileUrl);
            updatedRecording.setFileSize(fileSize);
            updatedRecording.setDuration(duration);
            updatedRecording.setStatus(3);  // 可用状态
            updatedRecording.setUpdatedAt(LocalDateTime.now());

            recordingMapper.updateById(updatedRecording);

            log.info("录制文件上传完成, recordingId={}, fileSize={}, duration={}s",
                    recording.getId(), fileSize, duration);

            // 删除本地文件
            file.delete();

        } catch (Exception e) {
            log.error("上传录制文件异常", e);

            // 更新录制状态为失败
            LiveRecording failedRecording = new LiveRecording();
            failedRecording.setId(recording.getId());
            failedRecording.setStatus(4);  // 失败状态
            failedRecording.setUpdatedAt(LocalDateTime.now());

            recordingMapper.updateById(failedRecording);
        }
    }
}