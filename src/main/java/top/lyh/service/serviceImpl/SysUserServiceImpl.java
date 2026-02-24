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
import org.springframework.web.multipart.MultipartFile;
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
import top.lyh.utils.AliOSSUtils;
import top.lyh.utils.JwtUtil;
import top.lyh.utils.RedisUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
    @Autowired
    private AliOSSUtils aliOSSUtils;

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

    @Transactional
    @Override
    public ResultDTO updateUser(SysUser user) {
        try {
            if (user == null || user.getId() == null) return ResultDTO.error("参数错误");

            SysUser old = sysUserMapper.selectById(user.getId());
            if (old == null) return ResultDTO.error("用户不存在");

            // 创建要更新的对象
            SysUser updateUser = new SysUser();
            updateUser.setId(user.getId());
            updateUser.setUpdateTime(new Date());

            // 判断用户名是否变化
            if (StringUtils.isNotBlank(user.getUserName()) && !user.getUserName().equals(old.getUserName())) {
                // 唯一性检查
                if (checkUserNameExist(user.getUserName(), user.getId()))
                    return ResultDTO.error("用户名已存在");
                updateUser.setUserName(user.getUserName());
            }

            // 判断真实姓名是否变化
            if (StringUtils.isNotBlank(user.getTrueName()) && !user.getTrueName().equals(old.getTrueName())) {
                updateUser.setTrueName(user.getTrueName());
            }

            // 判断密码是否变化
            if (StringUtils.isNotBlank(user.getPassword())) {
                String salt = old.getSalt();
                if (StringUtils.isBlank(salt)) {
                    salt = UUID.randomUUID().toString();
                    updateUser.setSalt(salt);
                } else {
                    updateUser.setSalt(old.getSalt());
                }

                String encryptedPassword = new SimpleHash("SHA-256", user.getPassword(),
                        ByteSource.Util.bytes(salt), 10).toHex();

                // 只有当新密码与旧密码不同时才更新
                if (!encryptedPassword.equals(old.getPassword())) {
                    updateUser.setPassword(encryptedPassword);
                }
            }

            // 判断头像是否变化
            if (StringUtils.isNotBlank(user.getAvatar()) && !user.getAvatar().equals(old.getAvatar())) {
                updateUser.setAvatar(user.getAvatar());
            }
            // 判断邮箱是否变化
            if (StringUtils.isNotBlank(user.getEmail()) && !user.getEmail().equals(old.getEmail())){
                updateUser.setEmail(user.getEmail());
            }
            // 判断性别是否变化
            if (StringUtils.isNotBlank(user.getSex()) && !user.getSex().equals(old.getSex())) {
                updateUser.setSex(user.getSex());
            }


            // 只有有变化时才执行更新
            if (hasChanges(updateUser)) {
                sysUserMapper.updateById(updateUser);
            }

            // 返回最新用户信息
            SysUser result = sysUserMapper.selectById(user.getId());
            result.setPassword(null);
            result.setSalt(null);
            return ResultDTO.success("操作成功", result);

        } catch (Exception e) {
            log.error("更新用户异常", e);
            return ResultDTO.error("操作失败");
        }
    }

    /**
     * 判断是否有字段被修改
     */
    private boolean hasChanges(SysUser user) {
        return user.getUserName() != null ||
                user.getTrueName() != null ||
                user.getPassword() != null ||
                user.getSalt() != null ||
                user.getAvatar() != null ||
                user.getEmail() != null||
                user.getSex() != null;
    }
    private boolean checkUserNameExist(String userName, Long excludeId) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, userName)
                .ne(excludeId != null, SysUser::getId, excludeId)) != null;
    }
    @Transactional
    @Override
    public ResultDTO updateUserPhone(Long userId, String newPhone) {
        try {
            // 参数校验
            if (userId == null || StringUtils.isBlank(newPhone)) {
                return ResultDTO.error("参数错误");
            }

            // 检查用户是否存在
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                return ResultDTO.error("用户不存在");
            }

            // 检查新手机号是否已被其他用户使用
            SysUser existingUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, newPhone)
                    .ne(SysUser::getId, userId));

            if (existingUser != null) {
                return ResultDTO.error("该手机号已被其他用户使用");
            }

            // 更新手机号
            SysUser updateUser = new SysUser();
            updateUser.setId(userId);
            updateUser.setPhone(newPhone);
            updateUser.setUpdateTime(new Date());

            int result = sysUserMapper.updateById(updateUser);
            if (result > 0) {
                log.info("用户{}手机号更新成功，新手机号：{}", userId, newPhone);
                return ResultDTO.success("手机号修改成功");
            } else {
                return ResultDTO.error("手机号修改失败");
            }

        } catch (Exception e) {
            log.error("修改用户手机号异常，userId: {}, newPhone: {}", userId, newPhone, e);
            return ResultDTO.error("系统异常，请稍后重试");
        }
    }
}