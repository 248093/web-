package top.lyh.aop;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.lyh.anno.LogAnnotation;
import top.lyh.entity.pojo.OperationLog;
import top.lyh.entity.pojo.SysUser;
import top.lyh.service.OperationLogService;
import top.lyh.service.SysUserService;
import top.lyh.utils.JwtUtil;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 日志切面类
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private SysUserService sysUserService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint point, LogAnnotation logAnnotation) throws Throwable {
        long beginTime = System.currentTimeMillis();
        
        // 执行方法
        Object result = point.proceed();
        
        // 执行时长(毫秒)
        long time = System.currentTimeMillis() - beginTime;
        
        // 保存日志
        saveLog(point, logAnnotation, time, result);
        
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, LogAnnotation logAnnotation, long time, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        OperationLog operationLog = new OperationLog();
        
        // 获取注解上的描述
        operationLog.setOperation(logAnnotation.value());
        
        // 请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        operationLog.setApiUrl(className + "." + methodName + "()");
        
        // 请求的参数
        if (logAnnotation.recordParams()) {
            Object[] args = joinPoint.getArgs();
            try {
                String params = JSON.toJSONString(args);
                operationLog.setParams(params.length() > 1000 ? params.substring(0, 1000) : params);
            } catch (Exception e) {
                operationLog.setParams("参数序列化失败");
            }
        }
        
        // 响应结果
        if (logAnnotation.recordResult()) {
            try {
                String resultStr = JSON.toJSONString(result);
                operationLog.setResult(resultStr.length() > 1000 ? resultStr.substring(0, 1000) : resultStr);
            } catch (Exception e) {
                operationLog.setResult("结果序列化失败");
            }
        }
        
        // 获取请求相关信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // 设置IP地址
            operationLog.setIp(getIpAddress(request));
            
            // 设置请求方法
            operationLog.setMethod(request.getMethod());
            
            // 设置请求头
            operationLog.setHeaders(JSON.toJSONString(request.getHeaderNames()));
            
            // 设置User-Agent
            operationLog.setUserAgent(request.getHeader("User-Agent"));
            
            // 获取当前用户信息
            String token = request.getHeader(JwtUtil.HEADER);
            if (token != null && !token.isEmpty()) {
                try {
                    Claims claims = jwtUtil.getClaimsByToken(token);
                    String username = claims.getSubject();
                    SysUser user = sysUserService.findByUsername(username);
                    if (user != null) {
                        operationLog.setUserId(user.getId());
                        operationLog.setUserName(user.getUserName());
                    }
                } catch (Exception e) {
                    log.warn("解析token获取用户信息失败: {}", e.getMessage());
                }
            }
        }
        
        operationLog.setCostTime(time);
        operationLog.setCreateTime(new Date());
        
        // 异步保存日志
        operationLogService.saveOperationLog(operationLog);
    }

    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
