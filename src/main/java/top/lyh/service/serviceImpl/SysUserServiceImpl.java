package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.lyh.entity.pojo.SysPermission;
import top.lyh.entity.pojo.SysRole;
import top.lyh.entity.pojo.SysUser;
import top.lyh.mapper.SysPermissionMapper;
import top.lyh.mapper.SysRoleMapper;
import top.lyh.mapper.SysUserMapper;
import top.lyh.service.SysUserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Override
    public SysUser findByUsername(String userName) {
        // 调用 Mapper 方法：通过用户名查询用户（已校验启用状态）
        return baseMapper.selectByUserName(userName);
    }

    @Override
    public Set<String> findRoles(String userName) {
        // 1. 通过用户名查询用户ID
        SysUser user = findByUsername(userName);
        if (user == null) {
            return new HashSet<>();
        }
        // 2. 通过用户ID查询角色列表
        List<SysRole> roleList = sysRoleMapper.selectByUserId(user.getId());
        // 3. 提取角色编码（Shiro 授权用）
        Set<String> roleCodes = new HashSet<>();
        for (SysRole role : roleList) {
            if (role.getStatus() == 1) { // 只添加启用的角色
                roleCodes.add(role.getRoleCode());
            }
        }
        return roleCodes;
    }

    @Override
    public Set<String> findPermissions(String userName) {
        // 1. 通过用户名查询用户ID
        SysUser user = findByUsername(userName);
        if (user == null) {
            return new HashSet<>();
        }
        // 2. 通过用户ID查询权限列表（三表关联，已去重、已校验启用状态）
        List<SysPermission> permList = sysPermissionMapper.selectByUserId(user.getId());
        // 3. 提取权限编码（Shiro 授权用）
        Set<String> permCodes = new HashSet<>();
        for (SysPermission perm : permList) {
            if (perm.getStatus() == 1) { // 只添加启用的权限
                permCodes.add(perm.getPermCode());
            }
        }
        return permCodes;
    }
}