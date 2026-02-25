package top.lyh.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 操作日志实体类
 */
@Data
@TableName("operation_log")
public class OperationLog {

    /**
     * 日志ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 操作描述（如：用户登录、创建直播间等）
     */
    private String operation;

    /**
     * 访问的接口URL
     */
    private String apiUrl;

    /**
     * 请求方法（GET、POST、PUT、DELETE等）
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 响应结果
     */
    private String result;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 操作时间
     */
    private Date createTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long costTime;

    /**
     * 请求头信息
     */
    private String headers;

    /**
     * 用户代理
     */
    private String userAgent;
}
