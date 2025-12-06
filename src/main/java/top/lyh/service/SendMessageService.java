package top.lyh.service;

import org.springframework.stereotype.Service;
import top.lyh.common.ResultDTO;
import top.lyh.validatio.PhoneNumber;

@Service
public interface SendMessageService {
    public ResultDTO sendMessage(@PhoneNumber String phone);
}
