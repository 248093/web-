package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.lyh.entity.pojo.SysUserRole;

import java.util.List;

/**
 * 用户-角色关联 Mapper（关联 sys_user_role 表）
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 通过用户ID删除所有关联角色（用户角色变更用）
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 通过角色ID删除所有关联用户（角色删除用）
     * @param roleId 角色ID
     * @return 影响行数
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量添加用户-角色关联（批量分配角色用）
     * @param userRoleList 用户-角色关联列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<SysUserRole> userRoleList);

    /**
     * 通过用户ID查询角色ID列表
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 通过角色ID查询用户ID列表
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}