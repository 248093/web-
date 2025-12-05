package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 系统权限实体类（对应 sys_permission 表）
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    /**
     * 权限ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限编码（唯一标识，如 live:view，Shiro 权限校验用）
     */
    private String permCode;

    /**
     * 权限显示名称（如：查看直播）
     */
    private String permName;

    /**
     * 权限类型：1-接口权限，2-菜单权限，3-按钮权限
     */
    private Integer permType;

    /**
     * 父权限ID（0表示顶级权限）
     */
    private Long parentId;

    /**
     * 资源路径（接口路径/路由路径）
     */
    private String resourcePath;

    /**
     * 权限状态：1-启用，0-禁用
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