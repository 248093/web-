package top.lyh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.UserLoginDTO;
import top.lyh.entity.dto.UserRegisterDTO;
import top.lyh.entity.pojo.SysUser;
import top.lyh.service.SendMessageService;
import top.lyh.service.SysUserService;
import top.lyh.utils.JwtUtil;
import top.lyh.validatio.PhoneNumber;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SendMessageService sendMessageService;
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
        result.put("username", user.getUserName());
        result.put("userId", user.getId());
        response.setHeader(JwtUtil.HEADER, token);
        return ResultDTO.success("登录成功", result);
    }
    @RequiresAuthentication
    @GetMapping("/logout")
    public ResultDTO logout() {
        // 退出登录
        SecurityUtils.getSubject().logout();
        return ResultDTO.success();
    }
    @PostMapping("/register")
    public ResultDTO register(@RequestBody @Validated UserRegisterDTO userRegisterDTO) {
        return sysUserService.addUser(userRegisterDTO);
    }
    @PutMapping("/sendMessage")
    public ResultDTO sendMessage(@PhoneNumber @RequestParam String phone)
    {
        return sendMessageService.sendMessage(phone);
    }

}
