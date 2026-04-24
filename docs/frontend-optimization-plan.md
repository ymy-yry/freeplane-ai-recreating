# Freeplane Web 前端优化方案

**对应后端版本**: Week 3（SPI架构 + 国内模型 + Auto路由 + 缓冲层缓存）  
**目标**: 将已有但孤立的前端 AI 代码接通后端，并围绕 Auto 模式、Chat/Build 双面板构建完整交互体系

---

## 一、现状差距分析

### 1.1 后端 Week 3 实际交付能力

| 能力 | 端点 | 状态 |
|---|---|---|
| 模型列表（含DashScope/ERNIE） | `GET /api/ai/chat/models` | ✅ 真实返回 |
| AI 对话（CHAT服务） | `POST /api/ai/chat/message` | ✅ 真实调用LangChain4j |
| 生成思维导图 | `POST /api/ai/build/generate-mindmap` | ✅ 返回AI文字结果 |
| 节点展开 | `POST /api/ai/build/expand-node` | ✅ 返回AI文字结果 |
| 分支摘要 | `POST /api/ai/build/summarize` | ✅ 返回AI文字结果 |
| 自动标签 | `POST /api/ai/build/tag` | ✅ 返回AI文字结果 |
| 智能缓冲层（Auto模式） | `POST /api/ai/chat/smart` | ✅ 走BufferLayerRouter |
| Auto 服务路由 | `AIServiceLoader.selectService()` | ✅ 根据action自动选CHAT/AGENT |
| 用户偏好配置 | `UserPreferenceConfig` | ✅ 后端已支持，前端尚无入口 |
| 性能监控数据 | `PerformanceMonitor` | ✅ 后端收集，前端尚未展示 |

### 1.2 前端当前问题（逐文件）

| 文件 | 问题 |
|---|---|
| `MindMapCanvas.vue` | 未调用 `aiStore.init()`，模型列表永远为空 |
| `MindMapCanvas.vue` | 未挂载任何 AI 面板组件 |
| `MindMapCanvas.vue` | 未注册右键菜单事件，`NodeContextMenu` 从未被触发 |
| `Toolbar.vue` | 无 AI 入口按钮，无 Chat/Build 模式切换 |
| `Toolbar.vue` | 搜索只打 log，未调用 `/nodes/search` |
| `NodeContextMenu.vue` | "AI展开节点"只打 log，未调用接口 |
| `components/ai/ModelSelector.vue` | 已完整实现，但**从未被任何页面引用** |
| `aiStore.ts` | 缺少 `aiMode`、`buildLoading`、Build 操作方法 |
| `types/ai.ts` | `ExpandNodeResult`/`SummarizeResult`/`TagResult` 与后端实际响应不符 |
| `aiApi.ts` | `expandNode`/`summarizeBranch`/`autoTag` 泛型类型与后端实际不符 |
| 整体 | 无 Auto 模式入口，无 Chat 对话面板，无 Build 操作面板 |

---

## 二、改动文件总览

| 改动类型 | 文件路径 | 主要内容 |
|---|---|---|
| 修改 | `src/types/ai.ts` | 修正 Build 操作返回类型；新增 AiMode/ServiceType 类型 |
| 修改 | `src/api/aiApi.ts` | 修正 expandNode/summarize/tag 泛型；新增 serviceType 参数说明 |
| 修改 | `src/stores/aiStore.ts` | 新增 aiMode/buildLoading/buildResult；新增 4 个 Build 操作方法 |
| 修改 | `src/components/Toolbar.vue` | 加 AI 按钮、Chat/Build tab、搜索接通接口 |
| 修改 | `src/components/NodeContextMenu.vue` | AI展开/摘要菜单项接通实际调用 |
| 修改 | `src/components/MindMapCanvas.vue` | 调用 init()、挂载面板、注册右键菜单 |
| 新建 | `src/components/ai/AiChatPanel.vue` | Chat 对话侧边栏 |
| 新建 | `src/components/ai/AiBuildPanel.vue` | Build 操作面板（含 Auto 模式入口） |
| 修改 | `src/components/ai/ModelSelector.vue` | 新增 Auto 模式选项 |
| 新建 | `src/components/ai/ModelConfigPanel.vue` | 自定义模型/API Key 配置对话框 |
| 修改 | `src/api/aiApi.ts` | 新增 saveModelConfig 接口 |
| 修改 | `src/stores/aiStore.ts` | 新增 saveCustomModel 方法 |

