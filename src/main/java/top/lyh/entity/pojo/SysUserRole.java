package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 用户-角色关联实体类（对应 sys_user_role 表）
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    /**
     * 关联ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（关联 sys_user.id）
     */
    private Long userId;

    /**
     * 角色ID（关联 sys_role.id）
     */
    private Long roleId;

    /**
     * 关联创建时间
     */
    private Date createTime;
}