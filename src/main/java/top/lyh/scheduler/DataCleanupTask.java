package top.lyh.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.lyh.entity.pojo.LiveRecording;
import top.lyh.mapper.LiveRecordingMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DataCleanupTask {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private LiveRecordingMapper recordingMapper;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${cleanup.days:30}")
    private int cleanupDays;

    @Value("${cleanup.batch-size:100}")
    private int batchSize;

    @Scheduled(cron = "${cleanup.cron:0 0 2 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOldData() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
        log.info("开始清理{}天前的数据，截止日期: {}", cleanupDays, cutoffDate);

        int totalDeleted = 0;
        int page = 0;

        while (true) {
            // 分页查询需要删除的记录
            QueryWrapper<LiveRecording> wrapper = new QueryWrapper<>();
            wrapper.lambda()
                    .le(LiveRecording::getCreatedAt, cutoffDate)
                    .eq(LiveRecording::getStatus, 3)
                    .last("LIMIT " + batchSize);
            
            List<LiveRecording> recordings = recordingMapper.selectList(wrapper);
            if (recordings.isEmpty()) {
                break;
            }

            // 批量删除
            List<Long> successIds = new ArrayList<>();
            for (LiveRecording recording : recordings) {
                try {
                    // 删除MinIO文件
                    if (recording.getFileUrl() != null) {
                        String objectName = extractObjectName(recording.getFileUrl());
                        minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                        );
                    }
                    successIds.add(recording.getId());
                    totalDeleted++;
                } catch (Exception e) {
                    log.error("删除失败 recordingId={}", recording.getId(), e);
                }
            }

            // 删除数据库记录
            if (!successIds.isEmpty()) {
                recordingMapper.deleteBatchIds(successIds);
            }

            page++;
            log.info("完成第{}批清理，已删除{}条记录", page, totalDeleted);
        }

        log.info("清理完成，共删除{}条记录", totalDeleted);
    }

    private String extractObjectName(String fileUrl) {
        // 从URL中提取对象路径
        int index = fileUrl.indexOf("/recordings/");
        if (index > 0) {
            return fileUrl.substring(index + 1);
        }
        return fileUrl;
    }
}