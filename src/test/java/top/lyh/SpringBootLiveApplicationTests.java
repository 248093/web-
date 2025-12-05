package top.lyh;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.lyh.entity.pojo.SysUser;
import top.lyh.mapper.SysUserMapper;

@SpringBootTest
class SpringBootLiveApplicationTests {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void contextLoads() {
    }
    @Test
    public void run(String... args) throws Exception {
        // 测试调用 Mapper 方法
        SysUser user = sysUserMapper.selectById(1L); // 假设存在 ID=1 的用户
        System.out.println("测试查询用户：" + user);
    }
}
