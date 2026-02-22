package top.lyh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.lyh.common.ResultDTO;
import top.lyh.entity.pojo.Gift;
import top.lyh.entity.vo.ContributionRankVo;
import top.lyh.service.ContributionRankService;
import top.lyh.service.GiftService;

import java.util.List;

@RestController
@RequestMapping("/api/gift")
public class GiftController {
    @Autowired
    private GiftService giftService;
    @Autowired
    private ContributionRankService contributionRankService;
    @GetMapping("/list")
    public ResultDTO gitfList(@Validated Long id) {
        List<Gift> gift = giftService.queryGift(id);
        return ResultDTO.success(gift);
    }
    @PostMapping("/saveOrEdit")
    public ResultDTO saveOrEditGift(@Validated @ModelAttribute Gift gift, @RequestParam("file") MultipartFile file) {
        boolean b = giftService.saveOrEditGift(gift, file);
        return b ? ResultDTO.success() : ResultDTO.error("保存失败！");
    }

    @DeleteMapping("/delete")
    public ResultDTO deleteGift(@Validated Long id) {
        boolean b = giftService.deleteGift(id);
        return b ? ResultDTO.success() : ResultDTO.error("删除失败！");
    }
    @GetMapping("/weeklyRank")
    public ResultDTO weeklyRank(@RequestParam Long liveRoomId) {
        List<ContributionRankVo> weeklyContributionRank = contributionRankService.getWeeklyContributionRank(liveRoomId);
        return ResultDTO.success(weeklyContributionRank);
    }

}
