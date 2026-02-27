package top.lyh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.lyh.anno.LogAnnotation;
import top.lyh.common.PageResult;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.SysUserQueryDto;
import top.lyh.entity.dto.UserLoginDTO;
import top.lyh.entity.dto.UserRegisterDTO;
import top.lyh.entity.pojo.DailyVisitStat;
import top.lyh.entity.pojo.SysUser;
import top.lyh.service.DailyVisitStatService;
import top.lyh.service.SendMessageService;
import top.lyh.service.SysUserService;
import top.lyh.utils.AliOSSUtils;
import top.lyh.utils.JwtUtil;
import top.lyh.utils.RedisUtil;
import top.lyh.validatio.PhoneNumber;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SendMessageService sendMessageService;
    @Autowired
    private DailyVisitStatService dailyVisitStatService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private AliOSSUtils aliOSSUtils;

    @PostMapping("/login")
    @LogAnnotation(value = "用户登录", recordParams = false, recordResult = true)
    public ResultDTO login(@RequestBody @Validated UserLoginDTO userLoginDTO, HttpServletResponse response) {
        // 1. 验证用户是否存在
        SysUser user = sysUserService.findByUsername(userLoginDTO.getUserName());
        if (user == null) {
            return ResultDTO.error("用户名不存在");
        }

        // 2. 验证账户状态
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            return ResultDTO.error("账户已被锁定");
        }
        // 验证账户角色是否匹配
        Set<String> roles = sysUserService.findRoles(user.getUserName());
        if (!roles.contains(userLoginDTO.getRoleCode())) {
            return ResultDTO.error("用户角色不匹配");
        }

        // 3. 验证密码
        String encryptedPassword = new SimpleHash(
                "SHA-256",
                userLoginDTO.getPassword(),
                ByteSource.Util.bytes(user.getSalt()),
                10
        ).toHex();

        if (!encryptedPassword.equals(user.getPassword())) {
            return ResultDTO.error("密码错误");
        }
        dailyVisitStatService.updateTodayVisitStat(user.getId());
        // 4. 生成JWT Token
        String token = jwtUtil.generateToken(user.getUserName());
        // 5. 构建完整的用户信息返回
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("id", user.getId());
        result.put("userName", user.getUserName());
        result.put("trueName", user.getTrueName());
        result.put("phone", user.getPhone());
        result.put("email", user.getEmail());
        result.put("sex", user.getSex());
        result.put("avatar", user.getAvatar());
        result.put("accountMoney", user.getAccountMoney());
        result.put("roleCode", userLoginDTO.getRoleCode());
        response.setHeader(JwtUtil.HEADER, token);
        return ResultDTO.success("登录成功", result);
    }

    @GetMapping("/logout")
    @RequiresAuthentication
    @LogAnnotation(value = "用户登出", recordParams = false, recordResult = true)
    public ResultDTO logout(HttpServletRequest request) {
        // 获取token
        String token = request.getHeader(JwtUtil.HEADER);

        if (token != null && !token.trim().isEmpty()) {
            try {
                // 使token立即过期（加入黑名单）
                expireTokenNow(token);
                log.info("Token已加入黑名单，标记为过期");
            } catch (Exception e) {
                log.error("标记token过期失败", e);
                // 即使失败也继续执行登出
            }
        }

        // 执行Shiro登出
        SecurityUtils.getSubject().logout();

        return ResultDTO.success("登出成功");
    }

    @PostMapping("/register")
    @LogAnnotation(value = "用户注册", recordParams = false, recordResult = true)
    public ResultDTO register(@RequestBody @Validated UserRegisterDTO userRegisterDTO) {
        return sysUserService.addUser(userRegisterDTO);
    }
    @PutMapping("/sendMessage")
    @LogAnnotation(value = "发送验证码", recordParams = false, recordResult = true)
    public ResultDTO sendMessage(@Validated @PhoneNumber @RequestParam String phone)
    {
        System.out.println(phone);
        return sendMessageService.sendMessage(phone);
    }
    public void expireTokenNow(String token) {
        try {
            // 获取token原本的过期时间
            Claims claims = jwtUtil.getClaimsByToken(token);
            Date expiration = claims.getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                // 将token存入Redis，有效期与token一致
                redisUtil.set("blacklist:"+token,"invalid", ttl);
                log.info("Token已标记为过期，剩余{}秒", ttl/1000);
            }
        } catch (Exception e) {
            log.error("标记token过期失败", e);
        }
    }
    @GetMapping("/getUserInfo")
    @RequiresAuthentication
    @LogAnnotation(value = "获取用户信息", recordParams = false, recordResult = true)
    public ResultDTO getUserInfo(@RequestParam Long userId) {
        return ResultDTO.success(sysUserService.getById(userId));
    }
    // 或者使用 JSON + 文件的方式
    @PostMapping("/updateUser")
    @RequiresAuthentication
    @LogAnnotation(value = "更新用户信息", recordParams = false, recordResult = true)
    public ResultDTO updateUser(
            @RequestBody SysUser user) {
        log.info("更新用户信息: {}", user);
        return sysUserService.updateUser(user);
    }
    @GetMapping("/check")
    @LogAnnotation(value = "用户名检查", recordParams = false, recordResult = true)
    public ResultDTO check(@RequestParam String userName) {
        if (sysUserService.findByUsername(userName) != null){
            return ResultDTO.error("用户名已存在");
        }else {
            return ResultDTO.success("用户名可用");
        }
    }
    @PostMapping("/uploadAvatar")
    @RequiresAuthentication
    @LogAnnotation(value = "上传用户头像", recordParams = false, recordResult = true)
    public ResultDTO uploadAvatar(@RequestParam MultipartFile file) {
        try {
            String url = aliOSSUtils.upload(file, "avatar");
            return ResultDTO.success("上传成功", url);
        } catch (Exception e) {
            log.error("上传失败", e);
            return ResultDTO.error("上传失败");
        }
    }
    @PostMapping("/updatePhone")
    @RequiresAuthentication
    @LogAnnotation(value = "更新手机号", recordParams = false, recordResult = true)
    public ResultDTO updatePhone(@RequestParam Long userId,
                                 @RequestParam String oldPhone,
                                 @RequestParam String newPhone,
                                 @RequestParam String phoneCode) {
        try {
            // 参数校验
            if (userId == null || StringUtils.isBlank(newPhone) || StringUtils.isBlank(phoneCode)) {
                return ResultDTO.error("参数不能为空");
            }

            // 验证手机号格式
            if (!newPhone.matches("^1[3-9]\\d{9}$")) {
                return ResultDTO.error("手机号格式不正确");
            }

            // 验证验证码
            Object codeObj = redisUtil.get("phone:" + oldPhone);
            String validCodeInRedis = codeObj != null ? codeObj.toString() : null;

            if (StringUtils.isBlank(validCodeInRedis)) {
                return ResultDTO.error("验证码不存在或已过期，请重新获取");
            }

            if (!validCodeInRedis.equals(phoneCode)) {
                return ResultDTO.error("验证码输入错误，请重新输入");
            }

            // 调用服务层方法更新手机号
            ResultDTO result = sysUserService.updateUserPhone(userId, newPhone);

            // 如果更新成功，清除验证码
            if (result.getCode() == 200) {
                redisUtil.del("phone:" + newPhone);
            }

            return result;
        } catch (Exception e) {
            log.error("修改手机号异常", e);
            return ResultDTO.error("修改手机号失败");
        }
    }
    // 获取数据库用户数量
    @GetMapping("/getUserCount")
    public ResultDTO getUserCount() {
        long count = sysUserService.count();
        return ResultDTO.success("获取用户数量成功", count);
    }
    /**
     * 分页查询用户列表（支持动态条件查询）
     * @param queryDto 查询条件
     * @return 分页结果
     */
    @PostMapping("/pageQuery")
    @LogAnnotation(value = "分页查询用户列表", recordParams = true, recordResult = true)
    @RequiresAuthentication
    public ResultDTO pageQueryUsers(@RequestBody @Valid SysUserQueryDto queryDto) {
        try {
            PageResult<SysUser> pageResult = sysUserService.pageQueryUsers(queryDto);
            return ResultDTO.success("查询成功", pageResult);
        } catch (Exception e) {
            log.error("分页查询用户列表异常", e);
            return ResultDTO.error("查询失败：" + e.getMessage());
        }
    }
    // 封禁用户
    @PutMapping("/updateStatus")
    @RequiresRoles("ADMIN")
    @LogAnnotation(value = "封禁用户", recordParams = false, recordResult = true)
    public ResultDTO updateStatus(@RequestParam Long userId, @RequestParam Integer status) {
        try {
            SysUser sysUser = new SysUser();
            sysUser.setId(userId);
            sysUser.setEnabled(status);
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUser::getId, userId);
            boolean result = sysUserService.update(sysUser, queryWrapper);
            return result ? ResultDTO.success("操作成功") : ResultDTO.error("操作失败");
        } catch (Exception e) {
            log.error("封禁用户异常", e);
            return ResultDTO.error("操作失败：" + e.getMessage());
        }
    }
}
