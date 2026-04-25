package org.freeplane.plugin.ai.buffer.mindmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.ai.buffer.BufferResponse;

import java.util.Map;

/**
 * 思维导图结果优化器。
 * 负责 JSON 格式验证、质量评估、结果优化。
 */
public class MindMapResultOptimizer {

    private final ObjectMapper objectMapper;

    public MindMapResultOptimizer() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 优化 AI 返回的结果
     * @param aiResponse AI 原始响应
     * @param response 响应对象（用于记录日志和评分）
     * @return 优化后的结果数据
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> optimizeResult(String aiResponse, BufferResponse response) {
        try {
            // 1. 清理 AI 响应
            String cleanedJson = cleanAIResponse(aiResponse);
            response.addLog("AI 响应清理完成: " + cleanedJson.length() + " 字符");

            // 2. JSON 格式验证
            Map<String, Object> mindMapData = validateAndParseJSON(cleanedJson);
            response.addLog("JSON 格式验证通过");

            // 3. 质量评估
            double qualityScore = assessQuality(mindMapData);
            response.setQualityScore(qualityScore);
            response.addLog("质量评分: " + qualityScore);

            // 4. 结果优化
            Map<String, Object> optimizedData = optimizeMindMapData(mindMapData);
            response.addLog("结果优化完成");

            return optimizedData;

        } catch (Exception e) {
            LogUtils.warn("MindMapResultOptimizer: optimization failed", e);
            response.addLog("优化失败: " + e.getMessage());
            response.setQualityScore(0.0);
            return null;
        }
    }

    /**
     * 清理 AI 响应，移除 Markdown 标记等
     */
    private String cleanAIResponse(String response) {
        String cleaned = response.trim();

        // 移除 Markdown 代码块标记
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * 验证并解析 JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> validateAndParseJSON(String json) throws Exception {
        try {
            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            // 验证基本结构
            if (!data.containsKey("text")) {
                throw new IllegalArgumentException("JSON 缺少 'text' 字段");
            }

            return data;
        } catch (Exception e) {
            LogUtils.warn("MindMapResultOptimizer: invalid JSON format", e);
            throw new IllegalArgumentException("JSON 格式无效: " + e.getMessage(), e);
        }
    }

    /**
     * 评估结果质量
     */
    @SuppressWarnings("unchecked")
    private double assessQuality(Map<String, Object> mindMapData) {
        double score = 50.0; // 基础分

        // 1. 节点数量评分 (0-20分)
        int nodeCount = countNodes(mindMapData);
        if (nodeCount >= 5) {
            score += 20.0;
        } else if (nodeCount >= 3) {
            score += 15.0;
        } else if (nodeCount >= 1) {
            score += 10.0;
        }

        // 2. 层级深度评分 (0-15分)
        int maxDepth = calculateMaxDepth(mindMapData);
        if (maxDepth >= 3) {
            score += 15.0;
        } else if (maxDepth >= 2) {
            score += 10.0;
        } else if (maxDepth >= 1) {
            score += 5.0;
        }

        // 3. 内容质量评分 (0-15分)
        if (hasMeaningfulContent(mindMapData)) {
            score += 15.0;
        }

        return Math.min(100.0, score);
    }

    /**
     * 统计节点数量
     */
    @SuppressWarnings("unchecked")
    private int countNodes(Map<String, Object> nodeData) {
        int count = 1; // 当前节点

        java.util.List<Map<String, Object>> children =
            (java.util.List<Map<String, Object>>) nodeData.get("children");

        if (children != null) {
            for (Map<String, Object> child : children) {
                count += countNodes(child);
            }
        }

        return count;
    }

    /**
     * 计算最大深度
     */
    @SuppressWarnings("unchecked")
    private int calculateMaxDepth(Map<String, Object> nodeData) {
        int maxDepth = 0;

        java.util.List<Map<String, Object>> children =
            (java.util.List<Map<String, Object>>) nodeData.get("children");

        if (children != null && !children.isEmpty()) {
            maxDepth = 1;
            for (Map<String, Object> child : children) {
                int childDepth = calculateMaxDepth(child);
                maxDepth = Math.max(maxDepth, 1 + childDepth);
            }
        }

        return maxDepth;
    }

    /**
     * 检查是否有有意义的内容
     */
    private boolean hasMeaningfulContent(Map<String, Object> nodeData) {
        String text = (String) nodeData.get("text");
        return text != null && text.trim().length() > 2;
    }

    /**
     * 优化思维导图数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> optimizeMindMapData(Map<String, Object> mindMapData) {
        // 1. 清理空节点
        removeEmptyNodes(mindMapData);

        // 2. 标准化文本
        normalizeText(mindMapData);

        return mindMapData;
    }

    /**
     * 移除空节点
     */
    @SuppressWarnings("unchecked")
    private void removeEmptyNodes(Map<String, Object> nodeData) {
        java.util.List<Map<String, Object>> children =
            (java.util.List<Map<String, Object>>) nodeData.get("children");

        if (children != null) {
            children.removeIf(child -> {
                String text = (String) child.get("text");
                return text == null || text.trim().isEmpty();
            });

            // 递归处理子节点
            for (Map<String, Object> child : children) {
                removeEmptyNodes(child);
            }
        }
    }

    /**
     * 标准化文本
     */
    @SuppressWarnings("unchecked")
    private void normalizeText(Map<String, Object> nodeData) {
        String text = (String) nodeData.get("text");
        if (text != null) {
            nodeData.put("text", text.trim());
        }

        java.util.List<Map<String, Object>> children =
            (java.util.List<Map<String, Object>>) nodeData.get("children");

        if (children != null) {
            for (Map<String, Object> child : children) {
                normalizeText(child);
            }
        }
    }
}