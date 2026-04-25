# Freeplane AI 插件 LangChain4j 开发指南

> 本文档参考 [LangChain4j 官方文档](https://docs.langchain4j.dev/) 编写，专门针对 Freeplane AI 插件的 LangChain4j 集成和使用方式。

---

## 目录

- [1. 快速开始](#1-快速开始)
- [2. 架构概览](#2-架构概览)
- [3. 聊天模型集成](#3-聊天模型集成)
- [4. 工具调用系统](#4-工具调用系统)
- [5. 对话记忆管理](#5-对话记忆管理)
- [6. Token 使用统计](#6-token-使用统计)
- [7. 可观测性与监听器](#7-可观测性与监听器)
- [8. 错误处理](#8-错误处理)
- [9. 性能优化](#9-性能优化)
- [10. 常见问题](#10-常见问题)
- [11. 最佳实践](#11-最佳实践)

---

## 1. 快速开始

### 1.1 环境要求

- **Java 版本**: Java 17+（AI 插件）
- **LangChain4j 版本**: `1.11.0`
- **构建工具**: Gradle 8.14

### 1.2 依赖配置

在 `freeplane_plugin_ai/build.gradle` 中配置：

```groovy
dependencies {
    // LangChain4j 核心
    lib 'dev.langchain4j:langchain4j:1.11.0'
    
    // OpenAI 兼容接口（支持 OpenRouter）
    lib 'dev.langchain4j:langchain4j-open-ai:1.11.0'
    
    // Ollama 本地模型
    lib 'dev.langchain4j:langchain4j-ollama:1.11.0'
    
    // Google Gemini
    lib 'dev.langchain4j:langchain4j-google-ai-gemini:1.11.0'
}
```

**为什么使用 `lib` 而不是 `implementation`？**

- `lib` 是 Freeplane 自定义的配置，用于 OSGi 插件依赖管理
- 这些依赖会被打包到插件的 `lib/` 目录
- OSGi Bundle 通过 `Bundle-ClassPath` 引用这些 JAR

### 1.3 编译配置

```groovy
java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.withType(JavaCompile) {
    options.compilerArgs << '-parameters'  // 保留参数名（LangChain4j 工具调用需要）
}

tasks.named('compileJava', JavaCompile) {
    options.release = 17
}
```

**重要**：`-parameters` 编译参数是必须的，因为 LangChain4j 通过反射获取工具方法的参数名。

---

## 2. 架构概览

### 2.1 核心组件

```
┌────────────────────────────────────────────────────────────┐
│                    AIChatPanel (UI)                        │
│  - 用户输入框                                               │
│  - 对话历史显示                                             │
│  - 工具调用摘要展示                                         │
└─────────────────────┬──────────────────────────────────────┘
                      │
                      ▼
┌────────────────────────────────────────────────────────────┐
│                   AIChatService                            │
│  - AiServices 构建器                                        │
│  - 监听器注册（Token、工具、错误）                           │
│  - 工具执行器工厂                                           │
└─────────────────────┬──────────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
┌──────────────────┐    ┌──────────────────┐
│   ChatModel      │    │  ChatMemory      │
│  - OpenAI        │    │  - 对话历史管理   │
│  - Ollama        │    │  - Token 管理     │
│  - Gemini        │    │  - 窗口滑动       │
└──────────────────┘    └──────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────────┐
│                   AIToolSet (工具集)                        │
│  - ReadNodesWithDescendantsTool    - 读取节点              │
│  - CreateNodesTool                 - 创建节点              │
│  - EditNodesTool                   - 编辑节点              │
│  - MoveNodesTool                   - 移动节点              │
│  - DeleteNodesTool                 - 删除节点              │
│  - SearchNodesTool                 - 搜索节点              │
│  - ... (12+ 工具)                                          │
└────────────────────────────────────────────────────────────┘
```

### 2.2 数据流

```
用户输入
  ↓
AIChatService.chat(message)
  ↓
AiServices 调用 ChatModel
  ↓
ChatModel 返回响应（可能包含 ToolExecutionRequest）
  ↓
如果有工具调用：
  ├→ ToolExecutor 执行工具
  ├→ 返回 ToolExecutionResultMessage
  └→ ChatModel 处理结果，返回最终响应
  ↓
TokenUsage 回调 → 更新 UI
  ↓
监听器回调 → 记录工具执行日志
  ↓
返回文本响应给用户
```

---

## 3. 聊天模型集成

### 3.1 模型工厂模式

`AIChatModelFactory` 负责创建不同提供商的 `ChatModel`：

```java
public class AIChatModelFactory {
    
    public static final String PROVIDER_NAME_OPENROUTER = "openrouter";
    public static final String PROVIDER_NAME_GEMINI = "gemini";
    public static final String PROVIDER_NAME_OLLAMA = "ollama";
    
    public static ChatModel createChatLanguageModel(AIProviderConfiguration configuration) {
        AIModelSelection selection = AIModelSelection.fromSelectionValue(
            configuration.getSelectedModelValue()
        );
        
        String providerName = selection.getProviderName();
        String modelName = selection.getModelName();
        
        // 根据提供商创建对应的模型
        if (PROVIDER_NAME_OPENROUTER.equalsIgnoreCase(providerName)) {
            return createOpenRouterModel(configuration, modelName);
        }
        if (PROVIDER_NAME_GEMINI.equalsIgnoreCase(providerName)) {
            return createGeminiModel(configuration, modelName);
        }
        if (PROVIDER_NAME_OLLAMA.equalsIgnoreCase(providerName)) {
            return createOllamaModel(configuration, modelName);
        }
        
        throw new IllegalArgumentException("Unknown provider: " + providerName);
    }
}
```

### 3.2 OpenRouter 模型配置

**适用场景**：通过 OpenRouter 访问多种 AI 模型（GPT-4、Claude、Llama 等）

```java
private static ChatModel createOpenRouterModel(
    AIProviderConfiguration configuration, 
    String modelName
) {
    return OpenAiChatModel.builder()
        .baseUrl(getOpenrouterServiceAddress(configuration))
        .apiKey(configuration.getOpenRouterKey())
        .modelName(modelName)
        .maxRetries(2)  // 自动重试 2 次
        .build();
}

private static String getOpenrouterServiceAddress(AIProviderConfiguration configuration) {
    String serviceAddress = configuration.getOpenrouterServiceAddress();
    if (serviceAddress == null || serviceAddress.isEmpty()) {
        return "https://openrouter.ai/api/v1";  // 默认地址
    }
    return serviceAddress;
}
```

**配置参数**：

| 参数 | 说明 | 示例 |
|------|------|------|
| `baseUrl` | API 端点 | `https://openrouter.ai/api/v1` |
| `apiKey` | API 密钥 | `sk-or-v1-...` |
| `modelName` | 模型名称 | `openai/gpt-4o`, `anthropic/claude-3.5-sonnet` |
| `maxRetries` | 失败重试次数 | `2` |

**支持的模型**：

```
openai/gpt-4o
openai/gpt-4o-mini
anthropic/claude-3.5-sonnet
anthropic/claude-3-opus
google/gemini-pro-1.5
meta-llama/llama-3.1-70b-instruct
mistralai/mistral-large
...
```

### 3.3 Google Gemini 模型配置

**适用场景**：使用 Google Gemini 原生 API，支持思考模式

```java
private static ChatModel createGeminiModel(
    AIProviderConfiguration configuration, 
    String modelName
) {
    GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder = 
        GoogleAiGeminiChatModel.builder()
            .apiKey(configuration.getGeminiKey())
            .modelName(modelName)
            .maxRetries(2);
    
    // 可选：自定义服务端点
    String serviceAddress = configuration.getGeminiServiceAddress();
    if (serviceAddress != null && !serviceAddress.isEmpty()) {
        builder.baseUrl(serviceAddress);
    }
    
    // Gemini 3 系列支持思考模式
    if (modelName != null && modelName.startsWith("gemini-3-")) {
        GeminiThinkingConfig thinkingConfig = GeminiThinkingConfig.builder()
            .includeThoughts(true)  // 包含思考过程
            .build();
        
        builder.thinkingConfig(thinkingConfig)
               .returnThinking(true)   // 返回思考内容
               .sendThinking(true);    // 发送思考内容
    }
    
    return builder.build();
}
```

**思考模式说明**：

```
用户问题 → AI 内部思考 → 返回最终答案
         ↑
   可配置是否展示给用户
```

**Gemini 模型列表**：

```
gemini-2.0-flash
gemini-2.0-pro
gemini-3-flash
gemini-3-pro
gemini-3-flash-thinking  # 支持思考模式
gemini-3-pro-thinking    # 支持思考模式
```

### 3.4 Ollama 本地模型配置

**适用场景**：本地部署开源模型，保护隐私，无 API 费用

```java
private static ChatModel createOllamaModel(
    AIProviderConfiguration configuration, 
    String modelName
) {
    OllamaChatModel.OllamaChatModelBuilder builder = 
        OllamaChatModel.builder()
            .baseUrl(getOllamaServiceAddress(configuration))
            .modelName(modelName)
            .maxRetries(2);
    
    // 可选：自定义请求头（如认证）
    Map<String, String> requestHeaders = configuration.getOllamaRequestHeaders();
    if (!requestHeaders.isEmpty()) {
        builder.customHeaders(requestHeaders);
    }
    
    return builder.build();
}

private static String getOllamaServiceAddress(AIProviderConfiguration configuration) {
    return configuration.getOllamaServiceAddress();
    // 默认：http://localhost:11434
}
```

**常用本地模型**：

```
llama3.1:8b
llama3.1:70b
qwen2.5:7b
qwen2.5:72b
mistral:7b
deepseek-r1:8b        # 支持思考
deepseek-r1:70b       # 支持思考
phi3:3.8b
```

**Ollama 安装**：

```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh

# Windows
# 下载安装包：https://ollama.com/download/windows

# 下载模型
ollama pull llama3.1:8b
ollama pull qwen2.5:7b

# 启动服务
ollama serve
```

### 3.5 模型选择建议

| 场景 | 推荐模型 | 优势 | 成本 |
|------|---------|------|------|
| 高质量输出 | GPT-4o / Claude 3.5 | 能力强，工具调用准确 | 高 |
| 性价比 | GPT-4o-mini / Llama 3.1 | 速度快，成本低 | 低 |
| 隐私保护 | Ollama 本地部署 | 数据不出本地 | 硬件成本 |
| 长上下文 | Gemini 2.0 Pro | 1M tokens 上下文 | 中 |
| 推理能力 | DeepSeek R1 | 思考模式，逻辑强 | 低/免费 |

---

## 4. 工具调用系统

### 4.1 工具定义

LangChain4j 使用 `@Tool` 注解定义工具：

```java
public class AIToolSet {
    
    @Tool("Read nodes with descendants.")
    public ReadNodesWithDescendantsResponse readNodesWithDescendants(
        ReadNodesWithDescendantsRequest request
    ) {
        try {
            // 实现逻辑
            return response;
        } catch (Exception e) {
            return ReadNodesWithDescendantsResponse.error(e.getMessage());
        }
    }
    
    @Tool("Delete nodes by identifier.")
    public DeleteNodesResponse deleteNodes(DeleteNodesRequest request) {
        // 实现逻辑
    }
    
    @Tool("Create nodes and subtrees relative to an anchor node.\n"
        + "Optional fields override defaults. Omit them to keep defaults.\n"
        + "Each optional field is an intentional override. Include it only when the specific value is justified; ")
    public CreateNodesResponse createNodes(CreateNodesRequest request) {
        // 实现逻辑
    }
}
```

**工具注解说明**：

- `@Tool` 的值是工具描述，会被发送给 AI 模型
- 描述越清晰，AI 调用工具的准确性越高
- 方法参数会被自动转换为 JSON Schema

### 4.2 工具执行器工厂

Freeplane 使用自定义的工具执行器工厂：

```java
public class ToolExecutorFactory {
    
    private final boolean wrapToolArgumentsExceptions;
    private final boolean propagateToolExecutionExceptions;
    private final Supplier<Boolean> cancellationSupplier;
    
    public ToolExecutorRegistry createRegistry(AIToolSet toolSet) {
        // 创建工具执行器映射
        Map<ToolSpecification, ToolExecutor> executors = new LinkedHashMap<>();
        
        // 遍历所有 @Tool 方法
        for (Method method : toolSet.getClass().getMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                // 创建工具规格
                ToolSpecification spec = ToolSpecification.from(method);
                
                // 创建执行器（带异常处理）
                ToolExecutor executor = createExecutor(method, toolSet);
                
                executors.put(spec, executor);
            }
        }
        
        return new ToolExecutorRegistry(executors);
    }
    
    private ToolExecutor createExecutor(Method method, Object target) {
        return new DefaultToolExecutor(target, method) {
            @Override
            public ToolExecutionResult execute(
                ToolExecutionRequest toolExecutionRequest,
                InvocationContext context
            ) {
                try {
                    // 检查取消
                    if (cancellationSupplier != null && cancellationSupplier.get()) {
                        throw new ToolExecutionException("Tool execution cancelled");
                    }
                    
                    // 执行工具
                    return super.execute(toolExecutionRequest, context);
                    
                } catch (Exception e) {
                    if (propagateToolExecutionExceptions) {
                        throw e;
                    }
                    // 返回错误结果而非抛出异常
                    return ToolExecutionResult.text("Error: " + e.getMessage());
                }
            }
        };
    }
}
```

### 4.3 工具调用流程

```
1. AI 模型决定调用工具
   ↓
2. 返回 ToolExecutionRequest
   {
     "name": "createNodes",
     "arguments": "{\"anchorNodeId\":\"n1\",\"nodes\":[...]}"
   }
   ↓
3. ToolExecutorRegistry 查找执行器
   ↓
4. 执行工具方法
   ↓
5. 返回 ToolExecutionResult
   {
     "toolCallId": "call_123",
     "text": "Created 5 nodes successfully"
   }
   ↓
6. 结果发送给 AI 模型
   ↓
7. AI 模型生成最终响应
```

### 4.4 Freeplane 工具集

项目包含 12+ 工具：

#### 读取工具

```java
@Tool("Read nodes with descendants.")
ReadNodesWithDescendantsResponse readNodesWithDescendants(
    ReadNodesWithDescendantsRequest request
)

@Tool("Fetch nodes for editing. editableContentFields (required): TEXT, DETAILS, NOTE, ATTRIBUTES, TAGS, ICONS.")
FetchNodesForEditingResponse fetchNodesForEditing(
    FetchNodesForEditingRequest request
)

@Tool("Get breadcrumbs for a node.")
BreadcrumbsResponse getBreadcrumbs(BreadcrumbsRequest request)
```

#### 创建工具

```java
@Tool("Create nodes and subtrees relative to an anchor node.")
CreateNodesResponse createNodes(CreateNodesRequest request)
```

#### 编辑工具

```java
@Tool("Edit node content through undo-aware controllers.")
EditNodesResponse editNodes(EditNodesRequest request)
```

#### 移动工具

```java
@Tool("Move nodes relative to an anchor node.")
MoveNodesResponse moveNodes(MoveNodesRequest request)

@Tool("Create summary content and a summary bracket for a summarized range.")
CreateSummaryResponse createSummary(CreateSummaryRequest request)

@Tool("Move existing nodes to become summary content for a summarized range.")
MoveNodesIntoSummaryResponse moveNodesIntoSummary(
    MoveNodesIntoSummaryRequest request
)
```

#### 删除工具

```java
@Tool("Delete nodes by identifier.")
DeleteNodesResponse deleteNodes(DeleteNodesRequest request)
```

#### 搜索工具

```java
@Tool("Search nodes by content.")
SearchNodesResponse searchNodes(SearchNodesRequest request)
```

#### 选择工具

```java
@Tool("Get identifiers for the currently selected map and node.")
SelectionIdentifiersResponse getSelectedMapAndNodeIdentifiers(
    SelectionIdentifiersRequest request
)

@Tool("Select a single node by identifier and make it visible in the current view.")
SelectSingleNodeResponse selectSingleNode(SelectSingleNodeRequest request)
```

#### 列表工具

```java
@Tool("Lists application-wide icons (not map-specific).")
ListResponse listAvailableIcons()

@Tool("Lists styles defined in the target map.")
ListResponse listMapStyles(String mapIdentifier)
```

#### 连接器工具

```java
@Tool("Edit connectors by source and target node identifier.")
ConnectorEditResponse editConnectors(ConnectorEditRequest request)
```

### 4.5 工具调用最佳实践

#### ✅ 推荐做法

```java
// 1. 清晰的工具描述
@Tool("Create nodes and subtrees relative to an anchor node.\n"
    + "Position can be: FIRST_CHILD, LAST_CHILD, BEFORE, AFTER.\n"
    + "Returns created node identifiers.")

// 2. 使用请求/响应对象
public CreateNodesResponse createNodes(CreateNodesRequest request) {
    // 结构化参数
}

// 3. 异常处理
try {
    // 工具逻辑
    return CreateNodesResponse.success(createdNodes);
} catch (Exception e) {
    return CreateNodesResponse.error(e.getMessage());
}

// 4. 记录工具调用
toolCallSummaryHandler.handleToolCallSummary(
    new ToolCallSummary("createNodes", "Created 5 nodes", false, ToolCaller.CHAT)
);
```

#### ❌ 避免做法

```java
// ❌ 模糊的描述
@Tool("Create nodes")

// ❌ 过多参数（难以理解）
public String createNodes(
    String anchorId,
    String position,
    String text,
    String details,
    String note,
    String style,
    // ... 20+ 参数
)

// ❌ 抛出未处理异常
public CreateNodesResponse createNodes(CreateNodesRequest request) {
    // 可能抛出 NullPointerException
    NodeModel node = request.getNode();
    node.getText();  // NPE!
}
```

---

## 5. 对话记忆管理

### 5.1 ChatMemory 接口

LangChain4j 提供 `ChatMemory` 接口管理对话历史：

```java
public interface ChatMemory {
    Object id();                    // 记忆 ID
    void add(ChatMessage message);  // 添加消息
    List<ChatMessage> messages();   // 获取消息列表
    void clear();                   // 清空记忆
}
```

### 5.2 Freeplane 自定义实现

`AssistantProfileChatMemory` 实现了高级记忆管理：

```java
public class AssistantProfileChatMemory implements ChatMemory {
    
    private final Object id;
    private final Function<Object, Integer> maxTokensProvider;  // 动态 Token 限制
    private final ChatTokenEstimator tokenEstimator;            // Token 估算器
    private final int protectedRecentTurnCount;                 // 保护的最近轮数
    private final double historicalToolTokenShare;              // 历史工具 Token 占比
    
    private final List<ChatMessage> conversationMessages = new ArrayList<>();
    private final List<HistoricalToolCycle> hiddenHistoricalToolCycles = new ArrayList<>();
    private int activeStartIndex;  // 活动窗口起始位置
    private final List<Integer> turnEndIndexes = new ArrayList<>();
    
    @Override
    public void add(ChatMessage message) {
        if (message == null) return;
        
        // 特殊消息类型处理
        if (message instanceof TranscriptHiddenSystemMessage) {
            // 隐藏的系统指令
            addConversationMessage(message);
            addConversationMessage(new InstructionAckMessage());
            return;
        }
        
        if (message instanceof AssistantProfileSwitchMessage) {
            // 助手配置切换
            addConversationMessage(message);
            addConversationMessage(new InstructionAckMessage());
            return;
        }
        
        // 普通消息
        addConversationMessage(message);
        rebuildTurnBoundaries();  // 重建轮次边界
    }
    
    @Override
    public List<ChatMessage> messages() {
        return buildMessages(activeConversationEndIndex());
    }
}
```

### 5.3 Token 窗口管理

**问题**：AI 模型有上下文窗口限制（如 128K tokens），超长对话需要裁剪。

**解决方案**：滑动窗口 + Token 估算

```java
private boolean evictIfNeededAfterResponse() {
    int maxTokens = maxTokensProvider.apply(id);
    long estimatedTokens = estimateTotalTokensForActiveWindow();
    
    // 如果超过限制，开始驱逐旧消息
    if (estimatedTokens < maxTokens) {
        return false;
    }
    
    // 目标：缩减到 maxTokens / 4
    int resetTargetTokens = maxTokens / 4;
    int minimumTurnBlocksToKeep = minimumTurnBlocksToKeep(maxTokens);
    
    // 逐轮驱逐，直到低于目标
    while (selection.visibleTokenCount() > resetTargetTokens) {
        if (!canAdvanceWindowByTurnWithMinimumRetention(minimumTurnBlocksToKeep)) {
            break;
        }
        if (!advanceWindowByOneTurn()) {
            break;
        }
    }
    
    return true;
}

private boolean advanceWindowByOneTurn() {
    int endIndex = activeConversationEndIndex();
    int startIndex = Math.min(activeStartIndex, endIndex);
    
    // 找到下一个轮次结束位置
    int nextTurnEnd = findNextTurnEndAfter(startIndex);
    if (nextTurnEnd <= startIndex) {
        return false;
    }
    
    // 移动窗口起始位置
    hiddenHistoricalToolCycles.clear();
    activeStartIndex = nextTurnEnd;
    rebuildTurnBoundaries();
    
    return true;
}
```

### 5.4 工具调用历史压缩

**问题**：工具调用消息占用大量 Token（请求 + 结果）。

**解决方案**：隐藏历史工具调用，只保留摘要

```java
private VisibleContextSelection selectVisibleContext(
    int conversationEndIndex, 
    int targetTokens
) {
    // 1. 计算历史 Token 预算
    long historicalTokens = Math.max(0L, (long) targetTokens - protectedTokens);
    long historicalToolTokenCap = (long) Math.floor(
        historicalTokens * historicalToolTokenShare  // 默认 50%
    );
    
    // 2. 收集历史工具调用周期
    List<HistoricalToolCycle> historicalCycles = collectHistoricalToolCycles(
        historicalEndIndex
    );
    
    // 3. 裁剪超出预算的工具调用
    List<HistoricalToolCycle> hiddenCycles = trimHistoricalToolCycles(
        historicalCycles, 
        historicalToolTokenCap
    );
    
    // 4. 构建可见上下文选择
    return visibleContextSelectionForHiddenCycles(endIndex, hiddenCycles);
}
```

### 5.5 撤销/重做支持

```java
public boolean canUndo() {
    return currentTurnCount > firstActiveTurnIndex();
}

public String undo() {
    if (!canUndo()) {
        return "";
    }
    
    int turnIndex = currentTurnCount - 1;
    int from = turnIndex == 0 ? 0 : turnEndIndexes.get(turnIndex - 1);
    from = Math.max(from, activeStartIndex);
    int to = turnEndIndexes.get(turnIndex);
    
    currentTurnCount = turnIndex;
    rebalanceActiveWindowForCurrentTurnRange();
    
    // 返回用户消息（用于显示）
    return findUserMessageInRange(from, to);
}

public void redo() {
    if (!canRedo()) {
        return;
    }
    currentTurnCount++;
    rebalanceActiveWindowForCurrentTurnRange();
}
```

### 5.6 消息类型

Freeplane 定义了多种消息类型：

| 消息类型 | 用途 | 是否发送 |
|---------|------|---------|
| `UserMessage` | 用户输入 | ✅ |
| `AiMessage` | AI 响应 | ✅ |
| `ToolExecutionResultMessage` | 工具执行结果 | ✅ |
| `SystemMessage` | 系统指令 | ✅ |
| `AssistantProfileSwitchMessage` | 助手配置切换 | ✅ |
| `TranscriptHiddenSystemMessage` | 隐藏的系统指令 | ❌（仅本地） |
| `RemovedForSpaceSystemMessage` | 空间不足标记 | ❌（仅本地） |
| `ToolCallSummaryMessage` | 工具调用摘要 | ❌（仅本地） |
| `InstructionAckMessage` | 指令确认 | ❌（仅本地） |

---

## 6. Token 使用统计

### 6.1 TokenUsage 追踪

```java
public class ChatTokenUsageTracker {
    
    private TokenUsage lastTokenUsage;
    private final Consumer<TokenUsage> tokenUsageConsumer;
    
    public void onResponseTokenUsage(TokenUsage tokenUsage) {
        this.lastTokenUsage = tokenUsage;
        
        if (tokenUsageConsumer != null) {
            tokenUsageConsumer.accept(tokenUsage);
        }
    }
    
    public void logToolExecuted(ToolExecutedEvent event) {
        // 记录工具调用的 Token 使用
        TokenUsage toolTokenUsage = event.tokenUsage();
        if (toolTokenUsage != null) {
            // 累加工具调用 Token
        }
    }
}
```

### 6.2 TokenUsage 数据结构

```java
public class TokenUsage {
    private final int inputTokenCount;    // 输入 Token 数
    private final int outputTokenCount;   // 输出 Token 数
    private final int totalTokenCount;    // 总 Token 数
    
    public int inputTokenCount() { return inputTokenCount; }
    public int outputTokenCount() { return outputTokenCount; }
    public int totalTokenCount() { return totalTokenCount; }
}
```

### 6.3 Token 估算器

使用 `OpenAiTokenCountEstimator` 估算消息 Token 数：

```java
private static class ChatTokenEstimator {
    
    private static final String FALLBACK_MODEL_NAME = "gpt-4o-mini";
    
    private final Supplier<String> modelNameProvider;
    private OpenAiTokenCountEstimator estimator;
    private String activeModelName;
    
    int estimateTokenCountInMessage(ChatMessage message) {
        OpenAiTokenCountEstimator activeEstimator = estimator();
        try {
            return activeEstimator.estimateTokenCountInMessage(message);
        } catch (RuntimeException error) {
            return 0;  // 估算失败返回 0
        }
    }
    
    private OpenAiTokenCountEstimator estimator() {
        String modelName = normalizeModelName(modelNameProvider.get());
        
        // 缓存 Estimator（避免重复创建）
        if (estimator == null || !modelName.equals(activeModelName)) {
            estimator = buildEstimator(modelName);
            activeModelName = modelName;
        }
        
        return estimator;
    }
    
    private OpenAiTokenCountEstimator buildEstimator(String modelName) {
        try {
            return new OpenAiTokenCountEstimator(modelName);
        } catch (IllegalArgumentException error) {
            // 模型不支持，使用备用模型
            return new OpenAiTokenCountEstimator(FALLBACK_MODEL_NAME);
        }
    }
}
```

### 6.4 性能指标

在 UI 中展示 Token 使用信息：

```
输入: 1,234 tokens
输出: 567 tokens
总计: 1,801 tokens

工具调用: 3 次
工具 Token: 2,345 tokens
```

**估算实际费用**（以 OpenAI GPT-4o 为例）：

```
输入: $2.50 / 1M tokens
输出: $10.00 / 1M tokens

本次对话费用:
  输入: 1,234 / 1,000,000 × $2.50 = $0.003
  输出: 567 / 1,000,000 × $10.00 = $0.006
  总计: $0.009
```

---

## 7. 可观测性与监听器

### 7.1 AiServiceListener 接口

LangChain4j 提供事件监听器：

```java
.registerListener(new AiServiceListener<AiServiceErrorEvent>() {
    @Override
    public Class<AiServiceErrorEvent> getEventClass() {
        return AiServiceErrorEvent.class;
    }
    
    @Override
    public void onEvent(AiServiceErrorEvent event) {
        event.error().printStackTrace();
    }
})

.registerListener(new AiServiceListener<AiServiceResponseReceivedEvent>() {
    @Override
    public Class<AiServiceResponseReceivedEvent> getEventClass() {
        return AiServiceResponseReceivedEvent.class;
    }
    
    @Override
    public void onEvent(AiServiceResponseReceivedEvent event) {
        if (tokenUsageConsumer != null) {
            tokenUsageConsumer.accept(event.response().tokenUsage());
        }
    }
})

.registerListener(new AiServiceListener<ToolExecutedEvent>() {
    @Override
    public Class<ToolExecutedEvent> getEventClass() {
        return ToolExecutedEvent.class;
    }
    
    @Override
    public void onEvent(ToolExecutedEvent event) {
        chatTokenUsageTracker.logToolExecuted(event);
    }
});
```

### 7.2 事件类型

| 事件类型 | 触发时机 | 用途 |
|---------|---------|------|
| `AiServiceErrorEvent` | AI 服务错误 | 错误日志、用户提示 |
| `AiServiceResponseReceivedEvent` | 收到 AI 响应 | Token 统计、响应处理 |
| `ToolExecutedEvent` | 工具执行完成 | 工具调用日志、摘要 |

### 7.3 工具调用摘要

```java
public class ToolCallSummaryHandler {
    
    public void handleToolCallSummary(ToolCallSummary summary) {
        // 格式化摘要
        String formattedText = ToolCallSummaryFormatter.format(summary);
        
        // 添加到对话记忆
        chatMemory.addToolCallSummary(
            formattedText, 
            summary.getToolCaller()
        );
        
        // 更新 UI
        updateToolCallSummaryUI(formattedText);
    }
}

public class ToolCallSummary {
    private final String toolName;
    private final String summaryText;
    private final boolean isError;
    private final ToolCaller toolCaller;
    
    // 示例: "tool=createNodes, Created 5 nodes under 'Project'"
}
```

---

## 8. 错误处理

### 8.1 工具参数错误处理

```java
private ToolArgumentsErrorHandler buildToolArgumentsErrorHandler() {
    return (error, context) -> {
        String errorMessage = isNullOrBlank(error.getMessage()) 
            ? error.getClass().getName() 
            : error.getMessage();
        
        String toolName = context == null 
            ? "unknown tool" 
            : context.toolExecutionRequest().name();
        
        String arguments = context == null 
            ? "" 
            : context.toolExecutionRequest().arguments();
        
        // 发布错误摘要
        publishToolArgumentsErrorSummary(toolName, arguments, errorMessage);
        
        // 返回错误结果（不中断对话）
        return ToolErrorHandlerResult.text(
            "Tool arguments error for " + toolName + ": " + errorMessage
        );
    };
}

private void publishToolArgumentsErrorSummary(
    String toolName, 
    String arguments, 
    String errorMessage
) {
    if (toolCallSummaryHandler == null) {
        return;
    }
    
    LogUtils.info(buildToolArgumentsErrorLog(toolName, arguments, errorMessage));
    
    String summaryText = "tool arguments error: tool=" + sanitizeSummaryValue(toolName);
    String safeArguments = sanitizeSummaryValue(arguments);
    
    if (!safeArguments.isEmpty()) {
        summaryText = summaryText + ", arguments=" + safeArguments;
    }
    
    String safeErrorMessage = sanitizeSummaryValue(errorMessage);
    if (!safeErrorMessage.isEmpty()) {
        summaryText = summaryText + ", error=" + safeErrorMessage;
    }
    
    ToolCallSummary summary = new ToolCallSummary(
        "toolArgumentsError", 
        summaryText, 
        true,  // isError
        ToolCaller.CHAT
    );
    
    toolCallSummaryHandler.handleToolCallSummary(summary);
}
```

### 8.2 错误处理策略

| 错误类型 | 处理策略 | 用户影响 |
|---------|---------|---------|
| 工具参数错误 | 返回错误文本，继续对话 | 低（AI 会修正） |
| 工具执行异常 | 捕获异常，返回错误信息 | 低 |
| 模型 API 错误 | 自动重试（2次） | 中（可能延迟） |
| 网络超时 | 重试 + 用户提示 | 中 |
| Token 超限 | 窗口滑动，压缩历史 | 低（自动处理） |

### 8.3 取消支持

```java
ToolExecutorFactory toolExecutorFactory = new ToolExecutorFactory(
    true,   // wrapToolArgumentsExceptions
    true,   // propagateToolExecutionExceptions
    cancellationSupplier  // 取消检查
);

// 工具执行器中检查取消
if (cancellationSupplier != null && cancellationSupplier.get()) {
    throw new ToolExecutionException("Tool execution cancelled");
}
```

---

## 9. 性能优化

### 9.1 模型重试机制

```java
OpenAiChatModel.builder()
    .maxRetries(2)  // 失败自动重试 2 次
    .build();
```

**重试策略**：

- 第 1 次失败 → 等待 1 秒 → 重试
- 第 2 次失败 → 等待 2 秒 → 重试
- 第 3 次失败 → 抛出异常

### 9.2 Token 缓存

**消息 Token 估算缓存**：

```java
private OpenAiTokenCountEstimator estimator;
private String activeModelName;

private OpenAiTokenCountEstimator estimator() {
    String modelName = normalizeModelName(modelNameProvider.get());
    
    // 模型未变化时复用 Estimator
    if (estimator == null || !modelName.equals(activeModelName)) {
        estimator = buildEstimator(modelName);
        activeModelName = modelName;
    }
    
    return estimator;
}
```

### 9.3 工具调用优化

**减少工具调用次数**：

```java
// ❌ 多次调用
readNodes(node1);
readNodes(node2);
readNodes(node3);

// ✅ 批量读取
readNodesWithDescendants(ReadNodesWithDescendantsRequest.builder()
    .nodeIds(List.of(node1, node2, node3))
    .build());
```

**工具结果缓存**：

```java
// 缓存只读工具的结果（如 listAvailableIcons）
private Map<String, ListResponse> toolCache = new HashMap<>();

public ListResponse listAvailableIcons() {
    return toolCache.computeIfAbsent("icons", key -> {
        // 实际查询
        return queryIcons();
    });
}
```

### 9.4 对话窗口优化

**动态 Token 限制**：

```java
AssistantProfileChatMemory.builder()
    .dynamicMaxTokens(profileId -> {
        // 根据助手配置返回不同限制
        return profile.getMaxTokens();
    })
    .protectedRecentTurnCount(2)        // 保护最近 2 轮
    .historicalToolTokenShare(0.5)      // 工具 Token 占 50%
    .build();
```

---

## 10. 常见问题

### 10.1 编译问题

#### 问题：工具参数名丢失

```
警告：工具方法参数名不可用
```

**原因**：未启用 `-parameters` 编译参数。

**解决方案**：

```groovy
tasks.withType(JavaCompile) {
    options.compilerArgs << '-parameters'
}
```

#### 问题：LangChain4j 类找不到

```
ClassNotFoundException: dev.langchain4j.model.chat.ChatModel
```

**原因**：依赖未正确打包。

**解决方案**：

```bash
# 清理并重新构建
.\gradlew.bat :freeplane_plugin_ai:clean
.\gradlew.bat :freeplane_plugin_ai:build

# 检查依赖是否包含
ls BIN\plugins\org.freeplane.plugin.ai\lib\
# 应包含 langchain4j-1.11.0.jar
```

### 10.2 运行时问题

#### 问题：工具调用失败

```
Tool execution error: null
```

**排查步骤**：

```java
// 1. 检查工具方法是否抛出异常
@Tool("Delete nodes by identifier.")
public DeleteNodesResponse deleteNodes(DeleteNodesRequest request) {
    try {
        // 添加日志
        LogUtils.info("Deleting nodes: " + request.getNodeIds());
        
        // 检查空值
        if (request == null || request.getNodeIds() == null) {
            return DeleteNodesResponse.error("Invalid request");
        }
        
        // 执行删除
        return doDelete(request);
        
    } catch (Exception e) {
        LogUtils.error("Delete failed", e);
        return DeleteNodesResponse.error(e.getMessage());
    }
}

// 2. 检查工具注册
ToolExecutorRegistry registry = toolExecutorFactory.createRegistry(toolSet);
System.out.println("Registered tools: " + registry.getExecutorsBySpecification().size());
```

#### 问题：Token 估算不准确

**原因**：不同模型的 Token 计算方式不同。

**解决方案**：

```java
// 使用与实际模型匹配的 Estimator
OpenAiTokenCountEstimator estimator = new OpenAiTokenCountEstimator("gpt-4o");

// 或使用通用估算（不精确但兼容）
int estimatedTokens = text.length() / 4;  // 粗略估算：1 token ≈ 4 字符
```

#### 问题：对话历史丢失

**原因**：Token 窗口滑动过快。

**解决方案**：

```java
AssistantProfileChatMemory.builder()
    .protectedRecentTurnCount(3)        // 增加保护轮数
    .historicalToolTokenShare(0.7)      // 提高工具 Token 占比
    .maxTokens(100000)                  // 增加总限制
    .build();
```

### 10.3 API 问题

#### 问题：OpenRouter 返回 401

```
Unauthorized: Invalid API key
```

**解决方案**：

```java
// 检查 API Key 格式
// OpenRouter: sk-or-v1-xxxxx
// OpenAI: sk-xxxxx
// 不兼容！

// 验证 API Key
if (!apiKey.startsWith("sk-or-v1-")) {
    throw new IllegalArgumentException("Invalid OpenRouter API key format");
}
```

#### 问题：Ollama 连接失败

```
Connection refused: localhost:11434
```

**解决方案**：

```bash
# 1. 检查 Ollama 是否运行
ollama list

# 2. 启动服务
ollama serve

# 3. 测试连接
curl http://localhost:11434/api/tags

# 4. 检查模型是否下载
ollama pull llama3.1:8b
```

#### 问题：Gemini 思考模式不生效

```java
// 仅 Gemini 3 系列支持思考模式
if (modelName.startsWith("gemini-3-")) {
    GeminiThinkingConfig thinkingConfig = GeminiThinkingConfig.builder()
        .includeThoughts(true)
        .build();
    
    builder.thinkingConfig(thinkingConfig)
           .returnThinking(true)
           .sendThinking(true);
}
```

---

## 11. 最佳实践

### 11.1 工具设计

✅ **推荐做法**：

```java
// 1. 单一职责
@Tool("Read nodes with descendants.")
@Tool("Create nodes.")
@Tool("Delete nodes.")

// 2. 清晰的描述
@Tool("Edit node content through undo-aware controllers.\n"
    + "Before TEXT/DETAILS/NOTE edits, call fetchNodesForEditing first.\n"
    + "Values starting with <html> are HTML; others are plain text.")

// 3. 结构化请求/响应
public class CreateNodesRequest {
    private String anchorNodeId;
    private String position;
    private List<NodeData> nodes;
    // 验证逻辑
}

// 4. 错误处理
public CreateNodesResponse createNodes(CreateNodesRequest request) {
    try {
        validate(request);
        // 执行逻辑
        return CreateNodesResponse.success(createdNodes);
    } catch (ValidationException e) {
        return CreateNodesResponse.error(e.getMessage());
    } catch (Exception e) {
        LogUtils.error("Unexpected error", e);
        return CreateNodesResponse.error("Internal error: " + e.getMessage());
    }
}
```

❌ **避免做法**：

```java
// ❌ 多功能混合
@Tool("Do everything with nodes")
public String nodeOperation(String action, Map<String, Object> params) {
    switch (action) {
        case "create": ...
        case "delete": ...
        case "update": ...
    }
}

// ❌ 模糊描述
@Tool("Edit nodes")

// ❌ 抛出未处理异常
public void deleteNodes(List<String> ids) {
    // 可能抛出 NPE、IOE 等
}
```

### 11.2 对话管理

✅ **推荐做法**：

```java
// 1. 动态 Token 限制
.dynamicMaxTokens(profileId -> {
    return profile.getMaxTokens();
})

// 2. 保护重要上下文
.protectedRecentTurnCount(2)  // 最近 2 轮不驱逐

// 3. 压缩工具历史
.historicalToolTokenShare(0.5)  // 工具 Token 最多占 50%

// 4. 支持撤销/重做
if (chatMemory.canUndo()) {
    String undoneMessage = chatMemory.undo();
    // 显示给用户
}
```

### 11.3 错误处理

✅ **推荐做法**：

```java
// 1. 工具参数错误
private ToolArgumentsErrorHandler buildToolArgumentsErrorHandler() {
    return (error, context) -> {
        LogUtils.warn("Tool arguments error", error);
        return ToolErrorHandlerResult.text(
            "Invalid arguments: " + error.getMessage()
        );
    };
}

// 2. 自动重试
.maxRetries(2)

// 3. 取消支持
if (cancellationSupplier.get()) {
    throw new ToolExecutionException("Cancelled");
}

// 4. 用户友好提示
catch (RateLimitException e) {
    return "Rate limit exceeded. Please wait a moment and try again.";
}
```

### 11.4 性能优化

✅ **推荐做法**：

```java
// 1. 批量操作
readNodesWithDescendants(ReadNodesWithDescendantsRequest.builder()
    .nodeIds(List.of(n1, n2, n3))
    .build());

// 2. 缓存只读结果
private Map<String, Object> cache = new HashMap<>();

// 3. 延迟加载
private OpenAiTokenCountEstimator estimator() {
    if (estimator == null) {
        estimator = new OpenAiTokenCountEstimator(modelName);
    }
    return estimator;
}

// 4. 异步工具执行（谨慎使用）
@Tool("Long running operation")
public CompletableFuture<ToolResponse> asyncTool(ToolRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        // 长时间操作
        return result;
    });
}
```

### 11.5 测试建议

```java
public class AIChatServiceTest {
    
    @Test
    public void testToolExecution() {
        // 1. 模拟 ChatModel
        ChatModel mockModel = mock(ChatModel.class);
        when(mockModel.generate(any())).thenReturn(response);
        
        // 2. 创建服务
        AIChatService service = new AIChatService(
            mockModel,
            toolSet,
            chatMemory,
            tokenTracker,
            summaryHandler,
            () -> false,  // cancellationSupplier
            null          // tokenUsageConsumer
        );
        
        // 3. 测试对话
        String response = service.chat("Create a node");
        
        // 4. 验证工具调用
        verify(toolSet).createNodes(any());
    }
    
    @Test
    public void testTokenTracking() {
        AtomicReference<TokenUsage> capturedUsage = new AtomicReference<>();
        
        AIChatService service = new AIChatService(
            mockModel,
            toolSet,
            chatMemory,
            tokenTracker,
            summaryHandler,
            () -> false,
            capturedUsage::set  // 捕获 TokenUsage
        );
        
        service.chat("Hello");
        
        assertThat(capturedUsage.get()).isNotNull();
        assertThat(capturedUsage.get().inputTokenCount()).isGreaterThan(0);
    }
}
```

---

## 附录

### A. LangChain4j 核心 API 速查

#### ChatModel

```java
// 创建模型
ChatModel model = OpenAiChatModel.builder()
    .apiKey("sk-...")
    .modelName("gpt-4o")
    .build();

// 调用
Response<AiMessage> response = model.generate(messages);
String text = response.content().text();
TokenUsage usage = response.tokenUsage();
```

#### AiServices

```java
// 创建 AI 服务
MyAssistant assistant = AiServices.builder(MyAssistant.class)
    .chatModel(model)
    .tools(toolSet)
    .chatMemory(memory)
    .build();

// 定义接口
interface MyAssistant {
    @SystemMessage("You are a helpful assistant.")
    String chat(@UserMessage String message);
}
```

#### Tool

```java
// 定义工具
@Tool("Description of the tool")
public ToolResponse myTool(ToolRequest request) {
    // 实现
    return response;
}

// 工具规格自动生成
ToolSpecification spec = ToolSpecification.from(method);
```

### B. 依赖版本对照

| LangChain4j 版本 | Java 版本 | 说明 |
|-----------------|----------|------|
| 1.11.0 | 17+ | Freeplane 当前使用 |
| 1.0.0+ | 17+ | 稳定版 |
| 0.36.2 | 8+ | 旧版（不推荐） |

### C. 模型提供商对比

| 提供商 | 优势 | 成本 | 延迟 |
|--------|------|------|------|
| OpenRouter | 多模型，统一接口 | 中 | 中 |
| OpenAI | 最强能力 | 高 | 低 |
| Google Gemini | 长上下文，免费额度 | 低 | 低 |
| Ollama | 本地，隐私 | 免费 | 取决于硬件 |

### D. 参考资源

- **LangChain4j 官方文档**: https://docs.langchain4j.dev/
- **LangChain4j GitHub**: https://github.com/langchain4j/langchain4j
- **LangChain4j 示例**: https://github.com/langchain4j/langchain4j-examples
- **OpenAI API**: https://platform.openai.com/docs
- **Ollama 文档**: https://ollama.com/docs
- **Google Gemini API**: https://ai.google.dev/docs

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2024-01-01 | 初始版本，基于 LangChain4j 1.11.0 |

---

**文档维护**：本文档随项目更新而更新，如有疑问请查阅 [LangChain4j 官方文档](https://docs.langchain4j.dev/) 或提交 Issue。
