package org.freeplane.plugin.ai.buffer.mindmap;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.ai.buffer.BufferRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 思维导图模型路由器。
 * 维护模型能力画像，实现评分算法，智能选择最优模型。
 */
public class MindMapModelRouter {

    /** 模型能力画像 */
    private static class ModelProfile {
        String modelName;
        String providerName;
        double qualityScore;    // 质量评分 (0-100)
        double speedScore;      // 速度评分 (0-100)
        double costScore;       // 成本评分 (0-100, 越高越便宜)
        boolean supportsJson;   // 是否支持 JSON 输出
        boolean supportsChinese; // 是否支持中文

        ModelProfile(String modelName, String providerName, double quality, double speed, double cost) {
            this.modelName = modelName;
            this.providerName = providerName;
            this.qualityScore = quality;
            this.speedScore = speed;
            this.costScore = cost;
            this.supportsJson = true;
            this.supportsChinese = true;
        }
    }

    private final List<ModelProfile> availableModels;

    public MindMapModelRouter() {
        this.availableModels = new ArrayList<>();
        initializeModelProfiles();
    }

    /**
     * 初始化模型能力画像
     */
    private void initializeModelProfiles() {
        // OpenAI GPT-4o - 高质量，适合思维导图生成
        availableModels.add(new ModelProfile(
            "openai/gpt-4o", "openrouter",
            95.0, 80.0, 60.0
        ));

        // Google Gemini - 平衡性能和成本
        availableModels.add(new ModelProfile(
            "gemini-2.0-flash", "gemini",
            85.0, 90.0, 80.0
        ));

        // DashScope Qwen - 中文支持好
        availableModels.add(new ModelProfile(
            "qwen-max", "dashscope",
            88.0, 85.0, 75.0
        ));

        // ERNIE - 中文优化
        availableModels.add(new ModelProfile(
            "ernie-4.5", "ernie",
            82.0, 75.0, 70.0
        ));

        LogUtils.info("MindMapModelRouter: initialized " + availableModels.size() + " model profiles");
    }

    /**
     * 选择最优模型
     * @param request 请求对象
     * @return 模型选择结果 (providerName/modelName)
     */
    public String selectBestModel(BufferRequest request) {
        // 过滤可用的模型（检查 API Key 配置）
        List<ModelProfile> usableModels = filterUsableModels();

        if (usableModels.isEmpty()) {
            LogUtils.warn("MindMapModelRouter: no usable models available");
            return null;
        }

        // 计算每个模型的得分
        Map<String, Double> modelScores = new HashMap<>();
        for (ModelProfile profile : usableModels) {
            double score = calculateScore(profile, request);
            modelScores.put(profile.providerName + "/" + profile.modelName, score);
            LogUtils.info("MindMapModelRouter: model " + profile.modelName + " scored " + score);
        }

        // 选择得分最高的模型
        String bestModel = null;
        double highestScore = -1.0;

        for (Map.Entry<String, Double> entry : modelScores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                bestModel = entry.getKey();
            }
        }

        LogUtils.info("MindMapModelRouter: selected best model - " + bestModel + " (score: " + highestScore + ")");
        return bestModel;
    }

    /**
     * 过滤可用的模型
     */
    private List<ModelProfile> filterUsableModels() {
        ResourceController rc = ResourceController.getResourceController();
        List<ModelProfile> usable = new ArrayList<>();

        for (ModelProfile profile : availableModels) {
            boolean hasKey = false;
            switch (profile.providerName) {
                case "openrouter":
                    hasKey = !rc.getProperty("ai_openrouter_key", "").trim().isEmpty();
                    break;
                case "gemini":
                    hasKey = !rc.getProperty("ai_gemini_key", "").trim().isEmpty();
                    break;
                case "dashscope":
                    hasKey = !rc.getProperty("ai_dashscope_key", "").trim().isEmpty();
                    break;
                case "ernie":
                    hasKey = !rc.getProperty("ai_ernie_key", "").trim().isEmpty();
                    break;
            }

            if (hasKey) {
                usable.add(profile);
            }
        }

        return usable;
    }

    /**
     * 计算模型得分
     */
    private double calculateScore(ModelProfile profile, BufferRequest request) {
        double score = 0.0;

        // 权重配置
        double qualityWeight = 0.5;  // 质量权重 50%
        double speedWeight = 0.3;    // 速度权重 30%
        double costWeight = 0.2;     // 成本权重 20%

        // 根据请求类型调整权重
        BufferRequest.RequestType requestType = request.getRequestType();
        if (requestType == BufferRequest.RequestType.MINDMAP_GENERATION) {
            // 思维导图生成更注重质量
            qualityWeight = 0.6;
            speedWeight = 0.25;
            costWeight = 0.15;
        } else if (requestType == BufferRequest.RequestType.BRANCH_SUMMARY) {
            // 摘要更注重速度
            qualityWeight = 0.4;
            speedWeight = 0.4;
            costWeight = 0.2;
        }

        // 计算加权得分
        score = profile.qualityScore * qualityWeight +
                profile.speedScore * speedWeight +
                profile.costScore * costWeight;

        // 语言偏好加分
        String language = request.getParameter("language", "zh");
        if ("zh".equals(language) && profile.supportsChinese) {
            score += 5.0; // 中文支持加分
        }

        return score;
    }
}