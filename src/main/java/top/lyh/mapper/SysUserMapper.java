package top.lyh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.lyh.entity.pojo.SysUser;

import java.util.List;

/**
 * 系统用户 Mapper（关联 sys_user 表）
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 通过用户名查询用户（登录用）
     * @param userName 用户名
     * @return 用户实体
     */
    SysUser selectByUserName(@Param("userName") String userName);

    /**
     * 通过手机号查询用户（注册/登录用）
     * @param phone 手机号
     * @return 用户实体
     */
    SysUser selectByPhone(@Param("phone") String phone);

    /**
     * 批量查询用户（用于管理员分页）
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    List<SysUser> selectBatchByIds(@Param("userIds") List<Long> userIds);
}