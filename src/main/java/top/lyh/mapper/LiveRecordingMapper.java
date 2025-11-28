package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lyh.entity.pojo.LiveRecording;

@Mapper
public interface LiveRecordingMapper extends BaseMapper<LiveRecording> {
    // 基础CRUD方法由BaseMapper提供
}