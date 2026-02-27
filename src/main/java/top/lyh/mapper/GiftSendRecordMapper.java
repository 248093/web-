package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import top.lyh.entity.dto.GiftSendRecordQueryDto;
import top.lyh.entity.pojo.GiftSendRecord;
import top.lyh.entity.vo.GiftSendRecordVO;

public interface GiftSendRecordMapper extends BaseMapper<GiftSendRecord> {
    /**
     * 动态条件分页查询礼物赠送记录（关联用户和礼物信息）
     */
    IPage<GiftSendRecordVO> selectByCondition(Page<GiftSendRecordVO> page,
                                              @Param("query") GiftSendRecordQueryDto query);
}
