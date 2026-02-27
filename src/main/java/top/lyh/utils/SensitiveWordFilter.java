package top.lyh.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Component
public class SensitiveWordFilter {

    /**
     * 敏感词根节点
     */
    private final Map<Character, Object> rootNode = new HashMap<>();

    /**
     * 默认替换字符
     */
    private static final String DEFAULT_REPLACE_CHAR = "***";

    /**
     * 添加敏感词到DFA树
     */
    public void addSensitiveWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        Map<Character, Object> currentNode = rootNode;
        char[] chars = word.trim().toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            Object node = currentNode.get(c);

            if (node == null) {
                // 创建新节点
                Map<Character, Object> newNode = new HashMap<>();
                newNode.put('\0', false); // 使用特殊字符作为结束标记
                currentNode.put(c, newNode);
                currentNode = newNode;
            } else {
                currentNode = (Map<Character, Object>) node;
            }

            // 最后一个字符标记为结束
            if (i == chars.length - 1) {
                currentNode.put('\0', true);
            }
        }
    }

    /**
     * 批量添加敏感词
     */
    public void addSensitiveWords(Collection<String> words) {
        if (CollectionUtils.isEmpty(words)) {
            return;
        }

        for (String word : words) {
            addSensitiveWord(word);
        }
        log.info("成功加载 {} 个敏感词到DFA树", words.size());
    }

    /**
     * 检查是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (checkSensitiveWord(chars, i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取文本中的所有敏感词
     */
    public Set<String> getSensitiveWords(String text) {
        Set<String> sensitiveWords = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return sensitiveWords;
        }

        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            String sensitiveWord = getSensitiveWord(chars, i);
            if (sensitiveWord != null) {
                sensitiveWords.add(sensitiveWord);
            }
        }

        return sensitiveWords;
    }

    /**
     * 过滤敏感词（替换为***）
     */
    public String filterSensitiveWords(String text) {
        return filterSensitiveWords(text, DEFAULT_REPLACE_CHAR);
    }

    /**
     * 过滤敏感词（自定义替换字符）
     */
    public String filterSensitiveWords(String text, String replaceChar) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        int i = 0;

        while (i < chars.length) {
            int length = getSensitiveWordLength(chars, i);
            if (length > 0) {
                // 发现敏感词，进行替换
                result.append(replaceChar);
                i += length;
            } else {
                // 正常字符，直接添加
                result.append(chars[i]);
                i++;
            }
        }

        return result.toString();
    }

    /**
     * 检查从指定位置开始是否存在敏感词
     */
    private boolean checkSensitiveWord(char[] chars, int beginIndex) {
        Map<Character, Object> currentNode = rootNode;

        for (int i = beginIndex; i < chars.length; i++) {
            char c = chars[i];
            Object node = currentNode.get(c);

            if (node == null) {
                return false;
            }

            currentNode = (Map<Character, Object>) node;

            // 如果是结束节点，说明匹配到了敏感词
            if ((Boolean) currentNode.get('\0')) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取从指定位置开始的敏感词
     */
    private String getSensitiveWord(char[] chars, int beginIndex) {
        Map<Character, Object> currentNode = rootNode;
        StringBuilder word = new StringBuilder();

        for (int i = beginIndex; i < chars.length; i++) {
            char c = chars[i];
            Object node = currentNode.get(c);

            if (node == null) {
                return null;
            }

            word.append(c);
            currentNode = (Map<Character, Object>) node;

            // 如果是结束节点，返回匹配到的敏感词
            if ((Boolean) currentNode.get('\0')) {
                return word.toString();
            }
        }

        return null;
    }

    /**
     * 获取敏感词长度
     */
    private int getSensitiveWordLength(char[] chars, int beginIndex) {
        Map<Character, Object> currentNode = rootNode;
        int length = 0;

        for (int i = beginIndex; i < chars.length; i++) {
            char c = chars[i];
            Object node = currentNode.get(c);

            if (node == null) {
                break;
            }

            length++;
            currentNode = (Map<Character, Object>) node;

            // 如果是结束节点，返回敏感词长度
            if ((Boolean) currentNode.get('\0')) {
                return length;
            }
        }

        return 0;
    }

    /**
     * 清空敏感词库
     */
    public void clear() {
        rootNode.clear();
        log.info("敏感词库已清空");
    }

    /**
     * 获取敏感词总数
     */
    public int getWordCount() {
        return countWords(rootNode);
    }

    private int countWords(Map<Character, Object> node) {
        int count = 0;
        for (Map.Entry<Character, Object> entry : node.entrySet()) {
            if (entry.getKey() != '\0') {
                @SuppressWarnings("unchecked")
                Map<Character, Object> childNode = (Map<Character, Object>) entry.getValue();
                count += countWords(childNode);
            }
        }
        if (node.get('\0') != null && (Boolean) node.get('\0')) {
            count++;
        }
        return count;
    }
}
