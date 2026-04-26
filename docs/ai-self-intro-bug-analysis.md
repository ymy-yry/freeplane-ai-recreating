# AI 插件问题排查与修复总结

> 本文档记录从项目调试过程中发现并修复的所有关键问题，包含根因分析和修复方法。

## 现象描述

用户在 Freeplane 中触发"生成思维导图"功能，结果预览区显示的不是 JSON 结构，而是类似如下的自我介绍文字：

> 好的，请告诉我您需要创建或修改的思维导图主题，以及具体的结构要求或内容要点，我会根据您的需求进行操作

或：

> 好的，我随时准备好帮助您创建或优化思维导图。请告诉我您的具体需求或主题，我将为您提供专业的支持。

---

## 历次根因排查

### 根因 1：`prompts.properties` 多行格式问题（已修复）

**位置**：`freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/buffer/prompts.properties`

**问题**：Java `Properties.load()` 只读取键值对的第一行，多行内容（`## 目标`、`## 任务` 等）被完全忽略。

```
# 原始错误写法（只有第一行被读取）
mindmap.generation.zh=你是一位专业的思维导图专家，擅长为各种主题创建结构清晰、内容丰富的思维导图。

## 上下文 (Context)   ← 被忽略
...
```

**影响**：AI 收到的 prompt 只有一句角色描述，没有 topic 占位符和 JSON 格式要求，AI 用中文自我介绍作为回复。

**修复**：所有 6 个模板改为单行格式，用 `\n` 表示换行，包含完整占位符和 JSON 格式要求。

---

### 根因 2：`MindMapBufferLayer.callAI()` 使用 `AIChatService`（已修复）

**位置**：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/mindmap/MindMapBufferLayer.java`

**问题**：`callAI()` 使用 `AIChatService.chat(prompt)`，该服务通过 `AiServices.builder()` 构建，system message 由 `MessageBuilder.buildForChat()` 提供，包含 `ROLE_DEFINITION`（描述性语气）和 `PROFILE_CONTROL_GUIDANCE`（含 "confirm with 'ok'"）。

某些模型（Qwen/DeepSeek）收到描述性 system message 后，会复述为自我介绍；收到 "confirm with ok" 语义后，先回一个 `ok` 确认。

**调用链**：
```
用户操作 → BufferLayerRouter → MindMapBufferLayer.process()
  → callAI(prompt, model, request)
  → AIChatService.chat(prompt)  ← 包含复杂 system message
  → AI 回复自我介绍或 "ok"
```

**修复**：改用底层 `ChatModel.chat(ChatRequest)` 直接调用，只传简洁的 system prompt：
```java
ChatRequest chatRequest = ChatRequest.builder()
    .messages(Arrays.asList(
        SystemMessage.from("You are a mind map expert. Return only valid JSON, no markdown, no explanation."),
        UserMessage.from(prompt)
    ))
    .build();
ChatResponse chatResponse = chatModel.chat(chatRequest);
```

---

### 根因 3：`DefaultAgentService` 中三个功能方法使用 `AIChatService`（已修复）

**位置**：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/service/impl/DefaultAgentService.java`

**问题**：`handleGenerateMindMap`、`handleExpandNode`、`handleSummarize`、`handleTag` 均调用 `agentService.chat(prompt)`（即 `AIChatService`），受同样的 system message 影响。

**修复**：新增 `chatWithModel(systemPrompt, userPrompt)` 方法，四个功能方法全部改为调用该方法，绕开 `AIChatService`。

---

### 根因 4：`MessageBuilder.ROLE_DEFINITION` 描述性语气（已修复）

**位置**：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/tools/MessageBuilder.java`

**原始内容**：
```java
"Your primary role is to help users create and manage mind maps..."
```

**问题**：描述性语气被 Qwen/DeepSeek 等模型解读为"我应该介绍自己的能力"，回复了自我介绍文字。

**修复**：改为指令式语气：
```java
"You are a Freeplane AI assistant. "
    + "Your task is to execute the user's mind map operations precisely and return results as requested. "
    + "Do NOT introduce yourself or describe your capabilities unless explicitly asked."
