package top.lyh.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginDTO implements Serializable {
    private static final long serialVersionUID = -1L;
    /**
     * 用户名（登录用，唯一）
     */
    private String userName;
    /**
     * 密码（加密后存储，如 BCrypt）
     */
    private String password;
}
