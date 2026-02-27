package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import top.lyh.common.PageResult;
import top.lyh.common.ResultDTO;
import top.lyh.entity.dto.SysUserQueryDto;
import top.lyh.entity.dto.UserRegisterDTO;
import top.lyh.entity.pojo.SysUser;
import java.util.Set;

/**
 * 仅保留 Shiro 认证授权必需的方法，去掉多余扩展
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * Shiro 认证：通过用户名查询用户（核心）
     * @param userName 用户名
     * @return 用户实体（含密码、盐值等）
     */
    SysUser findByUsername(String userName);

    /**
     * Shiro 授权：通过用户名查询角色编码集合
     * @param userName 用户名
     * @return 角色编码集合（如 {"USER", "ADMIN"}）
     */
    Set<String> findRoles(String userName);

    /**
     * Shiro 授权：通过用户名查询权限编码集合
     * @param userName 用户名
     * @return 权限编码集合（如 {"live:view", "live:create"}）
     */
    Set<String> findPermissions(String userName);
    ResultDTO addUser(UserRegisterDTO userRegisterDTO);
    public ResultDTO updateUser(SysUser user);
    ResultDTO updateUserPhone(Long userId, String newPhone);
    /**
     * 动态分页查询用户列表
     * @param queryDto 查询条件DTO
     * @return 分页结果
     */
    PageResult<SysUser> pageQueryUsers(SysUserQueryDto queryDto);
}