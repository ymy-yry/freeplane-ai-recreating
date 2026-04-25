package org.freeplane.plugin.ai.buffer.mindmap;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.plugin.ai.buffer.BufferRequest;
import org.freeplane.plugin.ai.buffer.BufferResponse;
import org.freeplane.plugin.ai.buffer.IBufferLayer;
import org.freeplane.plugin.ai.chat.AIChatModelFactory;
import org.freeplane.plugin.ai.chat.AIProviderConfiguration;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 思维导图完整缓冲层。
 * 整合需求分析、提示词优化、模型选择、结果优化等组件。
 */
public class MindMapBufferLayer implements IBufferLayer {

    private final MindMapRequirementAnalyzer requirementAnalyzer;
    private final MindMapPromptOptimizer promptOptimizer;
    private final MindMapModelRouter modelRouter;
    private final MindMapResultOptimizer resultOptimizer;

    public MindMapBufferLayer() {
        this.requirementAnalyzer = new MindMapRequirementAnalyzer();
        this.promptOptimizer = new MindMapPromptOptimizer();
        this.modelRouter = new MindMapModelRouter();
        this.resultOptimizer = new MindMapResultOptimizer();
        LogUtils.info("MindMapBufferLayer: initialized");
    }

    @Override
    public String getName() {
        return "MindMapBufferLayer";
    }

    @Override
    public boolean canHandle(BufferRequest request) {
        if (request == null || request.getUserInput() == null) {
            return false;
        }

        String input = request.getUserInput().toLowerCase();

        // 检查是否包含思维导图相关关键词
        boolean hasMindMapKeyword = input.contains("思维导图") ||
                                   input.contains("mindmap") ||
                                   input.contains("mind map") ||
                                   input.contains("脑图") ||
                                   input.contains("生成") ||
                                   input.contains("创建") ||
                                   input.contains("generate") ||
                                   input.contains("create");

        return hasMindMapKeyword;
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级
    }

    @Override
    public BufferResponse process(BufferRequest request) {
        long startTime = System.currentTimeMillis();
        BufferResponse response = new BufferResponse();

        try {
            // 步骤 1：需求分析
            LogUtils.info("MindMapBufferLayer: step 1 - requirement analysis");
            requirementAnalyzer.analyze(request);
            response.addLog("需求识别: " + request.getRequestType());

            // 步骤 2：提示词优化
            LogUtils.info("MindMapBufferLayer: step 2 - prompt optimization");
            String optimizedPrompt = promptOptimizer.optimizePrompt(request);
            response.addLog("提示词优化: " + optimizedPrompt.length() + " 字符");

            // 步骤 3：模型选择
            LogUtils.info("MindMapBufferLayer: step 3 - model selection");
            String selectedModel = modelRouter.selectBestModel(request);
            if (selectedModel == null) {
                response.setSuccess(false);
                response.setErrorMessage("没有可用的 AI 模型，请检查 API Key 配置");
                response.setProcessingTime(System.currentTimeMillis() - startTime);
                return response;
            }
            response.setUsedModel(selectedModel);
            response.addLog("模型选择: " + selectedModel);

            // 步骤 4：调用 AI（这里需要集成现有的 AI 调用逻辑）
            LogUtils.info("MindMapBufferLayer: step 4 - calling AI");
            String aiResponse = callAI(optimizedPrompt, selectedModel, request);

            // 步骤 5：结果优化
            LogUtils.info("MindMapBufferLayer: step 5 - result optimization");
            Map<String, Object> optimizedData = resultOptimizer.optimizeResult(aiResponse, response);

            if (optimizedData == null) {
                response.setSuccess(false);
                response.setErrorMessage("AI 返回结果格式无效");
                response.setProcessingTime(System.currentTimeMillis() - startTime);
                return response;
            }

            // 步骤 6：创建思维导图节点
            LogUtils.info("MindMapBufferLayer: step 6 - creating mindmap nodes");
            int nodeCount = createMindMapNodes(optimizedData);
            response.putData("nodeCount", nodeCount);

            // 设置成功响应
            response.setSuccess(true);
            response.setData(optimizedData);
            response.addLog("节点创建: " + nodeCount + " 个");
            response.setProcessingTime(System.currentTimeMillis() - startTime);

            LogUtils.info("MindMapBufferLayer: processing completed successfully in " +
                         (System.currentTimeMillis() - startTime) + "ms");

        } catch (Exception e) {
            LogUtils.warn("MindMapBufferLayer: processing failed", e);
            response.setSuccess(false);
            response.setErrorMessage("处理失败: " + e.getMessage());
            response.setProcessingTime(System.currentTimeMillis() - startTime);
        }

        return response;
    }

