# Stub 接口完善计划

**目标**：让新后端完整调用插件的全部功能（包括缓冲层、AI 对话、AI 工具集）

**优先级**：🔴 最高优先级

**预计工作量**：2-3 天

---

## 📋 当前 Stub 接口清单

| 接口 | 状态 | 优先级 | 复用能力 |
|------|------|--------|---------|
| `POST /api/ai/chat` | ⚠️ Stub | 🔴 P0 | AIChatService |
| `POST /api/ai/expand-node` | ⚠️ Stub | 🟡 P1 | AIToolSet (CreateNodesTool) |
| `POST /api/ai/summarize` | ⚠️ Stub | 🟡 P1 | AIToolSet (CreateSummaryTool) |
| `POST /api/ai/tag` | ⚠️ Stub | 🟢 P2 | AIToolSet (AutoTagNodesTool) |
| `POST /api/ai/smart` | ✅ 已接入 | - | BufferLayerRouter |

---

## 🎯 实现方案

### 方案 A：在 AIChatPanel 中添加公开方法（推荐 ✅）

**优势**：
- 最小改动，复用现有逻辑
- 自动享受插件的所有能力（模型选择、工具集、上下文管理）
- 无需重复创建 AIChatService

**实现步骤**：

#### 步骤 1：在 AIChatPanel 中添加公开方法

```java
// AIChatPanel.java 中新增公开方法

/**
 * 供 REST API 调用的公开对话方法
 * @param message 用户消息
 * @param modelSelection 模型选择（可选，null 使用当前配置）
 * @return AI 回复
 */
public String sendChatMessage(String message, String modelSelection) {
    // 1. 保存当前模型选择
    String originalModel = configuration.getSelectedModelValue();
    
    try {
        // 2. 如果指定了模型，临时切换
        if (modelSelection != null && !modelSelection.isEmpty()) {
            configuration.setSelectedModelValue(modelSelection);
        }
        
        // 3. 确保 chatService 已初始化
        ensureChatService();
        if (chatService == null) {
            return "[错误] AI 服务未初始化，请检查 API Key 配置";
        }
        
        // 4. 调用 AI 服务
        String reply = chatService.chat(message);
        return reply;
        
    } catch (Exception e) {
        LogUtils.warn("REST API chat failed", e);
        return "[错误] " + e.getMessage();
    } finally {
        // 5. 恢复原始模型选择
        if (modelSelection != null && !modelSelection.isEmpty()) {
            configuration.setSelectedModelValue(originalModel);
        }
    }
}
```

#### 步骤 2：在 AiRestController 中调用

```java
// AiRestController.java 修改 executeChat 方法

private String executeChat(String message, String modelSelection) {
    // 直接调用 AIChatPanel 的公开方法
    return aiChatPanel.sendChatMessage(message, modelSelection);
}
```

---

### 方案 B：独立创建 AIChatService（适合需要完全控制）

**优势**：
- 完全独立于 Swing UI
- 可以自定义工具集和上下文

**劣势**：
- 需要重复创建服务实例
- 需要手动管理配置

**实现步骤**：

```java
// AiRestController.java 中添加

private AIChatService createChatServiceForAPI() {
    // 1. 读取配置
    AIProviderConfiguration configuration = new AIProviderConfiguration();
    
    // 2. 创建 ChatModel
    ChatModel chatModel = AIChatModelFactory.createChatLanguageModel(configuration);
    
    // 3. 创建工具集
    AIToolSet toolSet = new AIToolSetBuilder()
        .availableMaps(availableMaps)
        .build();
    
    // 4. 创建内存（无上下文或单轮上下文）
    ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
    
    // 5. 创建服务
    return new AIChatService(
        chatModel,
        toolSet,
        chatMemory,
        new ChatTokenUsageTracker(totals -> {}),
        null, // toolCallSummaryHandler
        () -> false, // cancellationSupplier
        null // tokenUsageConsumer
    );
}

private String executeChat(String message, String modelSelection) {
    try {
        AIChatService chatService = createChatServiceForAPI();
        return chatService.chat(message);
    } catch (Exception e) {
        LogUtils.warn("Failed to execute chat", e);
        return "[错误] " + e.getMessage();
    }
}
```

---

## 🔧 AI 工具集接入方案

### 节点扩展接口（expand-node）

**当前 Stub**：返回空结果

**实现方案**：

```java
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
        MapModel mapModel = resolveMapModel(body);
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
        response.put("nodeId", nodeId);
        response.put("createdNodes", createdNodes);
        response.put("summary", "成功创建 " + createdNodes.size() + " 个子节点");
        sendJson(exchange, 200, response);
    } catch (Exception e) {
        LogUtils.warn("AiRestController.handleExpandNode error", e);
        sendError(exchange, 500, "Internal server error: " + e.getMessage());
    }
}

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
        List<Map<String, Object>> nodeDataList = objectMapper.readValue(cleanedJson, List.class);
        
        // 获取 MMapController
        MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
        
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

@SuppressWarnings("unchecked")
private void createChildNodesRecursive(NodeModel parentNode, List<Map<String, Object>> children, 
                                       MMapController mapController, List<Map<String, Object>> createdNodes) {
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
```