---

## 三、改动详述

### 3.1 `src/types/ai.ts` — 修正与新增类型

**问题**: `ExpandNodeResult` 含 `createdNodes[]`，但后端实际返回 `result`（AI文字）字段。

**修正后的 Build 操作响应结构**（对应后端 `DefaultAgentService` 各 handler 实际返回）：

```ts
// 修正：节点展开实际响应
export interface ExpandNodeActualResult {
  nodeId: string
  result: string         // AI 生成的展开内容（JSON文字）
  tokenUsage: TokenUsage
}

// 修正：分支摘要实际响应
export interface SummarizeActualResult {
  nodeId: string
  summary: string        // AI 生成的摘要文字
  tokenUsage: TokenUsage
}

// 修正：自动标签实际响应
export interface TagActualResult {
  nodeIds: string[]
  result: string         // AI 生成的标签JSON文字
  tokenUsage: TokenUsage
}

// 新增：AI 模式枚举
export type AiMode = 'chat' | 'build'

// 新增：服务类型（对应后端 AIServiceType）
export type ServiceType = 'auto' | 'chat' | 'agent'
```

---

### 3.2 `src/api/aiApi.ts` — 修正泛型 + 新增 serviceType 说明

**问题**: `expandNode` 的泛型用的是 `ExpandNodeResult`，解构时 `createdNodes` 为 undefined。

**修正**：将 `expandNode`/`summarizeBranch`/`autoTag` 泛型换为修正后的类型。

**新增**: 所有 Build 操作的 `serviceType` 默认值说明：
- 未传 `serviceType` → 后端 Auto 模式自动路由
- 传 `'agent'` → 强制走 `DefaultAgentService`
- 传 `'chat'` → 强制走 `DefaultChatService`

```ts
// aiChat 新增 serviceType 参数，支持前端控制路由
export function aiChat(data: {
  message: string
  modelSelection?: string
  mapId?: string
  selectedNodeId?: string
  serviceType?: 'auto' | 'chat' | 'agent'  // 不传则后端 Auto 路由
})
```

---

### 3.3 `src/stores/aiStore.ts` — 核心状态扩展

**新增状态**：

```ts
/** 当前 AI 面板模式：chat 对话 / build 操作 */
const aiMode = ref<AiMode>('chat')

/** Build 操作加载状态 */
const buildLoading = ref(false)

/** Build 操作最新结果文字（AI返回的文字，供面板展示） */
const buildResult = ref<string>('')

/** 服务类型：auto / chat / agent（控制后端路由） */
const serviceType = ref<ServiceType>('auto')
```

**新增方法**（4个 Build 操作，完成后触发 mapStore.loadMap 刷新导图）：

```ts
/** AI 展开节点 */
const expandNode = async (nodeId: string, options?: { count?: number; depth?: number; focus?: string })

/** AI 分支摘要 */
const summarize = async (nodeId: string, options?: { maxWords?: number; writeToNote?: boolean })

/** AI 一键生成思维导图 */
const generateMindMap = async (topic: string, options?: { maxDepth?: number })

/** AI 自动打标签 */
const autoTag = async (nodeIds: string[])
```

**修改 sendChat**：透传 `serviceType` 给后端，支持前端手动切换 Auto/Chat 模式：

```ts
const sendChat = async (message: string, nodeId?: string) => {
  // ... 现有逻辑
  const response = await aiApi.aiChat({
    message,
    modelSelection: currentModel.value,
    selectedNodeId: nodeId,
    serviceType: serviceType.value  // 新增：透传服务类型
  })
}
```

---

### 3.4 `src/components/Toolbar.vue` — AI 入口与三模式切换

