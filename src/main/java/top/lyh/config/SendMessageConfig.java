package top.lyh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "lyh.sendmessage")
public class SendMessageConfig {
    private String serverPort;
    private String accountSId;
    private String accountToken;
    private String appId;
    private String templateId;
}