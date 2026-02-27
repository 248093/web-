package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lyh.entity.pojo.SensitiveWord;

@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
    // 基础CRUD方法由BaseMapper提供
}
