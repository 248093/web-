package top.lyh.entity.dto;

import lombok.Data;
import java.util.Date;

@Data
public class SysUserQueryDto {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名（模糊查询）
     */
    private String userName;
    
    /**
     * 手机号（模糊查询）
     */
    private String phone;
    
    /**
     * 真实姓名（模糊查询）
     */
    private String trueName;
    
    /**
     * 邮箱（模糊查询）
     */
    private String email;
    
    /**
     * 账号状态：1-启用，0-禁用
     */
    private Integer enabled;
    
    /**
     * 性别：1-男，2-女
     */
    private String sex;
    
    /**
     * 最小创建时间
     */
    private Date createTimeStart;
    
    /**
     * 最大创建时间
     */
    private Date createTimeEnd;

    /**
     * 角色编码
     */
    private String roleCode;

    
    /**
     * 页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 是否启用动态查询（默认启用）
     */
    private Boolean enableDynamicQuery = true;
}
