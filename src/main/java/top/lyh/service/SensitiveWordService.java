package top.lyh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lyh.entity.pojo.SensitiveWord;

import java.util.List;

public interface SensitiveWordService extends IService<SensitiveWord> {
    
    /**
     * 加载所有启用的敏感词到内存
     */
    void loadSensitiveWords();
    
    /**
     * 添加敏感词
     */
    boolean addSensitiveWord(SensitiveWord sensitiveWord);
    
    /**
     * 批量添加敏感词
     */
    boolean batchAddSensitiveWords(List<SensitiveWord> sensitiveWords);
    
    /**
     * 删除敏感词
     */
    boolean deleteSensitiveWord(Long id);
    
    /**
     * 更新敏感词
     */
    boolean updateSensitiveWord(SensitiveWord sensitiveWord);
    
    /**
     * 获取所有启用的敏感词列表
     */
    List<String> getAllEnabledWords();
}
