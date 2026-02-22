package top.lyh;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan(basePackages = "top.lyh.mapper")
@PropertySource("classpath:sms.properties")
@SpringBootApplication
@EnableScheduling  // 启用定时任务
public class SpringBootLiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootLiveApplication.class, args);
    }


}