**核心设计原则**：Auto / Chat / Build 三者是**并列互斥**的模式，不是层级关系。

后端 `AIServiceType` 只有 `CHAT` 和 `AGENT` 两种服务类型，`"auto"` 是路由控制字——不传或传 `"auto"` 时后端根据 `action` 关键字自动推断走哪个服务。因此前端应将 Auto 视为第三种独立模式与 Chat、Build 并列。

**新增元素**（在工具栏现有按钮右侧追加）：

```
[ 分隔线 ] [ 🤖 AI ] [ Auto | Chat | Build ]
```

- `🤖 AI` 按钮：切换 `aiStore.panelVisible`
- `Auto | Chat | Build` 三选一 tab：绑定 `aiStore.aiMode`，仅面板展开时显示
  - 选 **Auto** → `aiStore.aiMode='auto'`，`serviceType` 不传（后端自动路由）
  - 选 **Chat** → `aiStore.aiMode='chat'`，`serviceType='chat'`
  - 选 **Build** → `aiStore.aiMode='build'`，`serviceType='agent'`

**修复搜索**：`handleSearch` 改为调用 `aiApi.searchNodes`，搜索结果高亮对应节点（通过 VueFlow 的 `setCenter` 定位）。

**引入 `ModelSelector`**：紧挨 AI 按钮后显示，当面板可见时展开。

---

### 3.5 `src/components/ai/ModelSelector.vue` — 职责说明

`ModelSelector` 只负责选择具体**模型**（决定调用哪家 API），与 Auto/Chat/Build 模式切换是两个独立维度：

- **模式** = Auto / Chat / Build（由 Toolbar tab 控制，决定路由策略）
- **模型** = 具体使用哪个模型（由 ModelSelector 控制）

两者可正交组合，例如“Auto 模式 + DashScope qwen-max 模型”。

下拉列表无需新增 Auto 选项，保持原有结构：
```
[ OpenRouter: gpt-4o ] [ Gemini: gemini-2.0-flash ] [ DashScope: qwen-max ] [ ERNIE: ernie-4.5 ]
```

若无任何模型配置，显示警告提示。

---

### 3.6 `src/components/ai/AiAutoPanel.vue`（新建）— Auto 模式面板

**布局**：右侧固定 320px 侧边栏，`v-if="aiStore.panelVisible && aiStore.aiMode === 'auto'"`

```
┌─────────────────────────────────┐
│  ✨ Auto 智能模式               │
│  [当前节点: node-xxx]           │
├─────────────────────────────────┤
│  描述你的需求，AI 自动选择最佳   │
│  处理方式（对话 or 操作导图）    │
├─────────────────────────────────┤
│  ┌─────────────────────────────┐│
│  │ 输入自然语言指令...          ││
│  │                             ││
│  └─────────────────── [发送] ──┘│
├─────────────────────────────────┤
│  示例指令：                      │
│  · 展开这个节点，生成5个子节点   │
│  · 帮我总结这个分支的内容       │
│  · 这个节点讲的是什么           │
└─────────────────────────────────┘
```

**发送逻辑**：调用 `POST /api/ai/chat/smart`（BufferLayerRouter），不传 `serviceType`，后端 Auto 路由自动判断。

---

### 3.7 `src/components/ai/AiChatPanel.vue`（新建）— Chat 对话面板

**布局**：右侧固定 320px 侧边栏，`v-if="aiStore.panelVisible && aiStore.aiMode === 'chat'"`

```
┌─────────────────────────────────┐
│  🤖 AI Chat  [模型选择器]  [清空] │  ← 顶部栏
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐   │
│  │ 用户: 帮我分析这个节点   │   │
│  └─────────────────────────┘   │
│  ┌─────────────────────────┐   │
│  │ AI:  这个节点主要涉及...  │   │
│  └─────────────────────────┘   │
│          ... 历史消息 ...        │  ← 消息列表（滚动区）
│                                 │
├─────────────────────────────────┤
│  当前节点: [node-xxx] ℹ️         │  ← 关联节点提示
├─────────────────────────────────┤
│  ┌─────────────────────────┐   │
│  │ 输入消息...  Ctrl+Enter  │   │
│  └──────────────────────[发送]──┘  ← 输入区
└─────────────────────────────────┘
```

