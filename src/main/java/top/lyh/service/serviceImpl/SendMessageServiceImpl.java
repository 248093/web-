package top.lyh.service.serviceImpl;

import com.cloopen.rest.sdk.BodyType;
import com.cloopen.rest.sdk.CCPRestSmsSDK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.lyh.common.ResultDTO;
import top.lyh.config.SendMessageConfig;
import top.lyh.service.SendMessageService;
import top.lyh.utils.RedisUtil;
import java.util.HashMap;
import java.util.Set;

@Slf4j
@Service
@Validated
public class SendMessageServiceImpl implements SendMessageService {
    @Autowired
    private SendMessageConfig sendMessageConfig;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public ResultDTO sendMessage(String phone) {
        int code = (int) (Math.random() * 9000 + 1000);
        String serverIp = "app.cloopen.com";
        //请求端口
        String serverPort = sendMessageConfig.getServerPort();
        //主账号,登录云通讯网站后,可在控制台首页看到开发者主账号ACCOUNT SID和主账号令牌AUTH TOKEN
        String accountSId = sendMessageConfig.getAccountSId();
        String accountToken = sendMessageConfig.getAccountToken();
        //请使用管理控制台中已创建应用的APPID
        String appId = sendMessageConfig.getAppId();
        CCPRestSmsSDK sdk = new CCPRestSmsSDK();
        sdk.init(serverIp, serverPort);
        sdk.setAccount(accountSId, accountToken);
        sdk.setAppId(appId);
        sdk.setBodyType(BodyType.Type_JSON);
        String to = phone;
        String templateId = sendMessageConfig.getTemplateId();
        String[] datas = {String.valueOf(code),"1"};
        HashMap<String, Object> result = sdk.sendTemplateSMS(to, templateId, datas);
        redisUtil.set("phone:"+phone,code,60);
        if ("000000".equals(result.get("statusCode"))) {
            //正常返回输出data包体信息（map）
            HashMap<String, Object> data = (HashMap<String, Object>) result.get("data");
            Set<String> keySet = data.keySet();
            for (String key : keySet) {
                Object object = data.get(key);
                System.out.println(key + " = " + object);
            }
        } else {
            //异常返回输出错误码和错误信息
            System.out.println("错误码=" + result.get("statusCode") + " 错误信息= " + result.get("statusMsg"));
            return ResultDTO.error("发送失败");
        }
        return ResultDTO.success("发送成功");
    }
}
