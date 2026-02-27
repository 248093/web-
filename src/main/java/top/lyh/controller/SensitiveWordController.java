package top.lyh.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.lyh.common.PageResult;
import top.lyh.common.ResultDTO;
import top.lyh.common.ResponseCodeEnum;
import top.lyh.entity.pojo.SensitiveWord;
import top.lyh.service.SensitiveWordService;
import top.lyh.utils.SensitiveWordFilter;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/sensitive-word")
public class SensitiveWordController {
    
    @Autowired
    private SensitiveWordService sensitiveWordService;
    
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;
    
    /**
     * 添加敏感词
     */
    @RequiresRoles("ADMIN")
    @PostMapping
    public ResultDTO addSensitiveWord(@RequestBody SensitiveWord sensitiveWord) {
        try {
            boolean result = sensitiveWordService.addSensitiveWord(sensitiveWord);
            return result ? 
                ResultDTO.success("添加敏感词成功") : 
                ResultDTO.error("添加敏感词失败");
        } catch (Exception e) {
            log.error("添加敏感词异常", e);
            return ResultDTO.error("添加敏感词失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量添加敏感词
     */
    @RequiresRoles("ADMIN")
    @PostMapping("/batch")
    public ResultDTO batchAddSensitiveWords(@RequestBody List<SensitiveWord> sensitiveWords) {
        try {
            boolean result = sensitiveWordService.batchAddSensitiveWords(sensitiveWords);
            return result ? 
                ResultDTO.success("批量添加敏感词成功") : 
                ResultDTO.error("批量添加敏感词失败");
        } catch (Exception e) {
            log.error("批量添加敏感词异常", e);
            return ResultDTO.error("批量添加敏感词失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除敏感词
     */
    @RequiresRoles("ADMIN")
    @DeleteMapping("/{id}")
    public ResultDTO deleteSensitiveWord(@PathVariable Long id) {
        try {
            boolean result = sensitiveWordService.deleteSensitiveWord(id);
            return result ? 
                ResultDTO.success("删除敏感词成功") : 
                ResultDTO.error("删除敏感词失败");
        } catch (Exception e) {
            log.error("删除敏感词异常", e);
            return ResultDTO.error("删除敏感词失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新敏感词
     */
    @RequiresRoles("ADMIN")
    @PutMapping
    public ResultDTO updateSensitiveWord(@RequestBody SensitiveWord sensitiveWord) {
        try {
            boolean result = sensitiveWordService.updateSensitiveWord(sensitiveWord);
            return result ? 
                ResultDTO.success("更新敏感词成功") : 
                ResultDTO.error("更新敏感词失败");
        } catch (Exception e) {
            log.error("更新敏感词异常", e);
            return ResultDTO.error("更新敏感词失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询敏感词列表
     */
    @RequiresRoles("ADMIN")
    @GetMapping("/list")
    public ResultDTO getSensitiveWordList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String word,
            @RequestParam(required = false) Integer type) {
        try {
            LambdaQueryWrapper<SensitiveWord> queryWrapper = new LambdaQueryWrapper<>();
            if (word != null){
                queryWrapper.like(SensitiveWord::getWord, word);
            }
            if (type != null){
                queryWrapper.eq(SensitiveWord::getType, type);
            }
            Page<SensitiveWord> pageF = new Page<>(page, size);
            IPage<SensitiveWord> pageResult = sensitiveWordService.page(pageF, queryWrapper);
            PageResult<SensitiveWord> pageResultDTO = new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), (int) pageF.getCurrent(), (int) pageF.getSize());

            // 简单实现，实际项目中应该使用分页查询
            return ResultDTO.success("查询成功", pageResultDTO);
        } catch (Exception e) {
            log.error("查询敏感词列表异常", e);
            return ResultDTO.error("查询敏感词列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 重新加载敏感词库
     */
    @RequiresRoles("ADMIN")
    @PostMapping("/reload")
    public ResultDTO reloadSensitiveWords() {
        try {
            sensitiveWordService.loadSensitiveWords();
            return ResultDTO.success("重新加载敏感词库成功");
        } catch (Exception e) {
            log.error("重新加载敏感词库异常", e);
            return ResultDTO.error("重新加载敏感词库失败：" + e.getMessage());
        }
    }
    
    /**
     * 测试敏感词过滤
     */
    @RequiresRoles("ADMIN")
    @PostMapping("/test")
    public ResultDTO testSensitiveWordFilter(@RequestParam String text) {
        try {
            boolean contains = sensitiveWordFilter.containsSensitiveWord(text);
            Set<String> words = sensitiveWordFilter.getSensitiveWords(text);
            String filteredText = sensitiveWordFilter.filterSensitiveWords(text);
            
            return ResultDTO.success("测试完成", new TestResult(contains, words, filteredText));
        } catch (Exception e) {
            log.error("测试敏感词过滤异常", e);
            return ResultDTO.error("测试失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取词库统计信息
     */
    @RequiresRoles("ADMIN")
    @GetMapping("/stats")
    public ResultDTO getStats() {
        try {
            int totalCount = sensitiveWordFilter.getWordCount();
            return ResultDTO.success("获取统计信息成功", 
                new StatsResult(totalCount));
        } catch (Exception e) {
            log.error("获取统计信息异常", e);
            return ResultDTO.error("获取统计信息失败：" + e.getMessage());
        }
    }
    
    // 内部类用于返回测试结果
    @Data
    static class TestResult {
        private boolean containsSensitiveWord;
        private Set<String> foundWords;
        private String filteredText;
        
        public TestResult(boolean contains, Set<String> words, String filtered) {
            this.containsSensitiveWord = contains;
            this.foundWords = words;
            this.filteredText = filtered;
        }
    }
    
    // 内部类用于返回统计信息
    @Data
    static class StatsResult {
        private int totalWords;
        
        public StatsResult(int total) {
            this.totalWords = total;
        }
    }
}