    // 懒初始化的底层 ChatModel（双重检查锁），绕开 AIChatService 的复杂 system message
    private volatile ChatModel chatModel;

    /**
     * 懒初始化 ChatModel
     */
    private void ensureChatModelInitialized() {
        if (chatModel == null) {
            synchronized (this) {
                if (chatModel == null) {
                    try {
                        AIProviderConfiguration configuration = new AIProviderConfiguration();
                        chatModel = AIChatModelFactory.createChatLanguageModel(configuration);
                        LogUtils.info("MindMapBufferLayer: ChatModel initialized");
                    } catch (Exception e) {
                        LogUtils.warn("MindMapBufferLayer: failed to initialize ChatModel", e);
                    }
                }
            }
        }
    }

    /**
     * 调用真实 AI 模型（使用底层 ChatModel，避免 AIChatService 的 system message 干扰）
     */
    private String callAI(String prompt, String selectedModel, BufferRequest request) {
        LogUtils.info("MindMapBufferLayer: calling AI model " + selectedModel);
        ensureChatModelInitialized();
        if (chatModel == null) {
            LogUtils.warn("MindMapBufferLayer: ChatModel unavailable, using sample JSON");
            return createSampleMindMapJSON(request.getParameter("topic", "主题"));
        }
        try {
            ChatRequest chatRequest = ChatRequest.builder()
                .messages(Arrays.asList(
                    SystemMessage.from("You are a mind map expert. Return only valid JSON, no markdown, no explanation."),
                    UserMessage.from(prompt)
                ))
                .build();
            ChatResponse chatResponse = chatModel.chat(chatRequest);
            return chatResponse.aiMessage().text();
        } catch (Exception e) {
            LogUtils.warn("MindMapBufferLayer: AI call failed", e);
            return createSampleMindMapJSON(request.getParameter("topic", "主题"));
        }
    }

    /**
     * 兜底示例 JSON（AI 不可用时使用）
     */
    private String createSampleMindMapJSON(String topic) {
        return String.format(
            "{" +
            "  \"text\": \"%s\"," +
            "  \"children\": [" +
            "    {\"text\": \"分支 1\", \"children\": [{\"text\": \"子分支 1.1\"}, {\"text\": \"子分支 1.2\"}]}," +
            "    {\"text\": \"分支 2\", \"children\": [{\"text\": \"子分支 2.1\"}]}," +
            "    {\"text\": \"分支 3\"}" +
            "  ]" +
            "}",
            topic
        );
    }

    /**
     * 从 AI 响应创建思维导图节点
     */
    @SuppressWarnings("unchecked")
    private int createMindMapNodes(Map<String, Object> mindMapData) {
        try {
            // 获取 MMapController
            Controller controller = Controller.getCurrentController();
            if (controller == null) {
                LogUtils.warn("MindMapBufferLayer: Controller not available");
                return 0;
            }

            MModeController modeController = (MModeController) controller.getModeController();
            MMapController mapController = (MMapController) modeController.getMapController();
            MapModel mapModel = controller.getMap();

            if (mapModel == null) {
                LogUtils.warn("MindMapBufferLayer: No map is currently open");
                return 0;
            }

            // 设置根节点文本
            NodeModel rootNode = mapModel.getRootNode();
            String rootText = (String) mindMapData.get("text");
            if (rootText != null) {
                rootNode.setText(rootText);
                mapController.nodeChanged(rootNode);
            }

            // 递归创建子节点
            List<Map<String, Object>> children =
                (List<Map<String, Object>>) mindMapData.get("children");

            int[] nodeCount = {1}; // 包括根节点
            if (children != null) {
                createNodesRecursive(rootNode, children, mapController, nodeCount);
            }

            return nodeCount[0];
        } catch (Exception e) {
            LogUtils.warn("MindMapBufferLayer: failed to create mindmap nodes", e);
            return 0;
        }
    }

    /**
     * 递归创建节点
     */
    @SuppressWarnings("unchecked")
    private void createNodesRecursive(NodeModel parentNode,
                                      List<Map<String, Object>> children,
                                      MMapController mapController,
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
            List<Map<String, Object>> subChildren =
                (List<Map<String, Object>>) childData.get("children");
            if (subChildren != null && !subChildren.isEmpty()) {
                createNodesRecursive(childNode, subChildren, mapController, nodeCount);
            }
        }
    }
}