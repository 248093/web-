package top.lyh.entity.dto;

import jakarta.validation.constraints.NotEmpty;
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
    private String userName;
    /**
     * 密码（加密后存储，如 BCrypt）
     */
    @NotEmpty
    private String password;
}
