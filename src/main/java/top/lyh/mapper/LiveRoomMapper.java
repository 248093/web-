package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lyh.entity.dto.LiveRoomQueryDto;
import top.lyh.entity.vo.LiveRoomDetailVo;
import top.lyh.entity.pojo.LiveRoom;

import java.util.List;

@Mapper
public interface LiveRoomMapper extends BaseMapper<LiveRoom> {
    // 自定义查询方法
    @Select("SELECT * FROM live_room WHERE status = 1 ORDER BY view_count DESC LIMIT #{limit}")
    List<LiveRoom> findHotLiveRooms(@Param("limit") int limit);
    /**
     * 动态条件查询直播间列表
     * @param query 查询条件
     * @param limit 限制条数（不分页传null）
     * @param offset 偏移量（不分页传null）
     * @return 直播间列表
     */
    List<LiveRoomDetailVo> selectByCondition(
            @Param("query") LiveRoomQueryDto query,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );
}