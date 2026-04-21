package org.freeplane.plugin.ai.service.impl;

import dev.langchain4j.model.output.TokenUsage;
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.ai.buffer.BufferLayerRouter;
import org.freeplane.plugin.ai.buffer.BufferRequest;
import org.freeplane.plugin.ai.buffer.BufferResponse;
import org.freeplane.plugin.ai.chat.AIChatService;
import org.freeplane.plugin.ai.chat.AIChatServiceFactory;
import org.freeplane.plugin.ai.chat.AIProviderConfiguration;
import org.freeplane.plugin.ai.chat.ChatTokenUsageTracker;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.maps.ControllerMapModelProvider;
import org.freeplane.plugin.ai.service.AIService;
import org.freeplane.plugin.ai.service.AIServiceResponse;
import org.freeplane.plugin.ai.service.AIServiceType;
import org.freeplane.plugin.ai.tools.AIToolSet;
import org.freeplane.plugin.ai.tools.AIToolSetBuilder;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummaryHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultAgentService implements AIService {
    private static final String SERVICE_NOT_INITIALIZED_MESSAGE =
        "AI service not initialized. Please configure AI provider in preferences.";

    private static volatile AIChatService agentService;
    private static volatile AIToolSet toolSet;
    private static volatile AvailableMaps availableMaps;
    private static volatile BufferLayerRouter bufferLayerRouter;
    private static final AtomicInteger inputTokens = new AtomicInteger(0);
    private static final AtomicInteger outputTokens = new AtomicInteger(0);

    @Override
    public AIServiceType getServiceType() {
        return AIServiceType.AGENT;
    }

    @Override
    public String getServiceName() {
        return "default_agent_service";
    }

    @Override
    public AIServiceResponse processRequest(Map<String, Object> request) {
        try {
            String action = (String) request.get("action");
            if (action == null) {
                return AIServiceResponse.error("Action is required");
            }

            switch (action) {
                case "generate-mindmap":
                    return handleGenerateMindMap(request);
                case "expand-node":
                    return handleExpandNode(request);
                case "summarize":
                    return handleSummarize(request);
                case "tag":
                    return handleTag(request);
                default:
                    return AIServiceResponse.error("Unknown action: " + action);
            }
        } catch (Exception e) {
            LogUtils.warn("DefaultAgentService.processRequest failed", e);
            return AIServiceResponse.error("Agent action failed: " + e.getMessage());
        }
    }

    private AIServiceResponse handleGenerateMindMap(Map<String, Object> request) {
        String topic = (String) request.get("topic");
        if (topic == null || topic.trim().isEmpty()) {
            return AIServiceResponse.error("Topic is required");
        }

        try {
            ensureAgentInitialized();
            if (agentService == null) {
                return AIServiceResponse.error(SERVICE_NOT_INITIALIZED_MESSAGE);
            }

            String prompt = buildMindMapPrompt(topic, request);
            String result = agentService.chat(prompt);

            Map<String, Object> data = Map.of(
                "success", true,
                "topic", topic,
                "result", result,
                "tokenUsage", Map.of(
                    "inputTokens", inputTokens.get(),
                    "outputTokens", outputTokens.get()
                )
            );

            return AIServiceResponse.success("Mindmap prompt generated", data);
        } catch (Exception e) {
            LogUtils.warn("DefaultAgentService.handleGenerateMindMap failed", e);
            return AIServiceResponse.error("Failed to generate mindmap: " + e.getMessage());
        }
    }

    private AIServiceResponse handleExpandNode(Map<String, Object> request) {
        String nodeId = (String) request.get("nodeId");
        if (nodeId == null) {
            return AIServiceResponse.error("NodeId is required");
        }

        try {
            ensureAgentInitialized();
            if (agentService == null) {
                return AIServiceResponse.error(SERVICE_NOT_INITIALIZED_MESSAGE);
            }

            String mapId = (String) request.get("mapId");
            Integer depth = (Integer) request.get("depth");
            Integer count = (Integer) request.get("count");
            String focus = (String) request.get("focus");

            String prompt = buildExpandNodePrompt(nodeId, mapId, depth, count, focus);
            String result = agentService.chat(prompt);

            Map<String, Object> data = Map.of(
                "nodeId", nodeId,
                "result", result,
                "tokenUsage", Map.of(
                    "inputTokens", inputTokens.get(),
                    "outputTokens", outputTokens.get()
                )
            );

            return AIServiceResponse.success("Node expansion prompt generated", data);
        } catch (Exception e) {
            LogUtils.warn("DefaultAgentService.handleExpandNode failed", e);
            return AIServiceResponse.error("Failed to expand node: " + e.getMessage());
        }
    }

    private AIServiceResponse handleSummarize(Map<String, Object> request) {
        String nodeId = (String) request.get("nodeId");
        if (nodeId == null) {
            return AIServiceResponse.error("NodeId is required");
        }

        try {
            ensureAgentInitialized();
            if (agentService == null) {
                return AIServiceResponse.error(SERVICE_NOT_INITIALIZED_MESSAGE);
            }

            String mapId = (String) request.get("mapId");
            Integer maxWords = (Integer) request.get("maxWords");
            Boolean writeToNote = (Boolean) request.get("writeToNote");

            String prompt = buildSummarizePrompt(nodeId, mapId, maxWords, writeToNote);
            String summary = agentService.chat(prompt);

            Map<String, Object> data = Map.of(
                "nodeId", nodeId,
                "summary", summary,
                "tokenUsage", Map.of(
                    "inputTokens", inputTokens.get(),
                    "outputTokens", outputTokens.get()
                )
            );

            return AIServiceResponse.success("Branch summarized", data);
        } catch (Exception e) {
            LogUtils.warn("DefaultAgentService.handleSummarize failed", e);
            return AIServiceResponse.error("Failed to summarize: " + e.getMessage());
        }
    }

    private AIServiceResponse handleTag(Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        java.util.List<String> nodeIds = (java.util.List<String>) request.get("nodeIds");
        if (nodeIds == null || nodeIds.isEmpty()) {
            return AIServiceResponse.error("NodeIds is required");
        }

        try {
            ensureAgentInitialized();
            if (agentService == null) {
                return AIServiceResponse.error(SERVICE_NOT_INITIALIZED_MESSAGE);
            }

            String mapId = (String) request.get("mapId");
            String prompt = buildTagPrompt(nodeIds, mapId);
            String result = agentService.chat(prompt);

            Map<String, Object> data = Map.of(
                "nodeIds", nodeIds,
                "result", result,
                "tokenUsage", Map.of(
                    "inputTokens", inputTokens.get(),
                    "outputTokens", outputTokens.get()
                )
            );

            return AIServiceResponse.success("Tags generated", data);
        } catch (Exception e) {
            LogUtils.warn("DefaultAgentService.handleTag failed", e);
            return AIServiceResponse.error("Failed to generate tags: " + e.getMessage());
        }
    }

    private void ensureAgentInitialized() {
        if (agentService == null) {
            synchronized (DefaultAgentService.class) {
                if (agentService == null) {
                    try {
                        AIProviderConfiguration configuration = new AIProviderConfiguration();
                        if (!isProviderConfigured(configuration)) {
                            LogUtils.warn("DefaultAgentService: No AI provider configured");
                            return;
                        }

                        availableMaps = new AvailableMaps(new ControllerMapModelProvider());
                        bufferLayerRouter = new BufferLayerRouter();

                        ToolCallSummaryHandler toolCallSummaryHandler = summary -> {
                            LogUtils.info("Agent tool call: " + summary.getSummaryText());
                        };

                        AvailableMaps.MapAccessListener mapAccessListener = (mapId, mapModel) -> {
                            LogUtils.info("Map accessed: " + mapId);
                        };

                        toolSet = new AIToolSetBuilder()
                            .toolCallSummaryHandler(toolCallSummaryHandler)
                            .availableMaps(availableMaps)
                            .mapAccessListener(mapAccessListener)
                            .toolCaller(org.freeplane.plugin.ai.tools.utilities.ToolCaller.CHAT)
                            .build();

                        ChatTokenUsageTracker tokenTracker = new ChatTokenUsageTracker(totals -> {
                            inputTokens.addAndGet((int) totals.getInputTokenCount());
                            outputTokens.addAndGet((int) totals.getOutputTokenCount());
                        });

                        agentService = AIChatServiceFactory.createService(
                            toolSet,
                            null,
                            tokenTracker,
                            toolCallSummaryHandler,
                            () -> false,
                            usage -> {}
                        );

                        LogUtils.info("DefaultAgentService: Agent AIChatService initialized successfully");
                    } catch (Exception e) {
                        LogUtils.warn("DefaultAgentService: Failed to initialize Agent AIChatService", e);
                    }
                }
            }
        }
    }

    private boolean isProviderConfigured(AIProviderConfiguration configuration) {
        return isNonEmpty(configuration.getOpenRouterKey())
            || isNonEmpty(configuration.getGeminiKey())
            || configuration.hasOllamaServiceAddress()
            || configuration.hasDashScopeKey()
            || configuration.hasErnieKey();
    }

    private boolean isNonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildMindMapPrompt(String topic, Map<String, Object> request) {
        Integer maxDepth = (Integer) request.get("maxDepth");
        if (maxDepth == null) maxDepth = 3;

        return String.format(
            "请为'%s'生成一个完整的思维导图结构。\n\n" +
            "要求：\n" +
            "1. 包含 %d 层级的节点\n" +
            "2. 每个节点内容具体、有价值\n" +
            "3. 返回严格的 JSON 格式,不要其他文字\n\n" +
            "返回格式示例：\n" +
            "{\n" +
            "  \"text\": \"%s\",\n" +
            "  \"children\": [\n" +
            "    {\"text\": \"一级分支1\", \"children\": [{\"text\": \"二级分支1.1\"}]},\n" +
            "    {\"text\": \"一级分支2\"}\n" +
            "  ]\n" +
            "}\n\n" +
            "请只返回 JSON,不要Markdown代码块标记。",
            topic, maxDepth, topic
        );
    }

    private String buildExpandNodePrompt(String nodeId, String mapId, Integer depth, Integer count, String focus) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为节点ID '").append(nodeId).append("' 生成展开内容。\n\n");

        if (depth != null) {
            prompt.append("展开层级: ").append(depth).append("\n");
        }
        if (count != null) {
            prompt.append("生成节点数: ").append(count).append("\n");
        }
        if (focus != null && !focus.trim().isEmpty()) {
            prompt.append("展开方向: ").append(focus).append("\n");
        }

        prompt.append("\n请生成合适的子节点内容，返回JSON格式：\n");
        prompt.append("{\n");
        prompt.append("  \"parentNodeId\": \"").append(nodeId).append("\",\n");
        prompt.append("  \"children\": [\n");
        prompt.append("    {\"text\": \"子节点1\", \"details\": \"可选的详细描述\"},\n");
        prompt.append("    {\"text\": \"子节点2\"}\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("请只返回JSON格式，不要其他文字。");

        return prompt.toString();
    }

    private String buildSummarizePrompt(String nodeId, String mapId, Integer maxWords, Boolean writeToNote) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为节点ID '").append(nodeId).append("' 的分支内容生成摘要。\n\n");

        if (maxWords != null) {
            prompt.append("最大字数: ").append(maxWords).append("\n");
        }
        if (Boolean.TRUE.equals(writeToNote)) {
            prompt.append("请将摘要写入节点的备注中。\n");
        }

        prompt.append("\n请生成简洁的摘要内容。");

        return prompt.toString();
    }

    private String buildTagPrompt(java.util.List<String> nodeIds, String mapId) {
        return String.format(
            "请为以下节点生成合适的标签：\n%s\n\n" +
            "要求：\n" +
            "1. 每个节点生成1-3个标签\n" +
            "2. 标签应该简洁、有意义\n" +
            "3. 返回JSON格式：\n" +
            "{\n" +
            "  \"tags\": [\n" +
            "    {\"nodeId\": \"节点ID\", \"tags\": [\"标签1\", \"标签2\"]},\n" +
            "    ...\n" +
            "  ]\n" +
            "}\n\n" +
            "请只返回JSON格式，不要其他文字。",
            nodeIds
        );
    }

    @Override
    public boolean canHandle(Map<String, Object> request) {
        String serviceType = (String) request.get("serviceType");
        return AIServiceType.AGENT.getCode().equals(serviceType);
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
