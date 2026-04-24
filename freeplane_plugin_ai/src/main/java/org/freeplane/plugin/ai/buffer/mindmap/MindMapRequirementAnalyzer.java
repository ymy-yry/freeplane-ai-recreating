package org.freeplane.plugin.ai.buffer.mindmap;

import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.ai.buffer.BufferRequest;

import java.util.regex.Pattern;

/**
 * 思维导图需求理解引擎。
 * 识别任务类型、提取参数、识别隐含需求。
 */
public class MindMapRequirementAnalyzer {

    // 任务类型识别关键词
    private static final String[] GENERATION_KEYWORDS = {
        "生成", "创建", "做", "制作", "画", "构建", "建立",
        "generate", "create", "make", "build"
    };

    private static final String[] EXPANSION_KEYWORDS = {
        "展开", "扩展", "补充", "细化", "深入",
        "expand", "extend", "detail"
    };

    private static final String[] SUMMARY_KEYWORDS = {
        "摘要", "总结", "概括", "提炼", "归纳",
        "summary", "summarize"
    };

    /**
     * 分析用户需求
     * @param request 请求对象
     */
    public void analyze(BufferRequest request) {
        String input = request.getUserInput();
        if (input == null || input.trim().isEmpty()) {
            LogUtils.warn("MindMapRequirementAnalyzer: empty input");
            return;
        }

        // 1. 识别任务类型
        BufferRequest.RequestType requestType = identifyRequestType(input);
        request.setRequestType(requestType);
        LogUtils.info("MindMapRequirementAnalyzer: identified request type - " + requestType);

        // 2. 提取主题
        String topic = extractTopic(input);
        request.addParameter("topic", topic);
        LogUtils.info("MindMapRequirementAnalyzer: extracted topic - " + topic);

        // 3. 提取层级深度
        int maxDepth = extractMaxDepth(input);
        request.addParameter("maxDepth", maxDepth);
        LogUtils.info("MindMapRequirementAnalyzer: extracted maxDepth - " + maxDepth);

        // 4. 识别语言
        String language = detectLanguage(input);
        request.addParameter("language", language);
        LogUtils.info("MindMapRequirementAnalyzer: detected language - " + language);

        // 5. 识别隐含需求
        identifyImplicitNeeds(request);
    }

    /**
     * 识别请求类型
     */
    private BufferRequest.RequestType identifyRequestType(String input) {
        String lowerInput = input.toLowerCase();

        // 检查是否包含展开关键词
        for (String keyword : EXPANSION_KEYWORDS) {
            if (lowerInput.contains(keyword.toLowerCase())) {
                return BufferRequest.RequestType.NODE_EXPANSION;
            }
        }

        // 检查是否包含摘要关键词
        for (String keyword : SUMMARY_KEYWORDS) {
            if (lowerInput.contains(keyword.toLowerCase())) {
                return BufferRequest.RequestType.BRANCH_SUMMARY;
            }
        }

        // 默认为生成类型
        for (String keyword : GENERATION_KEYWORDS) {
            if (lowerInput.contains(keyword.toLowerCase())) {
                return BufferRequest.RequestType.MINDMAP_GENERATION;
            }
        }

        // 默认返回思维导图生成
        return BufferRequest.RequestType.MINDMAP_GENERATION;
    }

    /**
     * 提取主题
     */
    private String extractTopic(String input) {
        // 移除常见的前缀词
        String topic = input
            .replaceAll("(?i)(帮我|给我|请|为我)\\s*(生成|创建|做|制作|画|构建|建立)", "")
            .replaceAll("(?i)(generate|create|make|build)\\s*(a|an|the)?", "")
            .trim();

        // 如果主题仍然为空，使用原始输入
        if (topic.isEmpty()) {
            topic = input;
        }

        return topic;
    }

    /**
     * 提取最大深度
     */
    private int extractMaxDepth(String input) {
        // 尝试匹配数字
        Pattern pattern = Pattern.compile("(\\d+)\\s*层");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            try {
                int depth = Integer.parseInt(matcher.group(1));
                return Math.max(1, Math.min(10, depth)); // 限制在 1-10 层
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        // 默认深度
        return 3;
    }

    /**
     * 检测语言
     */
    private String detectLanguage(String input) {
        // 简单判断：包含中文字符则为中文
        if (Pattern.compile("[\\u4e00-\\u9fa5]").matcher(input).find()) {
            return "zh";
        }
        return "en";
    }

    /**
     * 识别隐含需求
     */
    private void identifyImplicitNeeds(BufferRequest request) {
        String input = request.getUserInput().toLowerCase();

        // 如果提到"详细"，增加深度
        if (input.contains("详细") || input.contains("detailed")) {
            int currentDepth = request.getParameter("maxDepth", 3);
            request.addParameter("maxDepth", Math.min(currentDepth + 1, 10));
        }

        // 如果提到"简单"，减少深度
        if (input.contains("简单") || input.contains("simple")) {
            int currentDepth = request.getParameter("maxDepth", 3);
            request.addParameter("maxDepth", Math.max(currentDepth - 1, 1));
        }
    }
}