package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.lyh.entity.pojo.SysRolePermission;

import java.util.List;

/**
 * 角色-权限关联 Mapper（关联 sys_role_permission 表）
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 通过角色ID删除所有关联权限（角色权限变更用）
     * @param roleId 角色ID
     * @return 影响行数
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 通过权限ID删除所有关联角色（权限删除用）
     * @param permId 权限ID
     * @return 影响行数
     */
    int deleteByPermId(@Param("permId") Long permId);

    /**
     * 批量添加角色-权限关联（批量分配权限用）
     * @param rolePermissionList 角色-权限关联列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<SysRolePermission> rolePermissionList);

    /**
     * 通过角色ID查询权限ID列表
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> selectPermIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 通过权限ID查询角色ID列表
     * @param permId 权限ID
     * @return 角色ID列表
     */
    List<Long> selectRoleIdsByPermId(@Param("permId") Long permId);
}