**关键交互**：
- 消息列表绑定 `aiStore.chatHistory`，assistant 消息 loading 时显示打字动画
- 发送时自动携带当前选中节点 ID（由 MindMapCanvas 通过 provide 注入）
- `streaming` 为 true 时发送按钮禁用

---

### 3.8 `src/components/ai/AiBuildPanel.vue`（新建）— Build 操作面板

**布局**：同侧边栏，`v-if="aiStore.panelVisible && aiStore.aiMode === 'build'"`

顶部**不再放 Auto 下拉**（模式切换已在 Toolbar tab 层完成，Build 面板始终为 agent 模式），**但保留模型选择器**（与 Chat 面板一致）。

```
┌─────────────────────────────────┐
│  🔧 AI Build  [模型选择器]      │  ← 顶部（模型选择与Chat面板一致）
├─────────────────────────────────┤
│  ┌─────────── 生成思维导图 ─────┐ │
│  │ 主题: [__________________] │ │
│  │ 深度: [3▼]                 │ │
│  │              [一键生成]     │ │
│  └─────────────────────────────┘ │
│  ┌─────────── 展开节点 ─────────┐ │
│  │ 当前节点: node-xxx ✓         │ │
│  │ 数量: [5▼]  方向: [_______] │ │
│  │              [AI 展开]      │ │
│  └─────────────────────────────┘ │
│  ┌─────────── 分支摘要 ─────────┐ │
│  │ 当前节点: node-xxx ✓         │ │
│  │ 写入备注: [✓]               │ │
│  │              [生成摘要]     │ │
│  └─────────────────────────────┘ │
│  ┌─────────── 自动标签 ─────────┐ │
│  │ 当前节点: node-xxx ✓         │ │
│  │              [批量打标签]   │ │
│  └─────────────────────────────┘ │
├─────────────────────────────────┤
│  结果预览：                       │
│  ┌─────────────────────────────┐ │
│  │ AI 返回的文字结果...         │ │  ← buildResult 展示区
│  └─────────────────────────────┘ │
└─────────────────────────────────┘
```

**关键交互**：
- 顶部模型选择器与 Chat 面板一致，绑定 `aiStore.currentModel`，切换模型后后续所有 Build 操作使用新模型
- 展开/摘要/标签操作需要选中节点，无选中时按钮 disabled 并提示"请先选中节点"
- 操作中显示 `buildLoading` 动画
- 操作完成后自动调用 `mapStore.loadMap()` 刷新导图
- 结果预览区显示 `aiStore.buildResult`（AI返回的文字内容）
- 所有 Build 操作请求均携带 `modelSelection: aiStore.currentModel` 参数

---

### 3.9 `src/components/NodeContextMenu.vue` — 接通实际调用

**现状**：`handleAIExpand` 只打 log

**改动**（新增 AI 展开 + AI 摘要两个菜单项的真实调用）：

```
右键菜单新增/修改：
  🤖 AI 展开节点  →  aiStore.expandNode(nodeId) + mapStore.loadMap()
  📝 AI 分支摘要  →  aiStore.summarize(nodeId)   （新增菜单项）
  操作中显示 loading toast，完成后提示成功
```

需同时新增 `ai-summarize` emit 事件定义。

---

### 3.10 `src/components/MindMapCanvas.vue` — 串联所有组件

**改动清单**：

1. `onMounted` 新增 `await aiStore.init()` → **修复模型列表为空的根本问题**

2. 追踪选中节点 ID：
```ts
const selectedNodeId = ref<string>('')
// watch VueFlow 选中状态变化
watch(() => vueFlow.nodes.value.filter(n => n.selected), (selected) => {
  selectedNodeId.value = selected.length > 0 ? selected[0].id : ''
})
// provide 给子组件
provide('selectedNodeId', selectedNodeId)
```

