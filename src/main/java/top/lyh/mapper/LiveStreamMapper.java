package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lyh.entity.pojo.LiveStream;

@Mapper
public interface LiveStreamMapper extends BaseMapper<LiveStream> {
    // 基础CRUD方法由BaseMapper提供
}