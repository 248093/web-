package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 角色-权限关联实体类（对应 sys_role_permission 表）
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    /**
     * 关联ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID（关联 sys_role.id）
     */
    private Long roleId;

    /**
     * 权限ID（关联 sys_permission.id）
     */
    private Long permId;

    /**
     * 关联创建时间
     */
    private Date createTime;
}