package top.lyh.common;

import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.lyh.entity.pojo.SysUser;
import top.lyh.exceptionHandler.BaseException;
import top.lyh.service.SysUserService;
import top.lyh.utils.JwtUtil;

import java.util.Set;

@Component
@Slf4j
public class UserRealm extends AuthorizingRealm {
    @Resource
    private JwtUtil jwtUtil;
 
    @Autowired
    private SysUserService sysUserService;

    /**
     * 多重写一个support
     * 标识这个Realm是专门用来验证JwtToken
     * 不负责验证其他的token（UsernamePasswordToken）
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 获取用户角色和权限
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Object principal = principals.getPrimaryPrincipal();
        String username;

        if (principal instanceof String) {
            username = (String) principal;
        } else if (principal instanceof SysUser) {
            username = ((SysUser) principal).getUserName();
        } else {
            throw new IllegalStateException("Unknown principal type: " + principal.getClass());
        }

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        // 获取用户角色和权限
        Set<String> roles = sysUserService.findRoles(username);
        Set<String> permissions = sysUserService.findPermissions(username);
        System.out.println("用户 " + username + " 的角色：" + roles);
        System.out.println("用户 " + username + " 的权限：" + permissions);
        authorizationInfo.setRoles(roles);
        authorizationInfo.setStringPermissions(permissions);
        return authorizationInfo;
    }


    /**
     * 验证用户
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String jwt = (String) token.getCredentials();
        String username = jwtUtil.getClaimsByToken(jwt).getSubject();
        // 从数据库获取用户信息
        SysUser sysUser = sysUserService.findByUsername(username);
        if (sysUser == null) {
            throw new UnknownAccountException("用户不存在");
        }
        if (!sysUser.isEnabled()) {
            throw new LockedAccountException("账户被锁定");
        }
        Claims claims = jwtUtil.getClaimsByToken(jwt);
        if (jwtUtil.isTokenExpired(claims.getExpiration())) {
            throw new BaseException(ResponseCodeEnum.BAD_REQUEST, "token过期，请重新登录");
        }
        return new SimpleAuthenticationInfo(sysUser, jwt, getName());
    }
}