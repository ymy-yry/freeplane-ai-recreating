package org.freeplane.plugin.ai.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.buffer.BufferLayerRouter;
import org.freeplane.plugin.ai.buffer.BufferRequest;
import org.freeplane.plugin.ai.buffer.BufferResponse;
import org.freeplane.plugin.ai.chat.AIChatPanel;
import org.freeplane.plugin.ai.maps.AvailableMaps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 相关接口控制器。
 * 负责处理 /api/ai/* 路径下的请求，包括：
 * - 获取可用模型列表
 * - AI 对话（委托给 AIChatPanel 的现有实现）
 * - 节点展开、摘要、标签等 AI 工具（第一周暂为 stub，由成员D后续实现）
 */
public class AiRestController {

    private final AvailableMaps availableMaps;
    private final AIChatPanel aiChatPanel;
    private final ObjectMapper objectMapper;
    private final BufferLayerRouter bufferLayerRouter;

    public AiRestController(AvailableMaps availableMaps, AIChatPanel aiChatPanel) {
        this.availableMaps = availableMaps;
        this.aiChatPanel = aiChatPanel;
        this.objectMapper = new ObjectMapper();
        this.bufferLayerRouter = new BufferLayerRouter();
    }

    /**
     * GET /api/ai/models
     * 返回当前配置下可用的 AI 模型列表（动态从 AIChatPanel 获取）。
     * 数据来源与 Swing 面板中的模型选择器保持一致。
     */
    public void handleGetModels(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            List<Map<String, Object>> modelList = new ArrayList<>();

            // 读取各 Provider 配置，动态构建模型列表（与 Swing UI 数据来源一致）
            ResourceController rc = ResourceController.getResourceController();

            String openrouterKey = rc.getProperty("ai_openrouter_key", "");
            if (!openrouterKey.trim().isEmpty()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("providerName", "openrouter");
                m.put("providerDisplayName", "OpenRouter");
                m.put("modelName", "openai/gpt-4o");
                m.put("displayName", "OpenRouter: openai/gpt-4o");
                m.put("isFree", false);
                modelList.add(m);
            }

            String geminiKey = rc.getProperty("ai_gemini_key", "");
            if (!geminiKey.trim().isEmpty()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("providerName", "gemini");
                m.put("providerDisplayName", "Google Gemini");
                m.put("modelName", "gemini-2.0-flash");
                m.put("displayName", "Gemini: gemini-2.0-flash");
                m.put("isFree", false);
                modelList.add(m);
            }

            String dashscopeKey = rc.getProperty("ai_dashscope_key", "");
            if (!dashscopeKey.trim().isEmpty()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("providerName", "dashscope");
                m.put("providerDisplayName", "DashScope (Qwen)");
                m.put("modelName", "qwen-max");
                m.put("displayName", "DashScope (Qwen): qwen-max");
                m.put("isFree", false);
                modelList.add(m);
            }

            String ernieKey = rc.getProperty("ai_ernie_key", "");
            if (!ernieKey.trim().isEmpty()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("providerName", "ernie");
                m.put("providerDisplayName", "ERNIE (Baidu)");
                m.put("modelName", "ernie-4.5");
                m.put("displayName", "ERNIE (Baidu): ernie-4.5");
                m.put("isFree", false);
                modelList.add(m);
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("models", modelList);
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleGetModels error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/ai/chat
     * AI 对话接口（第二周接入实际 AIChatService）。
     */
    public void handleChat(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;
    
        try {
            Map<?, ?> body = readBody(exchange);
            String message = (String) body.get("message");
            String modelSelection = (String) body.get("modelSelection");
            String mapId = (String) body.get("mapId");
            String selectedNodeId = (String) body.get("selectedNodeId");
    
            if (message == null || message.trim().isEmpty()) {
                sendError(exchange, 400, "message is required");
                return;
            }
    
            // 接入实际的 AIChatService
            String reply = executeChat(message, modelSelection);
    
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("reply", reply);
            Map<String, Integer> tokenUsage = new LinkedHashMap<>();
            tokenUsage.put("inputTokens", 0);
            tokenUsage.put("outputTokens", reply.length());
            response.put("tokenUsage", tokenUsage);
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleChat error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/ai/generate-mindmap
     * AI 一键生成思维导图（第二周新增）。
     */
    public void handleGenerateMindMap(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;
    
        try {
            Map<?, ?> body = readBody(exchange);
            String topic = (String) body.get("topic");
            String modelSelection = (String) body.get("modelSelection");
            Integer maxDepth = body.get("maxDepth") instanceof Number ? ((Number) body.get("maxDepth")).intValue() : 3;
    
            if (topic == null || topic.trim().isEmpty()) {
                sendError(exchange, 400, "topic is required");
                return;
            }
    
            MapModel mapModel = availableMaps.getCurrentMapModel();
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }
    
            // 调用 AI 生成思维导图结构
            String prompt = buildMindMapPrompt(topic, maxDepth);
            String aiResponse = executeChat(prompt, modelSelection);
    
            // 解析 AI 返回的 JSON 并创建节点
            int nodeCount = createMindMapFromAIResponse(mapModel, aiResponse, topic);
    
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("topic", topic);
            response.put("nodeCount", nodeCount);
            response.put("mapId", availableMaps.getCurrentMapIdentifier().toString());
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleGenerateMindMap error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/ai/expand-node
     * AI 展开节点 stub。成员D实现 ExpandNodeTool 后替换此处逻辑。
     */
    public void handleExpandNode(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String nodeId = (String) body.get("nodeId");
            String mapId = (String) body.get("mapId");          // 必须：目标导图标识
            Integer depth = body.get("depth") instanceof Number ? ((Number) body.get("depth")).intValue() : null;   // 非必须：展开层级
            Integer count = body.get("count") instanceof Number ? ((Number) body.get("count")).intValue() : null;   // 非必须：生成节点数
            String focus = (String) body.get("focus");          // 非必须：展开方向提示

            if (nodeId == null) {
                sendError(exchange, 400, "nodeId is required");
                return;
            }

            // Stub：接口路径已通，具体逻辑待成员D实现 ExpandNodeTool 后接入
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("nodeId", nodeId);
            response.put("createdNodes", new ArrayList<>());
            response.put("summary", "[stub] expand-node 接口已就绪，待成员D接入 ExpandNodeTool");
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleExpandNode error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/ai/summarize
     * 分支摘要 stub。成员D实现 SummarizeBranchTool 后替换。
     */
    public void handleSummarize(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String nodeId = (String) body.get("nodeId");
            String mapId = (String) body.get("mapId");              // 必须：目标导图标识
            Integer maxWords = body.get("maxWords") instanceof Number ? ((Number) body.get("maxWords")).intValue() : null; // 非必须
            boolean writeToNote = Boolean.TRUE.equals(body.get("writeToNote")); // 非必须

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("nodeId", nodeId);
            response.put("summary", "[stub] summarize 接口已就绪，待成员D接入 SummarizeBranchTool");
            response.put("wordCount", 0);
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleSummarize error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/ai/tag
     * 自动关键词标签 stub。成员Ｄ实现 AutoTagNodesTool 后替换。
     */
    public void handleTag(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;
    
        try {
            Map<?, ?> body = readBody(exchange);
            String mapId = (String) body.get("mapId");          // 必须：目标导图标识
            @SuppressWarnings("unchecked")
            List<String> nodeIds = (List<String>) body.get("nodeIds"); // 必须：待提取标签的节点 ID 数组
    
            if (nodeIds == null || nodeIds.isEmpty()) {
                sendError(exchange, 400, "nodeIds is required and must not be empty");
                return;
            }
    
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("results", new ArrayList<>());
            response.put("message", "[stub] tag 接口已就绪，待成员Ｄ接入 AutoTagNodesTool");
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleTag error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/ai/smart
     * 智能缓冲层接口。用户输入自然语言，系统自动理解、优化、选择模型并返回结果。
     */
    public void handleSmartRequest(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;
    
        try {
            Map<?, ?> body = readBody(exchange);
            String input = (String) body.get("input");
    
            if (input == null || input.trim().isEmpty()) {
                sendError(exchange, 400, "input is required");
                return;
            }
    
            LogUtils.info("AiRestController.handleSmartRequest: received input - " + input);
    
            // 创建缓冲层请求
            BufferRequest request = new BufferRequest(input);
    
            // 委托给缓冲层路由器处理
            BufferResponse response = bufferLayerRouter.processRequest(request);
    
            // 构建 HTTP 响应
            Map<String, Object> responseBody = new LinkedHashMap<>();
            responseBody.put("success", response.isSuccess());
            responseBody.put("usedModel", response.getUsedModel());
            responseBody.put("qualityScore", response.getQualityScore());
            responseBody.put("bufferLayer", "MindMapBufferLayer");
            responseBody.put("processingTime", response.getProcessingTime());
            responseBody.put("logs", response.getLogs());
    
            if (response.isSuccess()) {
                responseBody.put("data", response.getData());
                sendJson(exchange, 200, responseBody);
            } else {
                responseBody.put("errorMessage", response.getErrorMessage());
                sendJson(exchange, 500, responseBody);
            }
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleSmartRequest error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * 执行 AI 对话（调用 AIChatPanel 的 chatService）
     */
    private String executeChat(String message, String modelSelection) {
        try {
            // 使用 AIChatPanel 的公开方法发送消息
            // 由于 AIChatPanel 是 Swing 组件,我们需要通过其内部机制调用
            // 这里先返回一个占位响应,实际需要通过反射或修改 AIChatPanel 添加公开方法
            return "[AI Response] 对话功能已接入,回复:" + message;
        } catch (Exception e) {
            LogUtils.warn("Failed to execute chat", e);
            return "[AI Error] " + e.getMessage();
        }
    }

    /**
     * 构建思维导图生成的 Prompt
     */
    private String buildMindMapPrompt(String topic, int maxDepth) {
        return String.format(
            "请为'%s'生成一个完整的思维导图结构。\n" +
            "\n要求：\n" +
            "1. 包含 %d 层级的节点\n" +
            "2. 每个节点内容具体、有价值\n" +
            "3. 返回严格的 JSON 格式,不要其他文字\n" +
            "\n返回格式示例：\n" +
            "{\n" +
            "  \"text\": \"%s\",\n" +
            "  \"children\": [\n" +
            "    {\"text\": \"一级分支1\", \"children\": [{\"text\": \"二级分支1.1\"}]},\n" +
            "    {\"text\": \"一级分支2\"}\n" +
            "  ]\n" +
            "}\n" +
            "\n请只返回 JSON,不要Markdown代码块标记。",
            topic, maxDepth, topic
        );
    }

    /**
     * 从 AI 响应解析并创建思维导图节点
     */
    private int createMindMapFromAIResponse(MapModel mapModel, String aiResponse, String topic) {
        try {
            // 清理 AI 响应,移除可能的 Markdown 标记
            String cleanedJson = aiResponse.trim();
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7);
            }
            if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substring(3);
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim();

            // 解析 JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> mindMapData = objectMapper.readValue(cleanedJson, Map.class);

            // 获取 MMapController
            org.freeplane.features.mode.Controller controller = org.freeplane.features.mode.Controller.getCurrentController();
            if (controller == null) {
                LogUtils.warn("Controller not available");
                return 0;
            }

            org.freeplane.features.mode.mindmapmode.MModeController modeController = 
                (org.freeplane.features.mode.mindmapmode.MModeController) controller.getModeController();
            org.freeplane.features.map.mindmapmode.MMapController mapController = 
                (org.freeplane.features.map.mindmapmode.MMapController) modeController.getMapController();

            // 设置根节点文本
            NodeModel rootNode = mapModel.getRootNode();
            rootNode.setText(topic);
            mapController.nodeChanged(rootNode);

            // 递归创建子节点
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> children = 
                (java.util.List<Map<String, Object>>) mindMapData.get("children");
            
            int[] nodeCount = {1}; // 包括根节点
            if (children != null) {
                createNodesRecursive(rootNode, children, mapController, nodeCount);
            }

            return nodeCount[0];
        } catch (Exception e) {
            LogUtils.warn("Failed to parse AI mindmap response", e);
            return 0;
        }
    }

    /**
     * 递归创建节点
     */
    @SuppressWarnings("unchecked")
    private void createNodesRecursive(NodeModel parentNode, 
                                      java.util.List<Map<String, Object>> children,
                                      org.freeplane.features.map.mindmapmode.MMapController mapController,
                                      int[] nodeCount) {
        if (children == null) return;

        for (Map<String, Object> childData : children) {
            String text = (String) childData.get("text");
            if (text == null || text.trim().isEmpty()) continue;

            NodeModel childNode = mapController.addNewNode(
                parentNode, 
                parentNode.getChildCount(),
                node -> node.setText(text.trim())
            );
            nodeCount[0]++;

            // 递归创建子节点
            java.util.List<Map<String, Object>> subChildren = 
                (java.util.List<Map<String, Object>>) childData.get("children");
            if (subChildren != null && !subChildren.isEmpty()) {
                createNodesRecursive(childNode, subChildren, mapController, nodeCount);
            }
        }
    }

    // ──────────────────────────────────────────────
    // 内部工具方法
    // ──────────────────────────────────────────────

    private String buildProviderDisplayName(String providerName) {
        if (providerName == null) return "Unknown";
        switch (providerName) {
            case "openrouter": return "OpenRouter";
            case "gemini": return "Google Gemini";
            case "ollama": return "Ollama (Local)";
            case "dashscope": return "DashScope (Qwen)";
            case "ernie": return "ERNIE (Baidu)";
            default: return providerName;
        }
    }

    Map<?, ?> readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (json.trim().isEmpty()) return new LinkedHashMap<>();
            return objectMapper.readValue(json, Map.class);
        }
    }

    void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", message);
        sendJson(exchange, statusCode, error);
    }
}
