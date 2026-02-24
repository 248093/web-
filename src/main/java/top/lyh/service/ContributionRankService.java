package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.entity.pojo.ContributionRank;
import top.lyh.entity.vo.ContributionRankVo;

import java.util.Date;
import java.util.List;

/**
* @author lyh
* @description 针对表【contribution_rank】的数据库操作Service
* @createDate 2026-02-12 22:43:57
*/
public interface ContributionRankService extends IService<ContributionRank> {
    List<ContributionRankVo> getWeeklyContributionRank(Long liveRoomId);


}
