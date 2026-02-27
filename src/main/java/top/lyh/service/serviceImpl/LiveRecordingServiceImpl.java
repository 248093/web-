package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import top.lyh.common.PageResult;
import top.lyh.entity.pojo.LiveRecording;
import top.lyh.entity.pojo.LiveRoom;
import top.lyh.mapper.LiveRecordingMapper;
import top.lyh.mapper.LiveRoomMapper;
import top.lyh.service.LiveRecordingService;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    private final Map<String, Process> recordingProcessCache = new ConcurrentHashMap<>();
    private final Map<String, Long> recordingProcessPidCache = new ConcurrentHashMap<>();

    /**
     * 开始录制直播
     */
    @Override
    @Transactional
    public LiveRecording startRecording(Long roomId) {
        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        log.info("开始录制 - 直播间信息: roomId={}, status={}, streamKey={}",
                roomId, liveRoom != null ? liveRoom.getStatus() : null,
                liveRoom != null ? liveRoom.getStreamKey() : null);

        if (liveRoom == null || liveRoom.getStatus() != 1) {
            throw new IllegalArgumentException("直播间不存在或未开播");
        }

        // 检查是否已有正在录制的任务
        QueryWrapper<LiveRecording> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("room_id", roomId)
                .eq("status", 0); // 0表示录制中
        Long count = recordingMapper.selectCount(checkWrapper);
        if (count > 0) {
            log.warn("该直播间已有正在录制的任务: roomId={}", roomId);
            throw new IllegalArgumentException("该直播间正在录制中");
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
        log.info("录制记录已创建: recordingId={}, fileName={}", recording.getId(), recording.getFileName());

        // 异步启动录制进程
        startRecordingProcess(liveRoom, recording);

        return recording;
    }

    /**
     * 停止录制直播
     */
    @Override
    @Transactional
    public LiveRecording stopRecording(Long recordingId) {
        LiveRecording recording = recordingMapper.selectById(recordingId);
        if (recording == null || recording.getStatus() != 0) {
            throw new IllegalArgumentException("录制任务不存在或已结束");
        }

        log.info("停止录制: recordingId={}, fileName={}", recording.getId(), recording.getFileName());

        // 更新录制状态
        recording.setStatus(1);  // 录制完成
        recording.setEndTime(LocalDateTime.now());
        recording.setUpdatedAt(LocalDateTime.now());
        recordingMapper.updateById(recording);

        // 停止录制进程
        stopRecordingProcess(recording);

        return recording;
    }

    /**
     * 获取直播回放列表
     */
    @Override
    public PageResult<LiveRecording> getRecordings(LiveRecording liveRecording, Integer page, Integer size) {
        QueryWrapper<LiveRecording> queryWrapper = new QueryWrapper<>();
        if (page == null || page <= 0) page = 1;
        if (size == null || size <= 0) size = 10;

        if (liveRecording.getRoomId() != null) {
            queryWrapper.eq("room_id", liveRecording.getRoomId());
        }
        if (liveRecording.getStartTime() != null) {
            queryWrapper.ge("start_time", liveRecording.getStartTime());
        }
        if (liveRecording.getEndTime() != null) {
            queryWrapper.le("end_time", liveRecording.getEndTime());
        }
        if (liveRecording.getStatus() != null) {
            queryWrapper.eq("status", liveRecording.getStatus());
        }

        queryWrapper.orderByDesc("created_at");

        Page<LiveRecording> pageParam = new Page<>(page, size);
        IPage<LiveRecording> pageList = recordingMapper.selectPage(pageParam, queryWrapper);
        return new PageResult<>(pageList.getRecords(), pageList.getTotal(), page, size);
    }

    /**
     * 启动直播录制进程
     */
    private void startRecordingProcess(LiveRoom liveRoom, LiveRecording recording) {
        CompletableFuture.runAsync(() -> {
            Process process = null;
            String processKey = "live:recording:process:" + recording.getId();

            try {
                File saveDir = new File(recordSavePath);
                if (!saveDir.exists()) {
                    boolean created = saveDir.mkdirs();
                    log.info("创建录制目录: {}, success={}", recordSavePath, created);
                }

                String outputPath = recordSavePath + "/" + recording.getFileName();
                String inputUrl = liveRoom.getFlvUrl();

                log.info("启动FFmpeg进程: recordingId={}, inputUrl={}, outputPath={}",
                        recording.getId(), inputUrl, outputPath);

                // 构建FFmpeg命令
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

                // 合并错误流以便捕获所有输出
                pb.redirectErrorStream(true);

                // 启动进程
                process = pb.start();

                // 缓存进程
                recordingProcessCache.put(processKey, process);

                // 获取PID - 所有Java 9+版本都支持，不限于以"9"开头
                try {
                    long pid = process.pid();
                    recordingProcessPidCache.put(processKey, pid);
                    log.info("✅ FFmpeg进程启动成功: recordingId={}, PID={}", recording.getId(), pid);
                } catch (Exception e) {
                    log.warn("获取FFmpeg PID失败: {}", e.getMessage());
                }

                // 异步读取FFmpeg输出（用于调试）
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                CompletableFuture.runAsync(() -> {
                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            log.debug("FFmpeg输出 [{}]: {}", recording.getId(), line);
                        }
                    } catch (IOException e) {
                        log.error("读取FFmpeg输出失败", e);
                    }
                });

                // 等待进程结束（不设超时，一直等待直到进程结束）
                int exitCode = process.waitFor();
                log.info("FFmpeg进程退出: recordingId={}, exitCode={}", recording.getId(), exitCode);

                // 检查退出码
                if (exitCode != 0) {
                    log.error("FFmpeg进程异常退出: recordingId={}, exitCode={}", recording.getId(), exitCode);
                }

                // 检查文件是否存在并上传
                File recordFile = new File(outputPath);
                if (recordFile.exists() && recordFile.length() > 0) {
                    log.info("录制文件生成成功: recordingId={}, size={} bytes",
                            recording.getId(), recordFile.length());
                    uploadRecording(recording, recordFile);
                } else {
                    log.error("录制文件不存在或为空: recordingId={}, path={}",
                            recording.getId(), outputPath);
                    updateRecordingStatus(recording.getId(), 4); // 4表示失败
                }

            } catch (Exception e) {
                log.error("❌ 录制异常: recordingId={}", recording.getId(), e);
                updateRecordingStatus(recording.getId(), 4);
            } finally {
                if (process != null) {
                    recordingProcessCache.remove(processKey);
                    recordingProcessPidCache.remove(processKey);
                    if (process.isAlive()) {
                        log.warn("强制终止FFmpeg进程: recordingId={}", recording.getId());
                        process.destroyForcibly();
                    }
                }
            }
        });
    }

    /**
     * 停止录制进程
     */
    private void stopRecordingProcess(LiveRecording recording) {
        log.info("停止录制进程: recordingId={}, fileName={}", recording.getId(), recording.getFileName());

        String processKey = "live:recording:process:" + recording.getId();

        try {
            // 先尝试通过PID停止
            Long ffmpegPid = recordingProcessPidCache.get(processKey);

            if (ffmpegPid != null) {
                log.info("尝试通过PID停止FFmpeg: recordingId={}, PID={}", recording.getId(), ffmpegPid);

                // Windows下使用taskkill
                ProcessBuilder pb = new ProcessBuilder(
                        "taskkill", "/F", "/T", "/PID", String.valueOf(ffmpegPid)
                );
                pb.redirectErrorStream(true);

                Process killProcess = pb.start();

                // 读取输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(killProcess.getInputStream()));
                CompletableFuture.runAsync(() -> {
                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            log.debug("taskkill输出: {}", line);
                        }
                    } catch (IOException e) {
                        log.error("读取taskkill输出失败", e);
                    }
                });

                boolean isKilled = killProcess.waitFor(5, TimeUnit.SECONDS);

                if (isKilled && killProcess.exitValue() == 0) {
                    log.info("✅ 通过PID停止FFmpeg成功: recordingId={}, PID={}", recording.getId(), ffmpegPid);

                    // 等待文件写入完成
                    Thread.sleep(2000);

                    // 检查文件并上传
                    File recordFile = new File(recordSavePath + "/" + recording.getFileName());
                    if (recordFile.exists() && recordFile.length() > 0) {
                        log.info("停止后找到录制文件: recordingId={}, size={}",
                                recording.getId(), recordFile.length());
                        uploadRecording(recording, recordFile);
                        return;
                    } else {
                        log.warn("停止后找不到录制文件或文件为空: recordingId={}, path={}",
                                recording.getId(), recordFile.getAbsolutePath());
                    }
                } else {
                    log.warn("通过PID停止FFmpeg失败: recordingId={}, exitCode={}",
                            recording.getId(), killProcess.exitValue());
                }
            }

            // 如果PID方式失败，尝试通过缓存的Process对象停止
            Process process = recordingProcessCache.get(processKey);
            if (process != null && process.isAlive()) {
                log.info("尝试通过Process对象停止FFmpeg: recordingId={}", recording.getId());
                process.destroy();

                boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                if (exited) {
                    log.info("通过Process对象停止FFmpeg成功: recordingId={}", recording.getId());

                    // 等待文件写入完成
                    Thread.sleep(2000);

                    File recordFile = new File(recordSavePath + "/" + recording.getFileName());
                    if (recordFile.exists() && recordFile.length() > 0) {
                        uploadRecording(recording, recordFile);
                        return;
                    }
                } else {
                    process.destroyForcibly();
                }
            }

            // 所有停止方式都失败
            log.error("❌ 停止FFmpeg进程失败: recordingId={}", recording.getId());
            updateRecordingStatus(recording.getId(), 4);

        } catch (Exception e) {
            log.error("停止录制进程异常: recordingId={}", recording.getId(), e);
            updateRecordingStatus(recording.getId(), 4);
        } finally {
            recordingProcessCache.remove(processKey);
            recordingProcessPidCache.remove(processKey);
        }
    }

    /**
     * 更新录制状态
     */
    private void updateRecordingStatus(Long recordingId, int status) {
        LiveRecording update = new LiveRecording();
        update.setId(recordingId);
        update.setStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        recordingMapper.updateById(update);
        log.info("更新录制状态: recordingId={}, status={}", recordingId, status);
    }

    /**
     * 上传录制文件到MinIO
     */
    private void uploadRecording(LiveRecording recording, File file) {
        log.info("开始上传录制文件: recordingId={}, file={}, size={}",
                recording.getId(), file.getName(), file.length());

        try {
            // 设置状态为处理中
            updateRecordingStatus(recording.getId(), 2); // 2表示处理中

            // 获取文件元数据
            long fileSize = file.length();

            // 使用FFmpeg获取视频时长
            int duration = 0;
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "ffprobe",
                        "-v", "error",
                        "-show_entries", "format=duration",
                        "-of", "default=noprint_wrappers=1:nokey=1",
                        file.getAbsolutePath()
                );
                pb.redirectErrorStream(true);

                Process probeProcess = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(probeProcess.getInputStream()));
                String durationStr = reader.readLine();

                if (durationStr != null && !durationStr.isEmpty()) {
                    duration = (int) Float.parseFloat(durationStr);
                    log.info("视频时长: {}秒", duration);
                }

                probeProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("获取视频时长失败: {}", e.getMessage());
            }

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
            log.info("MinIO上传成功: bucket={}, object={}", minioBucket, objectName);

            // 生成访问URL（7天有效期）
            String fileUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioBucket)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );

            // 更新录制记录
            LiveRecording updatedRecording = new LiveRecording();
            updatedRecording.setId(recording.getId());
            updatedRecording.setFileUrl(fileUrl);
            updatedRecording.setFileSize(fileSize);
            updatedRecording.setDuration(duration);
            updatedRecording.setStatus(3);  // 3表示可用
            updatedRecording.setUpdatedAt(LocalDateTime.now());

            recordingMapper.updateById(updatedRecording);

            log.info("✅ 录制文件上传完成: recordingId={}, fileSize={}, duration={}s, url={}",
                    recording.getId(), fileSize, duration, fileUrl);

            // 删除本地文件
            if (file.delete()) {
                log.info("本地文件已删除: {}", file.getAbsolutePath());
            } else {
                log.warn("本地文件删除失败: {}", file.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("❌ 上传录制文件异常", e);
            updateRecordingStatus(recording.getId(), 4); // 4表示失败
        }
    }
}