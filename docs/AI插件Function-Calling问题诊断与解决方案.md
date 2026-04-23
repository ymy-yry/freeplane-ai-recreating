# AI 插件 Function Calling 问题诊断与解决方案

> 创建时间：2026-04-22
> 最后更新：2026-04-22
> 状态：工具循环未触发，待最终解决

---

## 目录

- [问题概述](#问题概述)
- [架构背景](#架构背景)
- [修复过程记录](#修复过程记录)
- [当前未解决问题](#当前未解决问题)
- [解决方案对比](#解决方案对比)
- [关键代码位置](#关键代码位置)

---

## 问题概述

**Freeplane AI 插件的 build 功能无法正确执行工具调用循环。**

### 表现

调用 `POST /api/ai/build/summarize` 等接口时，AI 模型能够识别需要调用哪些工具，并返回了工具调用的 JSON，但 LangChain4j **没有实际执行工具**，而是将工具调用 JSON 作为普通文本直接返回。

### 影响范围

| 功能 | 端点 | 状态 |
|------|------|------|
| 生成思维导图 | `POST /api/ai/build/generate-mindmap` | 仅返回 JSON，不创建节点 |
| 展开节点 | `POST /api/ai/build/expand-node` | 仅返回 JSON，不插入子节点 |
| 分支摘要 | `POST /api/ai/build/summarize` | 仅返回 JSON，不写入备注 |
| 自动标签 | `POST /api/ai/build/tag` | 仅返回 JSON，不应用标签 |

---

## 架构背景

### 调用链路

```
前端请求
  → AiRestController.handleSummarize()
    → AIServiceLoader.selectService()
      → DefaultAgentService.processRequest()
        → DefaultAgentService.handleSummarize()
          → AIChatService.chat(prompt)          ← 问题所在层
            → LangChain4j AiServices.chat()
              → OpenAiChatModel (deepseek-v3 / ernie)
                → 模型返回 tool_calls JSON
                  → [期望] LangChain4j 执行工具 → 循环直到最终回复
                  → [实际] 直接返回 tool_calls JSON 文本
```

### 关键组件

| 组件 | 文件位置 | 职责 |
|------|---------|------|
| `DefaultAgentService` | `service/impl/DefaultAgentService.java` | build 功能入口，构建 prompt 并调用 AI |
| `AIChatService` | `chat/AIChatService.java` | 封装 LangChain4j，处理工具循环 |
| `AIChatServiceFactory` | `chat/AIChatServiceFactory.java` | 创建 AIChatService 实例 |
| `AIChatModelFactory` | `chat/AIChatModelFactory.java` | 根据 provider 创建模型实例 |
| `AIProviderConfiguration` | `chat/AIProviderConfiguration.java` | 读取 provider/model 配置 |
| `ToolExecutorFactory` | `tools/utilities/ToolExecutorFactory.java` | 扫描 @Tool 注解，注册工具 |

---

## 修复过程记录

### 修复一：分隔符错误导致模型选择失败

**时间**：2026-04-22 会话初期

**现象**：调用 build API 返回 500，日志显示 `Missing model selection`

**根因**：`defaults.properties` 中使用了错误的分隔符

```properties
# 错误
ai_selected_model = ernie:ernie-4.5

# 正确（AIModelSelection 类使用 | 作为 SELECTION_SEPARATOR）
ai_selected_model = ernie|ernie-4.5
```

**涉及代码**：`AIModelSelection.java` 中 `SELECTION_SEPARATOR = "|"`，`fromSelectionValue()` 用 `indexOf("|")` 解析，若返回 -1 则返回 null，触发"Missing model selection"异常。

---

### 修复二：模型名称不存在

**时间**：修复一之后

**现象**：API 返回 `{"error": {"code": "invalid_model", "message": "The model does not exist..."}}`

**根因**：`ernie-4.5` 不是有效的千帆模型名称

**修复**：

```properties
# 错误
ai_selected_model = ernie|ernie-4.5

# 正确（百度千帆实际模型名）
ai_selected_model = ernie|ernie-4.5-turbo-128k
ai_ernie_model_list=ernie-4.5-turbo-128k,ernie-4.5-turbo-32k,ernie-speed-pro-128k
```

---

### 修复三：API Key 被换行截断

**时间**：修复二之后

**现象**：`hasErnieKey()` 返回 false，虽然配置文件有写入 API Key

**根因**：使用 PowerShell `Add-Content` 写入 API Key 时，在特定字符位置自动插入了换行符

```
# 实际存储（错误，key 被截断为两行）
ai_ernie_key=bce-v3/ALTAK-AfROKZ9YqhoydcZnJK
KFH/8146984583225f5262f7e6b5e20f07221e016a5c
```

**修复**：改用 `[System.IO.File]::WriteAllText()` 写入，确保整个 key 在一行内（完整 88 字符）

---

### 修复四：验证 ERNIE 不支持 Function Calling

**时间**：修复三之后

**验证方法**：直接向千帆平台 API 发送带 `tools` 参数的请求

```powershell
$jsonBody = '{
  "model": "ernie-4.5-turbo-128k",
  "messages": [{"role": "user", "content": "请为节点生成摘要"}],
  "tools": [{"type": "function", "function": {"name": "readNodes", ...}}],
  "tool_choice": "auto"
}'
Invoke-RestMethod -Uri "https://qianfan.baidubce.com/v2/chat/completions" ...
```

**结论**：ernie-4.5-turbo-128k **不支持** function calling，`tools` 参数被完全忽略，模型直接返回纯文本

**百度千帆实际支持 function calling 的模型**（截至 2026-04-22）：

- ERNIE 系列：`ernie-5.0-thinking-preview`、`ernie-x1.1-preview`、`ernie-x1-turbo-*`、`ernie-speed-pro-128k`、`ernie-lite-pro-128k`
- DeepSeek 系列：`deepseek-v3.2`、`deepseek-v3.1-250821`、`deepseek-v3`、`deepseek-r1-250528`
- 其他：`kimi-k2.5`、`glm-5`、`minimax-m2.5`

---

### 修复五：切换到 deepseek-v3

**时间**：2026-04-22 下午

**原因**：ernie-4.5-turbo-128k 不支持 function calling，切换至官方支持列表中成本较低的 deepseek-v3

**验证 deepseek-v3 支持 function calling**：

```powershell
$jsonBody = '{
  "model": "deepseek-v3",
  "messages": [{"role": "user", "content": "查询北京天气"}],
  "tools": [{"type": "function", "function": {"name": "get_weather", ...}}],
  "tool_choice": "auto"
}'

# 返回（证明支持）：
# {
#   "id": "4012e7697...",
#   "type": "function",
#   "function": {
#     "name": "get_weather",
#     "arguments": "{\"location\":\"北京\"}"
#   }
# }
```

**已修改文件**：

1. `defaults.properties`：

```properties
ai_selected_model = ernie|deepseek-v3
ai_ernie_model_list=ernie-4.5-turbo-128k,ernie-4.5-turbo-32k,ernie-speed-pro-128k,deepseek-v3,deepseek-r1
```

2. `AIProviderConfiguration.java`（`inferDefaultModelSelection` 方法）：

```java
// ernie key 存在时的 fallback 默认模型
String modelName = (models != null && !models.trim().isEmpty())
    ? models.split(",")[0].trim()
    : "deepseek-v3";   // 原来是 "ernie-4.5-turbo-128k"
```

---

## 当前未解决问题

### 问题描述

deepseek-v3 能正确响应 function calling（直接 API 测试通过），但在 Freeplane 中通过 LangChain4j 调用时，工具循环未被触发。

**实际响应示例**（`POST /api/ai/build/summarize`）：

```json
{
  "summary": "I'll help generate a summary... First, we need to read the node's content.\n\n1. First, let's get the current map and node selection:\n\n```json\n[{\"name\":\"getSelectedMapAndNodeIdentifiers\",\"arguments\":{...}}]\n```"
}
```

模型把"我想调用工具"的 JSON 直接输出到了回复文本中，而不是被 LangChain4j 拦截执行。

### 状态矩阵

| 验证项 | 结果 | 验证方式 |
|--------|------|---------|
| deepseek-v3 直接 API | 支持 function calling | 千帆 API 直接测试 |
| Freeplane 模型加载 | 成功 | 日志 `AIChatService initialized successfully` |
| 工具注册 | 代码层面正确 | AiServices builder 中 `.tools(registry)` |
| 工具实际执行 | **未触发** | 日志中无 `Agent tool call:` 记录 |
| 最终回复 | 错误（返回 JSON 文本） | build API 响应内容 |

### 可能原因分析

#### 原因 A：千帆平台响应格式差异（可能性最高）

直接 API 测试时，千帆平台返回的 `tool_calls` 格式与 OpenAI 标准有细微差异。

OpenAI 标准格式（LangChain4j 期望）：
```json
{
  "choices": [{
    "message": {
      "role": "assistant",
      "content": null,
      "tool_calls": [{"id": "call_xxx", "type": "function", "function": {...}}]
    },
    "finish_reason": "tool_calls"
  }]
}
```

千帆实际格式需要抓包确认，可能缺少 `content: null` 或 `finish_reason: "tool_calls"`。

#### 原因 B：工具注册方式不兼容

当前代码：

```java
// AIChatService.java 第 48 行
.tools(toolExecutorRegistry.getExecutorsBySpecification())
// 类型：Map<ToolSpecification, ToolExecutor>
```

LangChain4j 文档建议也可以用：

```java
.tools(toolSetObject)  // 直接传入含 @Tool 注解方法的对象
```

两种方式在某些版本的行为可能不一致。

#### 原因 C：LangChain4j 版本兼容性问题

当前版本：`langchain4j:1.11.0`

相关已知 Issue：
- [#1912](https://github.com/langchain4j/langchain4j/issues/1912)：AiServices 不调用工具（Ask me for the tool）
- [#4601](https://github.com/langchain4j/langchain4j/issues/4601)：工具调用失败无错误报告
- [#4747](https://github.com/langchain4j/langchain4j/issues/4747)：并发工具执行时 hooks 未触发

---

## 解决方案对比

### 方案 A：使用 OpenRouter 平台

**原理**：OpenRouter 作为 API 网关，将所有模型的响应统一标准化为 OpenAI 格式，确保 LangChain4j 能正确解析 `tool_calls`。

**实施步骤**：

1. 注册 OpenRouter 账号（https://openrouter.ai），获取 API Key
2. 修改 `defaults.properties`：

```properties
ai_openrouter_key = sk-or-v1-...
ai_selected_model = openrouter|deepseek/deepseek-v3
```

代码已有完整 OpenRouter 支持，无需任何代码改动。

**适用场景**：希望快速验证工具循环是否因格式差异导致失败

**局限性**：
- 需要额外注册账号
- 请求多一跳代理，延迟略有增加
- 免费额度有限（$1/月）

---

### 方案 B：手动实现工具调用循环（绕过 LangChain4j）

**原理**：不依赖 LangChain4j 的自动工具循环，在 `AIChatService` 中新增 `chatWithToolLoop()` 方法，手动处理"发请求 → 检测 tool_calls → 执行工具 → 将结果加入 messages → 再次请求"的循环。

**前提条件**：需要访问底层 `ChatModel`（当前 `AIChatService` 通过 `AiServices` 代理封装，底层模型不可直接调用）

**改造点**：

1. `AIChatService` 中暴露底层 `ChatModel` 实例
2. 新增 `chatWithToolLoop(String message)` 方法，自行实现循环
3. `DefaultAgentService` 中将 `agentService.chat(prompt)` 替换为 `agentService.chatWithToolLoop(prompt)`

**适用场景**：不想依赖外部平台，希望直接在千帆平台运行

**局限性**：
- 代码改动较大（约 60-80 行新增）
- 需要自行处理错误重试、循环上限、工具结果序列化等边界情况
- 绕过了 LangChain4j 已有的稳定抽象层

---

### 方案 C：抓包定位格式差异后修复

**原理**：通过 HTTP 拦截器记录 LangChain4j 实际发出/接收的完整 JSON，确认是否是响应格式导致工具循环未触发，再针对性修复。

**实施步骤**：

1. 在 `OpenAiChatModel.builder()` 中加入请求日志拦截器
2. 发起一次 build 请求
3. 对比千帆返回的 `finish_reason` 是否为 `"tool_calls"`
4. 若格式有差异，通过 `chatRequestTransformer` 做适配

**适用场景**：需要精确定位问题根因，而不是绕开问题

**局限性**：排查耗时，LangChain4j 的拦截器 API 在 1.11.0 中需要确认是否支持

---

### 方案对比总览

| 维度 | 方案 A（OpenRouter） | 方案 B（手动循环） | 方案 C（抓包定位） |
|------|---------------------|-----------------|-----------------|
| 代码改动量 | 零（仅配置） | 大（60-80 行） | 中（10-20 行调试代码） |
| 解决速度 | 快（5 分钟） | 中（1-2 小时） | 慢（取决于排查结果） |
| 可维护性 | 高（复用现有架构） | 低（自维护循环逻辑） | 高（精准修复根因） |
| 依赖外部账号 | 是 | 否 | 否 |
| 根治问题 | 否（绕开问题） | 否（绕开问题） | 是（找到根因） |
| 推荐程度 | 用于快速验证 | 最后备选 | 推荐深入调查时使用 |

---

## 关键代码位置

| 文件 | 关键逻辑 |
|------|---------|
| `service/impl/DefaultAgentService.java` | build 功能入口，`handleSummarize()` 等方法，`ensureAgentInitialized()` 工具集初始化 |
| `chat/AIChatService.java` | LangChain4j `AiServices` 构建，工具注册，`chat()` 方法 |
| `chat/AIChatServiceFactory.java` | 创建 `AIChatService`，调用 `AIChatModelFactory` |
| `chat/AIChatModelFactory.java` | 按 provider 名称创建 `ChatModel`（ernie 分支使用 `OpenAiChatModel`） |
| `chat/AIProviderConfiguration.java` | 读取配置，`inferDefaultModelSelection()` 自动推断默认模型 |
| `tools/utilities/ToolExecutorFactory.java` | 扫描 `@Tool` 注解，创建 `ToolExecutorRegistry` |
| `src/main/resources/org/freeplane/plugin/ai/defaults.properties` | 所有 AI 相关配置的默认值，包括模型选择和 API Key |
