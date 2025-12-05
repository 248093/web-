package top.lyh.exceptionHandler;

import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lyh.common.ResponseCodeEnum;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException {
    private ResponseCodeEnum responseCode;

    public BaseException(ResponseCodeEnum responseCode, String message) {
        super(message);

        setResponseCode(responseCode);
    }
}