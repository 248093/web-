package top.lyh.service.serviceImpl;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.lyh.entity.pojo.ContributionRank;
import top.lyh.entity.vo.ContributionRankVo;
import top.lyh.mapper.ContributionRankMapper;
import top.lyh.service.ContributionRankService;

import java.util.Date;
import java.util.List;

/**
* @author lyh
* @description 针对表【contribution_rank】的数据库操作Service实现
* @createDate 2026-02-12 22:43:57
*/
@Service
public class ContributionRankServiceImpl extends ServiceImpl<ContributionRankMapper, ContributionRank>
    implements ContributionRankService {
    @Autowired
    private ContributionRankMapper contributionRankMapper;

    /**
     * 获取直播间近7天贡献榜
     *
     * @param liveRoomId 直播间ID
     * @return 贡献榜列表
     */
    @Override
    public List<ContributionRankVo> getWeeklyContributionRank(Long liveRoomId) {
        // 参数校验
        if (liveRoomId == null || liveRoomId <= 0) {
            throw new IllegalArgumentException("直播间ID不能为空或小于等于0");
        }

        // 查询数据库
        List<ContributionRankVo> result = contributionRankMapper.getWeeklyContributionRank(liveRoomId);


        return result;
    }

}




