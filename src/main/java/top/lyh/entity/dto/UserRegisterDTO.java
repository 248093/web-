package top.lyh.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import top.lyh.validatio.PhoneNumber;

import java.io.Serializable;

@Data
public class UserRegisterDTO implements Serializable {
    private static final long serialVersionUID = -1L;
    /**
     * 用户名（登录用，唯一）
     */
    @NotEmpty
    private String userName;
    /**
     * 密码（加密后存储，如 BCrypt）
     */
    @NotEmpty
    private String password;
    /**
     * 手机号
     */
    @PhoneNumber
    private String phone;
    /**
     * 手机验证码
     */
    private String phoneCode;
    /**
     * 真实姓名
     */
    @NotEmpty
    private String trueName;
    /**
     * 身份证号
     */
    @NotEmpty
    private String idNumber;
    /**
     * 角色编码（Shiro 授权用）
     */
    private String roleCode;
}
