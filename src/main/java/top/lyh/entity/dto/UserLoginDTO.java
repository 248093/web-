package top.lyh.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import top.lyh.validatio.PhoneNumber;

import java.io.Serializable;

@Data
public class UserLoginDTO implements Serializable {
    private static final long serialVersionUID = -1L;
    /**
     * 用户名（登录用，唯一）
     */
    @NotEmpty
    @Pattern(
            regexp = "^[a-zA-Z0-9_]{3,20}$", // Java 正则（注意无反斜杠转义，除了中文）
            message = "用户名需为3-20位字母、数字或下划线"
    )
    private String userName;
    /**
     * 密码（加密后存储，如 BCrypt）
     */
    @NotEmpty
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]{1,16}$", // 核心：Java 正则表达式
            message = "密码必须是1-16位字母数字组合（需同时包含字母和数字）"
    )
    private String password;
}
