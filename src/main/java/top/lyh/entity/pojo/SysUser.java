package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 系统用户实体类（对应 sys_user 表）
 */
@Data // Lombok 注解，自动生成 getter/setter/toString 等方法
@TableName("sys_user") // 指定数据库表名
public class SysUser {

    /**
     * 用户ID（主键，自增）
     */
    @TableId(type = IdType.AUTO) // 主键策略：自增（与表中 AUTO_INCREMENT 对应）
    private Long id;

    /**
     * 用户名（登录用，唯一）
     */
    private String userName;

    /**
     * 手机号（唯一，注册用）
     */
    private String phone;

    /**
     * 密码（加密后存储，如 BCrypt）
     */
    private String password;

    /**
     * 账号状态：1-启用，0-禁用
     */
    private Integer enabled;

    /**
     * 真实姓名
     */
    private String trueName;

    /**
     * 身份证号
     */
    private String idNumber;

    /**
     * 账户余额
     */
    private BigDecimal accountMoney;

    /**
     * 创建时间（注册时间）
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
    // Shiro 盐值：用 userName 作为盐（与你的 Realm 中 salt 逻辑一致）
    public String getCredentialsSalt() {
        return userName;
    }

    // 适配 isEnabled() 方法（你的 Realm 中判断账户是否锁定）
    public boolean isEnabled() {
        return enabled == 1;
    }
}