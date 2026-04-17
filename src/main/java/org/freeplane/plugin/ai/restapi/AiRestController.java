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
     * AI 展开节点：基于目标节点生成子节点内容。
     */
    public void handleExpandNode(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String nodeId = (String) body.get("nodeId");
            String mapId = (String) body.get("mapId");
            Integer depth = body.get("depth") instanceof Number ? ((Number) body.get("depth")).intValue() : null;
            Integer count = body.get("count") instanceof Number ? ((Number) body.get("count")).intValue() : 3;
            String focus = (String) body.get("focus");

            if (nodeId == null) {
                sendError(exchange, 400, "nodeId is required");
                return;
            }

            // 1. 获取目标节点
            MapModel mapModel = availableMaps.getCurrentMapModel();
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }

            NodeModel targetNode = mapModel.getNodeForID(nodeId);
            if (targetNode == null) {
                sendError(exchange, 404, "Node not found: " + nodeId);
                return;
            }

            // 2. 构建提示词
            String prompt = buildExpandNodePrompt(targetNode, depth, count, focus);

            // 3. 调用 AI 生成子节点内容
            String aiResponse = aiChatPanel.sendChatMessage(prompt, null);

            // 4. 解析 AI 响应并创建节点
            List<Map<String, Object>> createdNodes = parseAndCreateNodes(targetNode, aiResponse, mapModel);

            // 5. 返回结果
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("nodeId", nodeId);
            response.put("createdNodes", createdNodes);
            response.put("summary", "成功创建 " + createdNodes.size() + " 个子节点");
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleExpandNode error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/ai/summarize
     * 分支摘要：分析目标节点及其子节点的内容，生成简洁摘要。
     */
    public void handleSummarize(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String nodeId = (String) body.get("nodeId");
            String mapId = (String) body.get("mapId");
            Integer maxWords = body.get("maxWords") instanceof Number ? ((Number) body.get("maxWords")).intValue() : 200;
            boolean writeToNote = Boolean.TRUE.equals(body.get("writeToNote"));

            if (nodeId == null) {
                sendError(exchange, 400, "nodeId is required");
                return;
            }

            // 1. 获取目标节点
            MapModel mapModel = availableMaps.getCurrentMapModel();
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }

            NodeModel targetNode = mapModel.getNodeForID(nodeId);
            if (targetNode == null) {
                sendError(exchange, 404, "Node not found: " + nodeId);
                return;
            }

            // 2. 收集分支内容
            String branchContent = collectBranchContent(targetNode);

            // 3. 调用 AI 生成摘要
            String prompt = buildSummarizePrompt(branchContent, maxWords);
            String summary = aiChatPanel.sendChatMessage(prompt, null);

            // 4. 如果需要，写入节点备注
            if (writeToNote && summary != null) {
                org.freeplane.features.mode.Controller controller = org.freeplane.features.mode.Controller.getCurrentController();
                if (controller != null) {
                    org.freeplane.features.mode.mindmapmode.MModeController modeController = 
                        (org.freeplane.features.mode.mindmapmode.MModeController) controller.getModeController();
                    org.freeplane.features.map.mindmapmode.MMapController mapController = 
                        (org.freeplane.features.map.mindmapmode.MMapController) modeController.getMapController();
                    targetNode.setDetails(summary);
                    mapController.nodeChanged(targetNode);
                }
            }

            // 5. 返回结果
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("nodeId", nodeId);
            response.put("summary", summary);
            response.put("wordCount", summary != null ? summary.length() : 0);
            response.put("writtenToNote", writeToNote);
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("AiRestController.handleSummarize error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/ai/tag
     * 自动关键词标签：为指定节点提取关键词标签。
     */
    public void handleTag(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;
    
        try {
            Map<?, ?> body = readBody(exchange);
            String mapId = (String) body.get("mapId");
            @SuppressWarnings("unchecked")
            List<String> nodeIds = (List<String>) body.get("nodeIds");
    
            if (nodeIds == null || nodeIds.isEmpty()) {
                sendError(exchange, 400, "nodeIds is required and must not be empty");
                return;
            }
    
            // 1. 获取目标导图
            MapModel mapModel = availableMaps.getCurrentMapModel();
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }
    
            // 2. 收集节点内容
            List<Map<String, String>> nodeContents = new ArrayList<>();
            for (String nodeId : nodeIds) {
                NodeModel node = mapModel.getNodeForID(nodeId);
                if (node != null) {
                    Map<String, String> nodeInfo = new LinkedHashMap<>();
                    nodeInfo.put("nodeId", nodeId);
                    nodeInfo.put("text", node.getText());
                    nodeContents.add(nodeInfo);
                }
            }
    
            if (nodeContents.isEmpty()) {
                sendError(exchange, 404, "No valid nodes found");
                return;
            }
    
            // 3. 调用 AI 提取标签
            String prompt = buildTagPrompt(nodeContents);
            String aiResponse = aiChatPanel.sendChatMessage(prompt, null);
    
            // 4. 解析 AI 响应
            List<Map<String, Object>> results = parseTagResults(aiResponse, nodeContents);
    
            // 5. 返回结果
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("results", results);
            response.put("message", "成功为 " + results.size() + " 个节点提取标签");
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
     * 执行 AI 对话（调用 AIChatPanel 的公开方法）
     */
    private String executeChat(String message, String modelSelection) {
        try {
            // 直接调用 AIChatPanel 的公开方法，复用插件的完整能力
            return aiChatPanel.sendChatMessage(message, modelSelection);
        } catch (Exception e) {
            LogUtils.warn("Failed to execute chat", e);
            return "[错误] 对话失败: " + e.getMessage();
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

    /**
     * 构建节点扩展的提示词
     */
    private String buildExpandNodePrompt(NodeModel node, Integer depth, Integer count, String focus) {
        String nodeText = node.getText();
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下思维导图节点生成子节点内容：\n\n");
        prompt.append("父节点：").append(nodeText).append("\n\n");
        
        if (focus != null && !focus.isEmpty()) {
            prompt.append("扩展方向：").append(focus).append("\n");
        }
        
        prompt.append("要求：\n");
        prompt.append("1. 生成 ").append(count != null ? count : 3).append(" 个子节点\n");
        if (depth != null && depth > 1) {
            prompt.append("2. 每个子节点包含 ").append(depth).append(" 层级的内容\n");
        }
        prompt.append("3. 返回严格的 JSON 格式\n\n");
        prompt.append("返回格式：\n");
        prompt.append("[\n");
        prompt.append("  {\"text\": \"子节点1\", \"children\": [{\"text\": \"子子节点1.1\"}]},\n");
        prompt.append("  {\"text\": \"子节点2\"}\n");
        prompt.append("]\n\n");
        prompt.append("请只返回 JSON，不要其他文字。");
        
        return prompt.toString();
    }

    /**
     * 解析 AI 响应并创建节点
     */
    private List<Map<String, Object>> parseAndCreateNodes(NodeModel parentNode, String aiResponse, MapModel mapModel) {
        List<Map<String, Object>> createdNodes = new ArrayList<>();
        
        try {
            // 清理 AI 响应
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
            List<Map<String, Object>> nodeDataList = objectMapper.readValue(cleanedJson, List.class);
            
            // 获取 MMapController
            org.freeplane.features.mode.Controller controller = org.freeplane.features.mode.Controller.getCurrentController();
            if (controller == null) {
                LogUtils.warn("Controller not available");
                return createdNodes;
            }
            
            org.freeplane.features.mode.mindmapmode.MModeController modeController = 
                (org.freeplane.features.mode.mindmapmode.MModeController) controller.getModeController();
            org.freeplane.features.map.mindmapmode.MMapController mapController = 
                (org.freeplane.features.map.mindmapmode.MMapController) modeController.getMapController();
            
            // 递归创建节点
            for (Map<String, Object> nodeData : nodeDataList) {
                String text = (String) nodeData.get("text");
                if (text == null || text.trim().isEmpty()) continue;
                
                NodeModel newNode = mapController.addNewNode(
                    parentNode,
                    parentNode.getChildCount(),
                    node -> node.setText(text.trim())
                );
                
                Map<String, Object> nodeInfo = new LinkedHashMap<>();
                nodeInfo.put("nodeId", newNode.getID());
                nodeInfo.put("text", text);
                createdNodes.add(nodeInfo);
                
                // 递归创建子节点
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeData.get("children");
                if (children != null && !children.isEmpty()) {
                    createChildNodesRecursive(newNode, children, mapController, createdNodes);
                }
            }
        } catch (Exception e) {
            LogUtils.warn("Failed to parse and create nodes", e);
        }
        
        return createdNodes;
    }

    /**
     * 递归创建子节点
     */
    @SuppressWarnings("unchecked")
    private void createChildNodesRecursive(NodeModel parentNode, List<Map<String, Object>> children, 
                                          org.freeplane.features.map.mindmapmode.MMapController mapController,
                                          List<Map<String, Object>> createdNodes) {
        for (Map<String, Object> childData : children) {
            String text = (String) childData.get("text");
            if (text == null || text.trim().isEmpty()) continue;
            
            NodeModel childNode = mapController.addNewNode(
                parentNode,
                parentNode.getChildCount(),
                node -> node.setText(text.trim())
            );
            
            Map<String, Object> nodeInfo = new LinkedHashMap<>();
            nodeInfo.put("nodeId", childNode.getID());
            nodeInfo.put("text", text);
            nodeInfo.put("parentId", parentNode.getID());
            createdNodes.add(nodeInfo);
            
            List<Map<String, Object>> subChildren = (List<Map<String, Object>>) childData.get("children");
            if (subChildren != null && !subChildren.isEmpty()) {
                createChildNodesRecursive(childNode, subChildren, mapController, createdNodes);
            }
        }
    }

    /**
     * 收集分支内容（递归遍历所有子节点）
     */
    private String collectBranchContent(NodeModel node) {
        StringBuilder content = new StringBuilder();
        collectNodeContentRecursive(node, content, 0);
        return content.toString();
    }

    /**
     * 递归收集节点内容
     */
    private void collectNodeContentRecursive(NodeModel node, StringBuilder content, int depth) {
        if (node == null) return;
        
        String indent = "  ".repeat(depth);
        content.append(indent).append("- ").append(node.getText()).append("\n");
        
        for (NodeModel child : node.getChildren()) {
            collectNodeContentRecursive(child, content, depth + 1);
        }
    }

    /**
     * 构建摘要提示词
     */
    private String buildSummarizePrompt(String branchContent, int maxWords) {
        return String.format(
            "请为以下思维导图分支生成简洁的摘要：\n\n%s\n\n" +
            "要求：\n" +
            "1. 摘要长度不超过 %d 字\n" +
            "2. 突出核心主题和关键信息\n" +
            "3. 语言简洁明了\n" +
            "4. 只返回摘要文本，不要其他文字",
            branchContent,
            maxWords
        );
    }

    /**
     * 构建标签提取提示词
     */
    private String buildTagPrompt(List<Map<String, String>> nodeContents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下思维导图节点提取关键词标签：\n\n");
        
        for (Map<String, String> node : nodeContents) {
            prompt.append("节点 ID: ").append(node.get("nodeId")).append("\n");
            prompt.append("内容: ").append(node.get("text")).append("\n\n");
        }
        
        prompt.append("要求：\n");
        prompt.append("1. 为每个节点提取 2-5 个关键词标签\n");
        prompt.append("2. 标签应简洁、准确、具有代表性\n");
        prompt.append("3. 返回严格的 JSON 格式\n\n");
        prompt.append("返回格式：\n");
        prompt.append("[\n");
        prompt.append("  {\"nodeId\": \"节点ID\", \"tags\": [\"标签1\", \"标签2\", \"标签3\"]},\n");
        prompt.append("  ...\n");
        prompt.append("]\n\n");
        prompt.append("请只返回 JSON，不要其他文字。");
        
        return prompt.toString();
    }

    /**
     * 解析标签结果
     */
    private List<Map<String, Object>> parseTagResults(String aiResponse, List<Map<String, String>> nodeContents) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // 清理 AI 响应
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
            List<Map<String, Object>> tagDataList = objectMapper.readValue(cleanedJson, List.class);
            results.addAll(tagDataList);
        } catch (Exception e) {
            LogUtils.warn("Failed to parse tag results", e);
            // 如果解析失败，返回空结果
            for (Map<String, String> node : nodeContents) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("nodeId", node.get("nodeId"));
                result.put("tags", new ArrayList<>());
                results.add(result);
            }
        }
        
        return results;
    }

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
