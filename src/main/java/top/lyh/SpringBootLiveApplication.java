package top.lyh;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@MapperScan(basePackages = "top.lyh.mapper")
@PropertySource("classpath:sms.properties")
@SpringBootApplication
public class SpringBootLiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootLiveApplication.class, args);
    }


}
