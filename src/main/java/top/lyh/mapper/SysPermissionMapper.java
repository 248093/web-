package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.lyh.entity.pojo.SysPermission;

import java.util.List;

/**
 * 系统权限 Mapper（关联 sys_permission 表）
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 通过权限编码查询权限（Shiro权限校验用）
     * @param permCode 权限编码（如 live:create）
     * @return 权限实体
     */
    SysPermission selectByPermCode(@Param("permCode") String permCode);

    /**
     * 通过角色ID查询权限列表（权限授权用）
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 通过用户ID查询权限列表（Shiro授权核心方法）
     * @param userId 用户ID
     * @return 权限列表（去重）
     */
    List<SysPermission> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询所有启用的权限
     * @return 启用的权限列表
     */
    List<SysPermission> selectAllEnabled();
}