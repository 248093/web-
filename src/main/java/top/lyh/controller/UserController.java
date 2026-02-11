package top.lyh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.UserLoginDTO;
import top.lyh.entity.dto.UserRegisterDTO;
import top.lyh.entity.pojo.SysUser;
import top.lyh.service.SendMessageService;
import top.lyh.service.SysUserService;
import top.lyh.utils.JwtUtil;
import top.lyh.utils.RedisUtil;
import top.lyh.validatio.PhoneNumber;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    private RedisUtil redisUtil;
    @PostMapping("/login")
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

        // 4. 生成JWT Token
        String token = jwtUtil.generateToken(user.getUserName());
        // 5. 返回登录成功信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userName", user.getUserName());
        result.put("userId", user.getId());
        response.setHeader(JwtUtil.HEADER, token);
        return ResultDTO.success("登录成功", result);
    }

    @GetMapping("/logout")
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
    public ResultDTO register(@RequestBody @Validated UserRegisterDTO userRegisterDTO) {
        return sysUserService.addUser(userRegisterDTO);
    }
    @PutMapping("/sendMessage")
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

}
