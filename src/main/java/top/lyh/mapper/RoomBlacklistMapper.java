package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import top.lyh.entity.dto.RoomBlacklistQueryDto;
import top.lyh.entity.pojo.RoomBlacklist;
import top.lyh.entity.vo.RoomBlacklistDetailVo;

/**
 * 直播间黑名单Mapper接口
 */
public interface RoomBlacklistMapper extends BaseMapper<RoomBlacklist> {
    /**
     * 分页查询黑名单详情
     */
    IPage<RoomBlacklistDetailVo> selectBlacklistDetailPage(
            Page<RoomBlacklistDetailVo> page,
            @Param("query") RoomBlacklistQueryDto queryDto
    );
}
