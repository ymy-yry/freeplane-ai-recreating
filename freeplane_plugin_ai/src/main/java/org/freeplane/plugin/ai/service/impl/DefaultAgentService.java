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
import org.freeplane.plugin.ai.service.ToolExecutionService;
import org.freeplane.plugin.ai.service.impl.DefaultToolExecutionService;
import org.freeplane.plugin.ai.tools.AIToolSet;
import org.freeplane.plugin.ai.tools.AIToolSetBuilder;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummaryHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultAgentService implements AIService {

    private static volatile AIChatService agentService;
    private static volatile AIToolSet toolSet;
    private static volatile AvailableMaps availableMaps;
    private static volatile BufferLayerRouter bufferLayerRouter;
    private static final AtomicInteger inputTokens = new AtomicInteger(0);
    private static final AtomicInteger outputTokens = new AtomicInteger(0);
    private static Properties promptTemplates;
    private static volatile ToolExecutionService toolExecutionService;

    static {
        loadPromptTemplates();
    }

    private static void loadPromptTemplates() {
        promptTemplates = new Properties();
        try {
            String resourcePath = "org/freeplane/plugin/ai/buffer/prompts.properties";
            InputStream inputStream = DefaultAgentService.class.getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream != null) {
                promptTemplates.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                LogUtils.info("DefaultAgentService: Loaded prompt templates from " + resourcePath);
            } else {
                LogUtils.warn("DefaultAgentService: Could not find prompts.properties resource");
            }
        } catch (IOException e) {
            LogUtils.warn("DefaultAgentService: Failed to load prompt templates", e);
        }
    }

    private static String getPromptTemplate(String key, String defaultValue) {
        if (promptTemplates != null) {
            String template = promptTemplates.getProperty(key);
            if (template != null && !template.trim().isEmpty()) {
                return template;
            }
        }
        return defaultValue;
    }

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
                case "execute-tool":
                    return handleExecuteTool(request);
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

                        // 初始化工具执行服务
                        toolExecutionService = new DefaultToolExecutionService();
                        toolExecutionService.setToolSet(toolSet);

                        LogUtils.info("DefaultAgentService: Agent AIChatService initialized successfully");
                        LogUtils.info("DefaultAgentService: ToolExecutionService initialized successfully");
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
            || configuration.hasErnieKey()
            || configuration.hasDashScopeKey();
    }

    private boolean isNonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildMindMapPrompt(String topic, Map<String, Object> request) {
        Integer maxDepth = (Integer) request.get("maxDepth");
        if (maxDepth == null) maxDepth = 3;

        String template = getPromptTemplate("mindmap.generation.zh",
            "请为'{topic}'生成一个完整的思维导图结构。\n\n" +
            "要求：\n" +
            "1. 包含 {maxDepth} 层级的节点\n" +
            "2. 每个节点内容具体、有价值\n" +
            "3. 返回严格的 JSON 格式，不要其他文字\n\n" +
            "返回格式示例：\n" +
            "{\n" +
            "  \"text\": \"{topic}\",\n" +
            "  \"children\": [\n" +
            "    {\"text\": \"一级分支1\", \"children\": [{\"text\": \"二级分支1.1\"}]},\n" +
            "    {\"text\": \"一级分支2\"}\n" +
            "  ]\n" +
            "}\n\n" +
            "请只返回 JSON，不要Markdown代码块标记。");

        return template
            .replace("{topic}", topic)
            .replace("{maxDepth}", String.valueOf(maxDepth));
    }

    private String buildExpandNodePrompt(String nodeId, String mapId, Integer depth, Integer count, String focus) {
        if (depth == null) depth = 1;
        if (count == null) count = 3;
        if (focus == null) focus = "相关内容";

        String template = getPromptTemplate("mindmap.expansion.zh",
            "你是一位专业的内容扩展专家，擅长为思维导图节点提供详细、有价值的扩展内容。\n\n" +
            "任务：请为节点 '{nodeText}' 展开更详细的内容。\n\n" +
            "详细指令：\n" +
            "1. 基于节点内容，生成 {count} 个子节点\n" +
            "2. 展开深度为 {depth} 层\n" +
            "3. 关注方向：{focus}\n" +
            "4. 确保扩展内容与原节点主题相关\n" +
            "5. 内容要具体、有价值，避免空洞的表述\n\n" +
            "质量标准：\n" +
            "- 扩展内容与原节点主题高度相关\n" +
            "- 子节点内容具体、有深度\n" +
            "- 层次结构清晰，逻辑连贯\n" +
            "- 符合用户指定的关注方向\n\n" +
            "重要提示：由于您无法直接访问思维导图节点，请按以下步骤操作：\n" +
            "1. 首先使用 getSelectedMapAndNodeIdentifiers 或 readNodesWithDescendants 工具获取目标节点的信息\n" +
            "2. 然后使用 readNodesWithDescendants 工具读取目标节点的当前内容\n" +
            "3. 接着使用 createNodes 工具创建新的子节点\n" +
            "4. 创建完成后，返回新创建的节点信息摘要");

        return template
            .replace("{nodeText}", nodeId)
            .replace("{count}", String.valueOf(count))
            .replace("{depth}", String.valueOf(depth))
            .replace("{focus}", focus);
    }

    private String buildSummarizePrompt(String nodeId, String mapId, Integer maxWords, Boolean writeToNote) {
        if (maxWords == null) maxWords = 100;

        String template = getPromptTemplate("mindmap.summary.zh",
            "你是一位专业的内容摘要专家，擅长从复杂信息中提取核心要点，生成简洁明了的摘要。\n\n" +
            "任务：请为以下内容生成简洁的摘要。\n\n" +
            "内容：\n" +
            "{content}\n\n" +
            "详细指令：\n" +
            "1. 分析内容，识别核心要点和关键信息\n" +
            "2. 生成长度不超过 {maxWords} 字的摘要\n" +
            "3. 保留所有核心要点，确保信息完整性\n" +
            "4. 使用简洁清晰的语言，避免冗余\n" +
            "5. 直接返回摘要文本，不要包含任何其他内容\n" +
            "6. 使用中文进行摘要创作\n\n" +
            "质量标准：\n" +
            "- 摘要准确反映原内容的核心要点\n" +
            "- 语言简洁明了，没有冗余信息\n" +
            "- 逻辑连贯，易于理解\n" +
            "- 长度控制在指定范围内\n\n" +
            "返回格式：\n" +
            "请直接返回摘要文本，不要包含任何其他内容或格式标记。");

        String writeToNoteInstruction = Boolean.TRUE.equals(writeToNote)
            ? "\n\n【重要】写入备注流程：\n" +
              "由于您无法直接访问思维导图节点，请按以下步骤操作：\n" +
              "1. 首先使用 getSelectedMapAndNodeIdentifiers 或 readNodesWithDescendants 工具获取目标节点的信息\n" +
              "2. 使用 readNodesWithDescendants 工具读取目标节点的当前内容\n" +
              "3. 生成摘要内容\n" +
              "4. 使用 fetchNodesForEditing 工具获取节点的 originalContentType（必需字段：mapIdentifier、nodeIdentifiers=[节点ID]、editableContentFields=[NOTE]）\n" +
              "5. 使用 edit 工具写入摘要，必须传递从 fetchNodesForEditing 获取的 originalContentType\n" +
              "edit工具调用示例：\n" +
              "{ \"request\": { \"mapIdentifier\": \"地图UUID\", \"items\": [ { \"nodeIdentifier\": \"节点UUID\", \"editedElement\": \"NOTE\", \"originalContentType\": \"PLAIN_TEXT\", \"value\": \"摘要内容\" } ] } }"
            : "\n\n重要提示：由于您无法直接访问思维导图节点，请先使用 readNodesWithDescendants 工具读取节点内容，然后基于读取到的内容生成摘要。";

        return template
            .replace("{content}", "节点ID: " + nodeId + "\n\n请先使用readNodesWithDescendants工具读取节点内容，然后基于内容生成摘要。")
            .replace("{maxWords}", String.valueOf(maxWords))
            + writeToNoteInstruction;
    }

    private String buildTagPrompt(java.util.List<String> nodeIds, String mapId) {
        return String.format(
            "你是一位专业的标签生成专家，擅长为思维导图节点生成简洁、有意义的标签。\n\n" +
            "任务：请为以下节点生成合适的标签。\n\n" +
            "节点列表：\n%s\n\n" +
            "详细指令：\n" +
            "1. 每个节点生成1-3个标签\n" +
            "2. 标签应该简洁、有意义\n" +
            "3. 标签应与节点内容相关\n\n" +
            "重要提示：由于您无法直接访问思维导图节点，请按以下步骤操作：\n" +
            "1. 首先使用 getSelectedMapAndNodeIdentifiers 或 readNodesWithDescendants 工具获取节点信息\n" +
            "2. 然后使用 readNodesWithDescendants 工具读取每个节点的内容\n" +
            "3. 基于内容生成标签\n" +
            "4. 如需写入标签，请使用以下流程：\n" +
            "   a) 对于TAGS/ICONS：可直接使用 edit 工具（无需 fetchNodesForEditing）\n" +
            "   b) 对于TEXT/DETAILS/NOTE：必须先调用 fetchNodesForEditing 获取 originalContentType\n\n" +
            "返回格式：\n" +
            "请返回JSON格式，包含每个节点的ID和对应的标签列表：\n" +
            "{\n" +
            "  \"tags\": [\n" +
            "    {\"nodeId\": \"节点ID\", \"tags\": [\"标签1\", \"标签2\"]},\n" +
            "    ...\n" +
            "  ]\n" +
            "}",
            nodeIds
        );
    }

    private AIServiceResponse handleExecuteTool(Map<String, Object> request) {
        try {
            ensureAgentInitialized();

            String toolName = (String) request.get("toolName");
            if (toolName == null || toolName.trim().isEmpty()) {
                return AIServiceResponse.error("Tool name is required");
            }

            Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
            if (parameters == null) {
                parameters = new HashMap<>();
            }

            if (toolExecutionService == null) {
                return AIServiceResponse.error("Tool execution service not initialized");
            }

            if (!toolExecutionService.isToolSupported(toolName)) {
                return AIServiceResponse.error("Tool not supported: " + toolName);
            }

            Object result = toolExecutionService.executeTool(toolName, parameters);

            Map<String, Object> data = Map.of(
                "toolName", toolName,
                "result", result
            );

            return AIServiceResponse.success("Tool executed successfully", data);
        } catch (Exception e) {
            LogUtils.warn("DefaultAgentService.handleExecuteTool failed", e);
            return AIServiceResponse.error("Failed to execute tool: " + e.getMessage());
        }
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
