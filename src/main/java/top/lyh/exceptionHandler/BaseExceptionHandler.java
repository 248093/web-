package top.lyh.exceptionHandler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.common.ResultDTO;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class BaseExceptionHandler {

    /**
     * 处理BaseException
     *
     * @param response
     * @param e
     * @return
     */
    @ExceptionHandler(BaseException.class)
    public ResultDTO handlerGlobalException(HttpServletResponse response, BaseException e) {
        log.error("请求异常：", e);
        response.setStatus(e.getResponseCode().getCode());

        return ResultDTO.error(e.getResponseCode(), e);
    }

    /**
     * 处理BindException
     *
     * @param e
     * @return
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultDTO handlerBindException(BindException e) {
        log.error("请求异常：", e);
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();
        assert fieldError != null;
        String defaultMessage = fieldError.getDefaultMessage();

        return ResultDTO.error(ResponseCodeEnum.BAD_REQUEST, defaultMessage);
    }

    /**
     * 处理Exception
     *
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultDTO handlerException(Exception e) {
        log.error("请求异常：", e);

        return ResultDTO.error(ResponseCodeEnum.ERROR, e);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", "权限不足: " + e.getMessage());
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(403).body(result);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<?> handleAuthorization(AuthorizationException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("message", "授权失败: " + e.getMessage());
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(403).body(result);
    }

}