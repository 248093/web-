package top.lyh.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.lyh.entity.pojo.SensitiveWord;
import top.lyh.mapper.SensitiveWordMapper;
import top.lyh.service.SensitiveWordService;
import top.lyh.utils.SensitiveWordFilter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord>
        implements SensitiveWordService {
    
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;
    
    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;
    
    /**
     * 应用启动时自动加载敏感词
     */
    @PostConstruct
    public void init() {
        loadSensitiveWords();
    }
    
    @Override
    public void loadSensitiveWords() {
        try {
            // 清空现有词库
            sensitiveWordFilter.clear();
            
            // 查询所有启用的敏感词
            LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SensitiveWord::getStatus, 1); // 只加载启用的
            
            List<SensitiveWord> sensitiveWords = sensitiveWordMapper.selectList(wrapper);
            List<String> words = sensitiveWords.stream()
                    .map(SensitiveWord::getWord)
                    .collect(Collectors.toList());
            
            // 加载到DFA树
            sensitiveWordFilter.addSensitiveWords(words);
            
            log.info("敏感词库加载完成，共加载 {} 个敏感词", words.size());
        } catch (Exception e) {
            log.error("加载敏感词库失败", e);
        }
    }
    
    @Override
    public boolean addSensitiveWord(SensitiveWord sensitiveWord) {
        try {
            sensitiveWord.setCreatedAt(LocalDateTime.now());
            sensitiveWord.setUpdatedAt(LocalDateTime.now());
            
            boolean result = save(sensitiveWord);
            if (result && sensitiveWord.getStatus() == 1) {
                // 如果是启用状态，立即加载到内存
                sensitiveWordFilter.addSensitiveWord(sensitiveWord.getWord());
            }
            
            return result;
        } catch (Exception e) {
            log.error("添加敏感词失败", e);
            return false;
        }
    }
    
    @Override
    public boolean batchAddSensitiveWords(List<SensitiveWord> sensitiveWords) {
        try {
            LocalDateTime now = LocalDateTime.now();
            sensitiveWords.forEach(word -> {
                word.setCreatedAt(now);
                word.setUpdatedAt(now);
            });
            
            boolean result = saveBatch(sensitiveWords);
            if (result) {
                // 重新加载整个词库
                loadSensitiveWords();
            }
            
            return result;
        } catch (Exception e) {
            log.error("批量添加敏感词失败", e);
            return false;
        }
    }
    
    @Override
    public boolean deleteSensitiveWord(Long id) {
        try {
            SensitiveWord sensitiveWord = getById(id);
            if (sensitiveWord != null) {
                boolean result = removeById(id);
                if (result) {
                    // 重新加载词库（简单实现，实际项目中可以优化）
                    loadSensitiveWords();
                }
                return result;
            }
            return false;
        } catch (Exception e) {
            log.error("删除敏感词失败", e);
            return false;
        }
    }
    
    @Override
    public boolean updateSensitiveWord(SensitiveWord sensitiveWord) {
        try {
            sensitiveWord.setUpdatedAt(LocalDateTime.now());
            boolean result = updateById(sensitiveWord);
            if (result) {
                // 重新加载词库
                loadSensitiveWords();
            }
            return result;
        } catch (Exception e) {
            log.error("更新敏感词失败", e);
            return false;
        }
    }
    
    @Override
    public List<String> getAllEnabledWords() {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWord::getStatus, 1);
        return list(wrapper).stream()
                .map(SensitiveWord::getWord)
                .collect(Collectors.toList());
    }
}
