package top.lyh.utils;

import jakarta.validation.ConstraintValidatorContext;

/**
 * 参数校验 - 重置提示信息工具类
 */
public class ResetMessageUtil {

    /**
     * 重置提示信息
     */
    public static void reset(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

}