---

### 分支摘要接口（summarize）

**实现方案**：

```java
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

        // 1. 获取目标节点及其子节点内容
        MapModel mapModel = resolveMapModel(body);
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
        if (writeToNote) {
            MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
            targetNode.setDetails(summary);
            mapController.nodeChanged(targetNode);
        }

        // 5. 返回结果
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("nodeId", nodeId);
        response.put("summary", summary);
        response.put("wordCount", summary.length());
        response.put("writtenToNote", writeToNote);
        sendJson(exchange, 200, response);
    } catch (Exception e) {
        LogUtils.warn("AiRestController.handleSummarize error", e);
        sendError(exchange, 500, "Internal server error: " + e.getMessage());
    }
}

private String collectBranchContent(NodeModel node) {
    StringBuilder content = new StringBuilder();
    collectNodeContentRecursive(node, content, 0);
    return content.toString();
}

private void collectNodeContentRecursive(NodeModel node, StringBuilder content, int depth) {
    if (node == null) return;
    
    String indent = "  ".repeat(depth);
    content.append(indent).append("- ").append(node.getText()).append("\n");
    
    for (NodeModel child : node.getChildren()) {
        collectNodeContentRecursive(child, content, depth + 1);
    }
}

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
```

---

### 自动标签接口（tag）

**实现方案**：

```java
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
        MapModel mapModel = resolveMapModel(body);
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

        // 3. 调用 AI 提取标签
        String prompt = buildTagPrompt(nodeContents);
        String aiResponse = aiChatPanel.sendChatMessage(prompt, null);

        // 4. 解析 AI 响应
        List<Map<String, Object>> results = parseTagResults(aiResponse, nodeContents);

        // 5. 返回结果
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("message", "成功为 " + results.size() + " 个节点提取标签");
        sendJson(exchange, 200, response);
    } catch (Exception e) {
        LogUtils.warn("AiRestController.handleTag error", e);
        sendError(exchange, 500, "Internal server error: " + e.getMessage());
    }
}

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

private List<Map<String, Object>> parseTagResults(String aiResponse, List<Map<String, String>> nodeContents) {
    List<Map<String, Object>> results = new ArrayList<>();
    
    try {
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
```

---

## 📝 实施步骤

### Phase 1：AI 对话接口（P0，半天）

1. ✅ 在 AIChatPanel 中添加 `sendChatMessage()` 公开方法
2. ✅ 修改 `AiRestController.executeChat()` 调用新方法
3. ✅ 测试对话功能

### Phase 2：节点扩展接口（P1，1天）

1. ✅ 实现 `handleExpandNode()` 完整逻辑
2. ✅ 添加提示词构建方法
3. ✅ 添加节点解析和创建方法
4. ✅ 测试节点扩展功能

### Phase 3：分支摘要接口（P1，半天）

1. ✅ 实现 `handleSummarize()` 完整逻辑
2. ✅ 添加分支内容收集方法
3. ✅ 添加摘要提示词构建方法
4. ✅ 测试摘要功能

### Phase 4：自动标签接口（P2，半天）

1. ✅ 实现 `handleTag()` 完整逻辑
2. ✅ 添加标签提示词构建方法
3. ✅ 添加标签解析方法
4. ✅ 测试标签功能

### Phase 5：测试与文档（0.5天）

1. ✅ 编写接口测试用例
2. ✅ 更新 API 文档
3. ✅ 编写使用示例

---

## ⚠️ 注意事项

### 1. 线程安全问题

- `AIChatPanel` 是 Swing 组件，部分方法需要在 EDT 线程执行
- REST API 调用在独立线程，需注意线程切换
- 使用 `SwingUtilities.invokeLater()` 处理 UI 更新

### 2. 配置管理

- 临时切换模型后必须恢复原始配置
- 避免影响 Swing UI 的用户体验

### 3. 错误处理

- AI 服务可能返回无效 JSON，需要容错处理
- 节点操作可能失败（如节点不存在），需要友好提示

### 4. 性能优化

- 批量创建节点时，考虑一次性刷新 UI
- 大文本摘要时，考虑异步处理

---

## 🎯 验收标准

1. ✅ 所有 Stub 接口返回真实数据
2. ✅ AI 对话能正常调用插件的 AIChatService
3. ✅ 节点扩展能创建真实的思维导图节点
4. ✅ 分支摘要能生成准确的摘要内容
5. ✅ 自动标签能提取合理的关键词
6. ✅ 所有接口都有完整的错误处理
7. ✅ 不影响 Swing UI 的正常使用
8. ✅ 提供完整的 API 文档和示例

---

## 📚 参考文件

- `AIChatPanel.java` - AI 对话面板（包含 chatService）
- `AIChatService.java` - AI 对话服务
- `AIToolSetBuilder.java` - AI 工具集构建器
- `NodeRestController.java` - 节点操作参考
- `MindMapBufferLayer.java` - 缓冲层参考
