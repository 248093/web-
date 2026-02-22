package top.lyh.entity.vo;

import lombok.Data;

@Data
public class ContributionRankVo {
    private Long userId;      // 用户ID
    private String username;  // 用户名
    private String avatar;    // 头像URL
    private Double totalAmount; // 总贡献金额
    private Integer rank;   //排名
}
