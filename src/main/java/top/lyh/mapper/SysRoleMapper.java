package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.lyh.entity.pojo.SysRole;

import java.util.List;

/**
 * 系统角色 Mapper（关联 sys_role 表）
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 通过角色编码查询角色（Shiro权限校验用）
     * @param roleCode 角色编码（如 USER/HOST/ADMIN）
     * @return 角色实体
     */
    SysRole selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 通过用户ID查询角色列表（权限授权用）
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询所有启用的角色
     * @return 启用的角色列表
     */
    List<SysRole> selectAllEnabled();
}