3. 注册右键菜单（现有 `NodeContextMenu` 从未被挂载）：
```ts
// 监听 VueFlow 的 node-context-menu 事件
// 显示 NodeContextMenu 组件，传入 x/y/nodeId
```

4. 模板引入四个 AI 组件（Auto/Chat/Build 三面板 + 右键菜单）：
```html
<AiAutoPanel />
<AiChatPanel />
<AiBuildPanel />
<NodeContextMenu v-bind="contextMenu" @ai-expand="handleAIExpand" @ai-summarize="handleAISummarize" />
```

---

### 3.11 `src/components/ai/ModelConfigPanel.vue`（新建）— 自定义模型配置对话框

**入口**：ModelSelector 下拉底部 `⚙️ 添加自定义模型...` 点击触发，或 AiAutoPanel/AiChatPanel 顶部模型选择区旁的齿轮按钮。

**布局（Modal 弹窗）**：

```
┌─────────────────────────────────────────┐
| ⚙️ 配置 AI 模型                    [✕]  |
├─────────────────────────────────────────┤
| 预设 Provider（Tab 切换）                |
| [ OpenRouter ] [ Gemini ] [ DashScope ] |
| [ ERNIE ] [ Ollama ] [ 自定义 ]         |
├─────────────────────────────────────────┤
|                                         |
|  Provider: [OpenRouter            ▼]    |
|  API Key:  [••••••••••••••]  [👁️]      |
|            获取 API Key →                |
|  Base URL: [https://openrouter.ai/api/v1]│ (自定义时显示)
|  模型名:   [openai/gpt-4o         ]     | (自定义时必填)
|                                         |
├─────────────────────────────────────────┤
| ❌ 错误信息（若有）                      |
|              [取消]  [保存并刷新]        |
└─────────────────────────────────────────┘
```

**预设 Provider 配置规则**：

| Provider | API Key 配置项 | Base URL（默认值） | 默认模型 | Key 获取链接 |
|----------|--------------|-------------------|---------|-------------|
| OpenRouter | `ai_openrouter_key` | `https://openrouter.ai/api/v1` | `openai/gpt-4o` | https://openrouter.ai/keys |
| Google Gemini | `ai_gemini_key` | `https://generativelanguage.googleapis.com` | `gemini-2.0-flash` | https://aistudio.google.com/app/apikey |
| DashScope (Qwen) | `ai_dashscope_key` | `https://dashscope.aliyuncs.com` | `qwen-max` | https://dashscope.console.aliyun.com/apiKey |
| ERNIE (Baidu) | `ai_ernie_key` | `https://aip.baidubce.com` | `ernie-4.5` | https://console.bce.baidu.com/iam/ |
| Ollama | `ai_ollama_api_key`（可选） | `http://localhost:11434` | `llama3` | 本地部署无需 Key |
| 自定义 | `ai_{providerName}_key` | 用户填写 | 用户填写 | — |

**保存逻辑**：
1. 用户填写后点击"保存并刷新"
2. 调用 `POST /api/ai/config/save`（**需后端新增此接口**）
3. 后端通过 `ResourceController.setProperty()` 写入配置
4. 返回 `{ success: true }` 后，前端调用 `aiStore.fetchModelList()` 刷新模型列表
5. ModelSelector 下拉自动出现新配置的模型

**字段校验**：
- API Key 不可为空（Ollama 除外）
- 自定义 Provider 时，Provider Name 不可包含空格或特殊字符（正则 `^[a-zA-Z][a-zA-Z0-9_]*$`）
- 自定义 Provider 时，模型名必填
- 保存前可点击"测试连接"（可选）发送轻量请求验证 Key 是否有效

---

## 四、Auto 模式详解

### 4.1 Auto 与 Chat/Build 的关系

**Auto 是与 Chat、Build 并列的第三种模式，三者互斥**，不是 Chat 或 Build 的子选项。

```
前端三模式
  ├── Auto   → serviceType 不传（后端自动路由）→ 走 /api/ai/chat/smart
  ├── Chat   → serviceType='chat'             → 走 /api/ai/chat/message
  └── Build  → serviceType='agent'            → 走 /api/ai/build/*
```

