package top.lyh.service.serviceImpl;

import com.auth0.jwt.JWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.UserRegisterDTO;
import top.lyh.entity.pojo.SysPermission;
import top.lyh.entity.pojo.SysRole;
import top.lyh.entity.pojo.SysUser;
import top.lyh.entity.pojo.SysUserRole;
import top.lyh.mapper.SysPermissionMapper;
import top.lyh.mapper.SysRoleMapper;
import top.lyh.mapper.SysUserMapper;
import top.lyh.mapper.SysUserRoleMapper;
import top.lyh.service.SysUserService;
import top.lyh.utils.JwtUtil;
import top.lyh.utils.RedisUtil;

import java.util.*;

@Service
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private JwtUtil jwtUtil;

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

    @Transactional
    @Override
    public ResultDTO addUser(UserRegisterDTO userRegisterDTO) {
        // 使用LambdaQueryWrapper匹配用户名或手机号
        SysUser existingUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, userRegisterDTO.getUserName())
                .or()
                .eq(SysUser::getPhone, userRegisterDTO.getPhone()));

        if (existingUser != null) {
            // 用户名或手机号已存在
            return ResultDTO.error("用户名或手机号已存在");
        }
        // 2. 验证码核心校验（存在性 + 正确性）
        // 2.1 从Redis获取存储的验证码（key格式：phone:138xxxx8888）

        Object codeObj = redisUtil.get("phone:" + userRegisterDTO.getPhone());
        String validCodeInRedis = codeObj != null ? codeObj.toString() : null;
        log.info("Redis中的验证码：{}", validCodeInRedis);


        // 2.2 校验1：验证码是否存在（Redis中无值 = 未发送/已过期）
        if (StringUtils.isBlank(validCodeInRedis)) {
            return ResultDTO.error("验证码不存在或已过期，请重新获取");
        }

        // 2.3 校验2：用户输入的验证码是否与Redis中的一致
        // 注意：需确保DTO中包含用户输入的验证码字段（如verifyCode）
        if (!validCodeInRedis.equals(userRegisterDTO.getPhoneCode())) {
            return ResultDTO.error("验证码输入错误，请重新输入");
        }
        //  关联角色（可选：区分主播/普通用户）
        SysRole sysRole = sysRoleMapper.selectByRoleCode(userRegisterDTO.getRoleCode());
        if (sysRole == null) {
            throw new RuntimeException("角色配置错误，请联系管理员");
        }
        SysUser sysUser = new SysUser();
        sysUser.setUserName(userRegisterDTO.getUserName());
        sysUser.setPhone(userRegisterDTO.getPhone());
        String salt = UUID.randomUUID().toString();
        sysUser.setSalt(salt);
        // 加密密码
        String encryptedPassword = new SimpleHash(
                "SHA-256",
                userRegisterDTO.getPassword(),
                ByteSource.Util.bytes(salt),
                10
        ).toHex();
        sysUser.setPassword(encryptedPassword);
        sysUser.setTrueName(userRegisterDTO.getTrueName());
        sysUser.setIdNumber(userRegisterDTO.getIdNumber());
        sysUser.setEnabled(1);
        sysUser.setCreateTime(new Date());
        sysUser.setUpdateTime(new Date());
        log.info("用户信息：{}", sysUser);
        sysUserMapper.insert(sysUser);
        SysUserRole sysUserRole =new SysUserRole();
        sysUserRole.setRoleId(sysRole.getId());
        sysUserRole.setUserId(sysUser.getId());
        sysUserRole.setCreateTime(new Date());
        sysUserRoleMapper.insert(sysUserRole);
        redisUtil.del("phone:" + userRegisterDTO.getPhone());
        return ResultDTO.success("注册成功");
    }

}