```

---

## 两条独立调用路径

项目中存在两条完全独立的 AI 调用路径，需要分别修复：

| | REST API 路径 | Buffer 层路径 |
|---|---|---|
| **入口** | `POST /api/ai/build/generate-mindmap` | Freeplane 原生 Swing 菜单/按钮 |
| **处理类** | `DefaultAgentService` | `MindMapBufferLayer` |
| **触发方式** | 前端 Vue 组件或外部 HTTP 请求 | Freeplane 内置 UI 操作 |
| **AI 调用** | `chatWithModel()` → `ChatModel` ✅ | `ChatModel.chat()` ✅ |

---

## 已修复文件清单

| 文件 | 修复内容 |
|---|---|
| `prompts.properties` | 6 个模板从多行格式改为单行格式，含占位符和 JSON 要求 |
| `MindMapBufferLayer.java` | `callAI()` 从 `AIChatService` 改为底层 `ChatModel` |
| `DefaultAgentService.java` | 新增 `chatWithModel()`，四个功能方法全部改用底层 `ChatModel` |
| `MessageBuilder.java` | `ROLE_DEFINITION` 改为指令式，加入 "Do NOT introduce yourself" |

---

## 当前仍未解决的问题

**现象**：重启 Freeplane 后，AI 仍然返回自我介绍文字。

**可能原因**：

1. **Freeplane 未完全重启**：OSGi 插件架构下，类在启动时加载，运行期替换 jar 不会热更新，必须完全关闭（任务管理器确认 java 进程退出）后重启。

2. **用户触发的是 Freeplane 自带的 AI chat 功能**：`AIChatPanel.sendMessage()` → `chatRequestFlow.submitRequest(chatService)` 走的是 Freeplane 原生 chat 流程，该流程的 system message 来自 `MessageBuilder.buildForChat()`。如果用户配置了 Assistant Profile，profile 的定义会覆盖 `ROLE_DEFINITION`。

3. **Assistant Profile 中包含自我介绍内容**：如果用户在 Freeplane 的"AI 助手配置"中设置了包含自我介绍的 profile 定义，AI 会按照 profile 指示进行自我介绍。

---

## 排查建议

1. **确认 jar 已更新**：检查 `BIN/plugins/org.freeplane.plugin.ai/freeplane_plugin_ai-1.13.3.jar` 的修改时间是否是最新的。

2. **确认完全重启**：在任务管理器中确认所有 `javaw.exe` / `java.exe` 进程已退出，再启动 Freeplane。

3. **检查 Assistant Profile**：在 Freeplane AI 面板中，查看是否有激活的 Assistant Profile，其 profile 定义是否包含自我介绍语句。

4. **区分触发路径**：
   - 如果通过菜单"AI → 生成思维导图"触发 → 走 buffer 层路径
   - 如果在 AI chat 面板输入文字发送 → 走 `AIChatService` 路径（受 `MessageBuilder.buildForChat()` 影响）
   - 如果通过 Vue 前端触发 → 走 REST API 路径（`DefaultAgentService`）

---

## 问题 5：prompts.properties 中文编码与多行格式

### 5.1 历史背景与误区

`Properties.load(InputStream)` 诞生于 Java 1.1（1997年），强制使用 ISO-8859-1 编码，中文必须转义为 `\uXXXX`。

JDK 1.6（2006年）新增了 `Properties.load(Reader)` 方法，传入 `InputStreamReader(UTF-8)` 就可以**直接写中文**，无需转义。

### 5.2 我们项目的实际加载方式

`MindMapPromptOptimizer.loadTemplates()` 第 29 行已使用 UTF-8：

```java
promptTemplates.load(new java.io.InputStreamReader(is, StandardCharsets.UTF_8));
```

**结论：`prompts.properties` 完全可以直接写中文，不需要 `\uXXXX` 转义。**

### 5.3 真正的限制：Properties 格式的换行截断（与编码无关）

无论用哪种编码加载，`.properties` 格式规定：每个键值对以换行符结束，除非行尾有 `\`（续行符）。

```
# 错误写法：第二行开始全部被截断
mindmap.generation.zh=你是一位专业的思维导图专家。

## 上下文（此行被忽略）
主题：{topic}（此行被忽略）
```

### 5.4 两种正确写法

**写法一：单行格式（当前已采用）**

用 `\n` 表示换行，`Properties.load` 会自动解释为换行符：

```properties
mindmap.generation.zh=你是专业思维导图专家。\n请为主题"{topic}"生成 {maxDepth} 层结构。\n严格返回 JSON。
```

**写法二：多行 + `\` 续行符（可读性更好）**

中文直接写，每行末尾加 `\`：

```properties
mindmap.generation.zh=\
  你是专业思维导图专家。\n\
  请为主题"{topic}"生成 {maxDepth} 层完整结构。\n\
  要求：每个节点内容具体有价值，分支逻辑关联，使用中文。\n\
  严格返回 JSON，不含 Markdown 标记。\n\
  示例：{"text":"{topic}","children":[{"text":"分支1"},{"text":"分支2"}]}
```

关键规则：
- 每行末尾 `\` 是续行符，告诉解析器下一行仍属于同一个键
- `\n` 会被解释为换行，AI 收到的是带换行的多行文本
- 最后一行末尾**不加** `\`
- 中文直接写，因为加载方式已是 UTF-8

---

## 问题 6：lib/plugin-1.13.3.jar 未同步更新导致修复不生效

### 根因

`freeplane_plugin_ai-1.13.3.jar` 的 `MANIFEST.MF` 声明了 OSGi Bundle-ClassPath：

```
Bundle-ClassPath: ., lib/plugin-1.13.3.jar, lib/langchain4j-1.11.0.jar, ...
```

OSGi 加载 class 时，实际从 `lib/plugin-1.13.3.jar` 中读取业务代码。每次只更新 `freeplane_plugin_ai-1.13.3.jar` 而不更新 `lib/plugin-1.13.3.jar`，导致旧 class 一直被使用，修复代码永远不生效。

### 修复方法

每次编译后，同时更新两个位置：

```powershell
$jar = Get-ChildItem "freeplane_plugin_ai\build\libs\*.jar" | Select-Object -First 1
Copy-Item $jar.FullName "BIN\plugins\org.freeplane.plugin.ai\"
Copy-Item $jar.FullName "BIN\plugins\org.freeplane.plugin.ai\lib\plugin-1.13.3.jar" -Force
```

---

## 问题 7：前端 smartRequest 超时 30 秒导致请求失败

### 根因

`freeplane_web/src/api/aiApi.ts` 中 `smartRequest` 使用 `postWithFallback`（基础 axios 实例，超时 30 秒），而 AI 生成思维导图实际需要 40~60 秒，导致前端报超时错误，但后端已成功处理完成。

### 修复方法

将 `smartRequest` 改用 `postWithLongTimeout`（120 秒超时实例）：

```typescript
// 修复前
export function smartRequest(data) {
  return postWithFallback('/ai/chat/smart', '/ai/smart', data)
}

// 修复后
export function smartRequest(data) {
  return postWithLongTimeout('/ai/chat/smart', '/ai/smart', data)
}
```

**文件**：`freeplane_web/src/api/aiApi.ts`