### 4.2 后端 Auto 路由规则（`AIServiceLoader.inferServiceType`）

| 传入 action 关键字 | 推断服务 |
|---|---|
| `generate-mindmap`, `expand-node`, `summarize`, `tag` | AGENT（DefaultAgentService） |
| `chat`, `message`, `question`, `answer` | CHAT（DefaultChatService） |
| `serviceType` 未传或传 `"auto"` | 根据 action 自动推断 |

### 4.3 三种模式的前端行为对比

| 模式 | UI形态 | 用户操作 | 前端传参 | 后端路由 |
|---|---|---|---|---|
| **Auto** | 单一自然语言输入框 | 自由描述意图 | 不传 serviceType | BufferLayerRouter → 自动推断 |
| **Chat** | 对话气泡列表 | 问答对话 | `serviceType='chat'` | DefaultChatService |
| **Build** | 操作卡片（4个功能） | 点击指定操作 | `serviceType='agent'` + `action=xxx` | DefaultAgentService |

### 4.4 模式与模型的正交关系

Auto/Chat/Build 控制**路由策略**，ModelSelector 控制**使用哪个模型**，两者完全独立：

```
[ Auto 模式 ] + [ DashScope qwen-max ]  →  后端自动路由 + 用通义千问回答
[ Chat 模式 ] + [ ERNIE ernie-4.5 ]   →  强制对话服务 + 用文心一言回答
[ Build 模式 ] + [ OpenRouter gpt-4o ] →  强制Agent服务 + 用GPT-4o操作
```

---

## 五、关键问题修复说明

### 5.1 国内模型（DashScope/ERNIE）配置后无法生效

**后端 Bug**（需同步反馈给后端同学修复）：

`DefaultChatService.isProviderConfigured()` 和 `DefaultAgentService.isProviderConfigured()` 均只检查：
```java
return isNonEmpty(configuration.getOpenRouterKey())
    || isNonEmpty(configuration.getGeminiKey())
    || configuration.hasOllamaServiceAddress();
// ❌ 缺少 DashScope 和 ERNIE 判断
```

导致配置了国内模型 Key 但服务仍报"未配置"。

**前端应对**：在 `ModelSelector` 和 Build 面板中，当模型列表为空时，给出明确提示：
```
⚠️ 未检测到已配置的 AI Provider
请在 Freeplane → 偏好设置 → AI 配置中填写 API Key
支持：OpenRouter / Google Gemini / DashScope(通义千问) / ERNIE(文心一言) / Ollama
```

### 5.2 Build 操作返回结果是文字，不直接操作导图

后端 `DefaultAgentService` 的 expand/summarize/tag 均返回 AI 生成的 **JSON 文字**（非自动执行工具），前端拿到 `result` 字段后需展示结果并提示用户"AI建议已生成，是否应用？"

**前端策略**：
- Build 面板下方"结果预览"展示 AI 返回的文字
- 提供"应用到导图"按钮（解析JSON → 调用 createNode/editNode）
- 或直接展示文字供用户参考手动操作

---

### 5.3 自定义模型配置数据流

```
用户打开 ModelConfigPanel → 选择 Provider（如 DashScope）
  → 填写 API Key（自动填充 Base URL 和默认模型）
  → 点击"保存并刷新"
  → aiStore.saveCustomModel(config)
  → aiApi.saveModelConfig({ providerName, apiKey, baseUrl, modelName })
  → POST /api/ai/config/save  （需后端新增）
  → 后端 ResourceController.setProperty("ai_dashscope_key", apiKey)
  → 返回 { success: true }
  → aiStore.fetchModelList()  ← 刷新模型列表
  → ModelSelector 下拉自动出现 "DashScope: qwen-max"
  → 配置面板关闭
```

**后端需配合事项**：

当前后端 `AiRestController` **无保存配置的 REST 接口**，配置仅能通过桌面端偏好设置完成。需新增：

