package top.lyh.filter;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import top.lyh.common.JwtToken;
import top.lyh.utils.JwtUtil;
import top.lyh.utils.RedisUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtFilter extends AccessControlFilter {

    private final RedisUtil redisUtil;
    private final PathMatcher pathMatcher = new AntPathMatcher(); // 路径匹配器

    // 匿名路径列表 - 支持通配符
    private final List<String> anonPaths = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/live/rooms",
            "/api/live/room/*",
            "/api/user/sendMessage",
            "/ws-live/**",           // ✅ 支持通配符
            "/api/srs/callback/**",  // ✅ 支持通配符
            "/error",
            "/favicon.ico",
            "/api/gift/weeklyRank",
            "/api/gift/list",
            "/api/live/room/*/online/increment",
            "/api/live/room/*/online/decrement",
            "/api/live/room/*/online/count",
            "/notify",
            "/api/category/tree",
            "/api/category/root",
            "/api/category/sub/**",
            "/api/category/room-count",
            "/api/category/count-list",
            "/api/room/blacklist/check"
    );

    @Autowired
    public JwtFilter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
        log.info("JwtFilter 初始化完成，RedisUtil: {}", redisUtil != null ? "已注入" : "未注入");
    }

    /**
     * 判断是否是匿名路径
     */
    private boolean isAnonPath(String path) {
        for (String pattern : anonPaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // 1. 如果是匿名路径，直接允许访问
        if (isAnonPath(path)) {
            log.debug("匿名路径允许访问: {} {}", method, path);
            return true;
        }

        // 2. OPTIONS 请求（预检请求）直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 其他路径交由 onAccessDenied 处理
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        log.info("验证Token: {} {}", request.getMethod(), request.getRequestURI());

        String token = request.getHeader(JwtUtil.HEADER);
        log.info("请求Header中的Token: {}", token);

        // 如果token为空，返回401
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token为空，拒绝访问");
            onLoginFail(response, "未提供认证Token");
            return false;
        }

        // 检查token是否在黑名单中
        if (redisUtil.hasKey("blacklist:" + token)) {
            log.warn("Token在黑名单中，拒绝访问");
            onLoginFail(response, "Token已失效");
            return false;
        }

        JwtToken jwtToken = new JwtToken(token);
        try {
            Subject subject = getSubject(servletRequest, servletResponse);
            log.info("登录前Subject isAuthenticated: {}", subject.isAuthenticated());

            subject.login(jwtToken);

            log.info("登录后Subject isAuthenticated: {}", subject.isAuthenticated());
            log.info("登录后Subject Principal: {}", subject.getPrincipal());

            return true;
        } catch (AuthenticationException e) {
            log.error("Token认证失败: {}", e.getMessage());
            onLoginFail(response, "Token无效或已过期");
            return false;
        } catch (AuthorizationException e) {
            log.error("授权失败: {}", e.getMessage());
            onLoginFail(response, "权限不足");
            return false;
        }
    }

    // 登录失败要执行的方法 - 返回JSON格式
    private void onLoginFail(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"code\":401,\"message\":\"%s\",\"success\":false,\"timestamp\":%d}",
                message, System.currentTimeMillis()
        );

        response.getWriter().write(jsonResponse);
    }
}