package top.lyh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.UserLoginDTO;
import top.lyh.entity.pojo.SysUser;
import top.lyh.service.SysUserService;
import top.lyh.utils.JwtUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private JwtUtil jwtUtil;
    @PostMapping("/login")
    public ResultDTO login(@RequestBody @Validated UserLoginDTO userLoginDTO, HttpServletResponse response) {
        String username = userLoginDTO.getUserName();
        String password = userLoginDTO.getPassword();

        // 根据用户名或手机号查询用户
        SysUser user = null;
        if (username != null && !username.isEmpty()) {
            user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, username));
        }
        if (user == null) {
            return ResultDTO.error("用户不存在");
        }

        if (!user.getPassword().equals(password)) {
            return ResultDTO.error("密码错误");
        }

        String token = jwtUtil.generateToken(user.getUserName());
        response.setHeader(JwtUtil.HEADER, token);
        response.setHeader("Access-Control-Expose-Headers", JwtUtil.HEADER);
        Map<String, String> map = new HashMap<>();
        map.put("token", token);
        return ResultDTO.success(map);
    }
    @RequiresAuthentication
    @GetMapping("/logout")
    public ResultDTO logout() {
        // 退出登录
        SecurityUtils.getSubject().logout();
        return ResultDTO.success();
    }

}