```java
// POST /api/ai/config/save
// 接收: { providerName, apiKey, baseUrl?, modelName? }
// 逻辑: ResourceController.setProperty("ai_{providerName}_key", apiKey)
// 返回: { success: true }
```

**前端降级方案**（若后端暂不新增接口）：
- 在 ModelSelector 中显示提示："请在 Freeplane 桌面端 → 偏好设置 → AI 配置中填写 API Key"
- 提供配置指引链接（跳转文档）
- 用户手动在桌面端配置后，前端调用 `GET /api/ai/chat/models` 即可获取新模型

---

## 六、实现优先级

| 优先级 | 文件 | 改动内容 | 估时 |
|---|---|---|---|
| **P0** | `MindMapCanvas.vue` | 调用 `aiStore.init()` | 5分钟 |
| **P0** | `types/ai.ts` | 修正返回类型 | 10分钟 |
| **P0** | `aiStore.ts` | 新增 aiMode/buildLoading/buildResult/4个Build方法 | 30分钟 |
| **P1** | `Toolbar.vue` | 加 AI 按钮 + **Auto/Chat/Build 三 tab 并列** | 30分钟 |
| **P2** | `AiAutoPanel.vue` | 新建 Auto 自然语言输入面板 | 30分钟 |
| **P1** | `AiChatPanel.vue` | 新建 Chat 对话面板 | 45分钟 |
| **P1** | `NodeContextMenu.vue` | 接通 expandNode 调用 | 15分钟 |
| **P2** | `AiBuildPanel.vue` | 新建 Build 操作面板 | 60分钟 |
| **P2** | `ModelSelector.vue` | 无需改动（Auto不是模型选项） | — |
| **P1** | `ModelConfigPanel.vue` | 新建自定义模型配置对话框 | 45分钟 |
| **P1** | `aiApi.ts` / `aiStore.ts` | 新增 saveModelConfig 接口和方法 | 20分钟 |
| **P0** | 后端同学 | 新增 `POST /api/ai/config/save` 接口 | 20分钟 |
| **P3** | `Toolbar.vue` | 搜索接通 `/nodes/search` | 20分钟 |
| **P3** | `MindMapCanvas.vue` | 右键菜单注册 + 选中节点追踪 | 30分钟 |

---

## 七、数据流图

```
用户操作
  │
  ├─ Toolbar 点击 AI 按钮
  │     └─ aiStore.panelVisible = true
  │
  ├─ Toolbar 切换 Auto | Chat | Build tab（三者并列互斥）
  │     ├─ Auto  → aiStore.aiMode='auto'
  │     ├─ Chat  → aiStore.aiMode='chat'  + serviceType='chat'
  │     └─ Build → aiStore.aiMode='build' + serviceType='agent'
  │
  ├─ Auto 面板输入自然语言
  │     └─ POST /api/ai/chat/smart {input: '帮我展开节点'}
  │           └─ BufferLayerRouter → 自动路由
  │
  ├─ Chat 面板发送消息
  │     └─ aiStore.sendChat(msg, nodeId)
  │           └─ POST /api/ai/chat/message {serviceType:'chat'}
  │                 └─ 后端 AIServiceLoader 强制 → CHAT
  │                       └─ DefaultChatService → LangChain4j → 模型
  │
  ├─ Build 面板点击“AI展开”
  │     └─ aiStore.expandNode(nodeId)
  │           └─ POST /api/ai/build/expand-node {action:'expand-node', serviceType:'agent'}
  │                 └─ 后端 AIServiceLoader 强制 → AGENT
  │                       └─ DefaultAgentService.handleExpandNode → AI返回JSON文字
  │                             └─ 前端展示结果 → 用户选择是否应用
  │
  └─ 右键菜单 “AI展开节点”
        └─ aiStore.expandNode(nodeId)
              └─ 同 Build 面板流程
                    └─ 操作完成后 mapStore.loadMap() 刷新导图
```

---

**文档版本**: v1.0  
**对应后端**: Week 3 工作报告（SPI架构 + DashScope/ERNIE + Auto路由）  
**最后更新**: 2026-04-20
