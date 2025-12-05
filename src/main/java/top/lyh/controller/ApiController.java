package top.lyh.controller;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

      // 未登录回调（Shiro 会自动转发到这里）
    @GetMapping("/unauth")
    public ResponseEntity<?> unauth() {
        return ResponseEntity.status(401).body("请先登录");
    }

    // 权限/角色不足回调
    @GetMapping("/unauthorized")
    public ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(403).body("权限不足");
    }
    @GetMapping("/admin")
    @RequiresRoles("ADMIN")
    public ResponseEntity<?> admin() {
        return ResponseEntity.ok("欢迎管理员");
    }
 
    @GetMapping("/user")
    @RequiresPermissions("sys:user:view")
    public ResponseEntity<?> user() {
        return ResponseEntity.ok("用户信息");
    }
}