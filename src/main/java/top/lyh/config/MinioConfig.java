package top.lyh.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration  // 标记这是一个配置类，Spring 启动时会加载
public class MinioConfig {

    // 从 application.yml 中读取配置项
    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    // @Bean 注解：让 Spring 自动创建这个方法返回的实例，并存入容器
    @Bean
    public MinioClient minioClient() {
        // 用 MinIO 提供的 builder 模式创建实例，传入连接信息
        return MinioClient.builder()
                .endpoint(endpoint)       // MinIO 服务地址
                .credentials(accessKey, secretKey)  // 身份认证
                .build();
    }
}