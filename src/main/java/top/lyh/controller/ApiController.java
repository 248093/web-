package top.lyh.controller;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.common.ResultDTO;

@RestController
@RequestMapping("/api")
public class ApiController {

    // 未登录回调（Shiro 会自动转发到这里）
    @GetMapping("/unauth")
    public ResultDTO unauth() {
        return ResultDTO.error(ResponseCodeEnum.UNAUTHORIZED, "请先登录");
    }

    // 权限/角色不足回调
    @GetMapping("/unauthorized")
    public ResultDTO unauthorized() {
        return ResultDTO.error(ResponseCodeEnum.FORBIDDEN, "权限不足");
    }

    @GetMapping("/admin")
    @RequiresRoles("ADMIN")
    public ResultDTO admin() {
        return ResultDTO.success("欢迎管理员");
    }
}
