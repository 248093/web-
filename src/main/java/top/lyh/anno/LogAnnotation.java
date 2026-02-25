package top.lyh.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAnnotation {
    
    /**
     * 操作描述
     */
    String value() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean recordParams() default false;
    
    /**
     * 是否记录响应结果
     */
    boolean recordResult() default true;
}
