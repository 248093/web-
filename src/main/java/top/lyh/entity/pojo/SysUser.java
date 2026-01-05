package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 系统用户实体类（对应 sys_user 表）
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 用户ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
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
    @JsonIgnore
    private String password;

    /**
     * 账号状态：1-启用，0-禁用
     */
    private Integer enabled;

    /**
     * 真实姓名
     */
    @JsonIgnore
    private String trueName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别（对应数据库 sex 枚举字段）
     * 类型用 String 匹配数据库 ENUM('1','2') 的字符串类型
     */
    private String sex;

    /**
     * 性别枚举（内部工具类，用于统一管理性别编码）
     * 修正：code 改为 String 类型，匹配数据库枚举值
     */
    public enum SexEnum {
        MALE("1", "男"),
        FEMALE("2", "女");

        // 数据库存储的枚举值（字符串类型）
        private final String code;
        // 便于前端展示的描述（可选）
        private final String desc;

        SexEnum(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        // 新增：根据code反向获取枚举（查询后转换用）
        public static SexEnum getByCode(String code) {
            for (SexEnum sex : values()) {
                if (sex.code.equals(code)) {
                    return sex;
                }
            }
            return null; // 或抛出异常
        }
    }

    /**
     * 身份证号
     */
    @JsonIgnore
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
     * 盐值
     */
    private String salt;

    /**
     * 更新时间
     */
    private Date updateTime;

    // 可选：新增性别枚举转换方法（方便业务层使用）
    public SexEnum getSexEnum() {
        return SexEnum.getByCode(this.sex);
    }

    public void setSexByEnum(SexEnum sexEnum) {
        this.sex = sexEnum.getCode();
    }
}