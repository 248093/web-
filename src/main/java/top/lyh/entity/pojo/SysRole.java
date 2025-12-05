package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 系统角色实体类（对应 sys_role 表）
 */
@Data
@TableName("sys_role")
public class SysRole {

    /**
     * 角色ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码（唯一标识，如 USER/HOST/ADMIN，Shiro 权限校验用）
     */
    private String roleCode;

    /**
     * 角色显示名称（如：普通用户/主播/系统管理员）
     */
    private String roleName;

    /**
     * 角色功能描述
     */
    private String roleDesc;

    /**
     * 角色状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}