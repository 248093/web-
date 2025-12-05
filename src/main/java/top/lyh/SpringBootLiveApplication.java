package top.lyh;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = "top.lyh.mapper")
@SpringBootApplication
public class SpringBootLiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootLiveApplication.class, args);
    }


}
