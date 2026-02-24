package top.lyh.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import top.lyh.entity.pojo.ContributionRank;
import top.lyh.entity.vo.ContributionRankVo;

import java.time.LocalDate;
import java.util.List;

/**
* @author lyh
* @description 针对表【contribution_rank】的数据库操作Mapper
* @createDate 2026-02-12 22:43:57
* @Entity .domain.ContributionRank
*/
public interface ContributionRankMapper extends BaseMapper<ContributionRank> {
    List<ContributionRankVo> getWeeklyContributionRank(
            @Param("liveRoomId") Long liveRoomId
    );

    int deleteExpiredContributions();

}




