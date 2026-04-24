# 前端 AI 功能详细改进方案

> 基于现有 `freeplane_web` 前端代码（Vue3 + Pinia + VueFlow）与后端 `freeplane_plugin_ai` REST API 现状，针对以下三项需求进行完整改进设计：
> 1. **主动选择模型**（Chat 与 Build 模式各自独立选择）
> 2. **智能体 Chat 面板**（自然对话模式）
> 3. **智能问答 Build 面板**（思维导图生成与智能缓冲层模式）
> 4. **前端自主配置 API Key**（用户在前端输入申请的 API Key 并写入后端）

---

## 一、现状差距分析

| 功能点 | 当前状态 | 差距 |
|--------|----------|------|
| 模型选择器 | `ModelSelector.vue` 已实现 | **未集成到任何页面**，没有 mode 区分 |
| Chat 对话 | `aiStore.sendChat()` 有逻辑 | **无 UI 面板**，方法无法被用户触发 |
| Build 智能问答 | `smartRequest()` API 已存在 | **无任何 UI 入口**，`buildHistory` 状态缺失 |
| AI 面板显示 | `panelVisible` 状态存在 | **始终未被渲染**，入口按钮缺失 |
| 自定义 API Key | 不支持 | 完全缺失，只能在 Freeplane 桌面端偏好设置中配置 |

---

## 二、文件改动总览

### 修改文件

| 文件路径 | 改动类型 | 说明 |
|----------|----------|------|
| `src/types/ai.ts` | 新增类型 | `AiMode`、`BuildMessage`、`CustomModelConfig`、`ProviderPreset` |
| `src/api/aiApi.ts` | 新增函数 | `saveModelConfig()`；`smartRequest` 增加 `modelSelection` 字段 |
| `src/stores/aiStore.ts` | 扩展状态与方法 | 双模式状态、Build 历史、配置面板状态、`sendSmartBuild`、`saveCustomModel` |
| `src/components/ai/ModelSelector.vue` | 改造 | 支持 `mode` prop、增加配置入口按钮 ⚙️ |
| `src/components/MindMapCanvas.vue` | 引入 AI 面板 | 添加 `AiPanel` 组件与 FAB 浮动按钮 |

### 新增文件

| 文件路径 | 说明 |
|----------|------|
| `src/components/ai/AiPanel.vue` | Chat / Build 双模式主面板容器 |
| `src/components/ai/ChatView.vue` | Chat 子视图（气泡流对话） |
| `src/components/ai/BuildView.vue` | Build 子视图（智能问答卡片结果） |
| `src/components/ai/ModelConfigPanel.vue` | 自定义 API Key 配置弹窗 |

### 后端配合（需同步修改）

| 文件路径 | 改动 |
|----------|------|
| `freeplane_plugin_ai/src/.../AiRestController.java` | 新增 `handleSaveConfig()` 方法 |
| `freeplane_plugin_ai/src/.../RestApiRouter.java` | 注册 `POST /api/ai/config` 路由 |

---

## 三、类型扩展：`src/types/ai.ts`

在现有类型基础上**追加**以下定义：

```typescript
/** AI 模式：智能体对话 或 智能问答构建 */
export type AiMode = 'chat' | 'build'

/** Build 模式单条历史记录 */
export interface BuildMessage {
  input: string          // 用户输入的自然语言
  response: SmartResponse // 缓冲层返回结果
  timestamp: number
}

/** 自定义模型配置（前端表单数据结构） */
export interface CustomModelConfig {
  providerName: string          // provider 标识，如 openrouter
  providerDisplayName: string   // 显示名，如 OpenRouter
  apiKey: string                // 用户填写的 API Key
  modelName: string             // 指定模型名，如 openai/gpt-4o
  baseUrl?: string              // 可选：自定义 Base URL（如 Ollama 本地部署）
}

/** 预设 Provider 配置（用于配置面板下拉/Tab 选项） */
export interface ProviderPreset {
  providerName: string
  providerDisplayName: string
  defaultModel: string          // 该 provider 的推荐默认模型
  placeholder: string           // API Key 输入框占位文本
  apiKeyUrl: string             // 官方获取 Key 的链接
}
```

---

## 四、API 层扩展：`src/api/aiApi.ts`

### 4.1 `smartRequest` 增加模型选择字段

```typescript
// 修改前
export function smartRequest(data: { input: string })

// 修改后：增加可选 modelSelection，允许 Build 模式覆盖缓冲层自动选择
export function smartRequest(data: {
  input: string
  modelSelection?: string   // 若传入，后端优先使用该模型；否则缓冲层自动路由
})
```

### 4.2 新增 `saveModelConfig()`

```typescript
/**
 * POST /api/ai/config
 * 将前端填写的 API Key 写入后端 Freeplane 偏好设置，写入后刷新模型列表即生效。
 * 注意：API Key 仅在本次请求中传输，前端不持久化。
 */
export function saveModelConfig(data: {
  providerName: string   // provider 标识
  apiKey: string         // API Key 明文（HTTPS 传输）
  modelName?: string     // 可选：指定默认模型，否则后端使用 provider 默认值
}) {
  return api.post<{
    success: boolean
    providerName: string
    message: string
  }>('/ai/config', data)
}
```

---

## 五、状态管理扩展：`src/stores/aiStore.ts`

### 5.1 新增状态

```typescript
// 当前激活的 AI 模式
const activeMode = ref<AiMode>('chat')

// Build 模式历史记录（最新在前）
const buildHistory = ref<BuildMessage[]>([])

// Chat 和 Build 模式各自独立的模型选择
const modelForMode = ref<{ chat: string; build: string }>({
  chat: '',
  build: ''
})

// 自定义配置面板相关状态
const configPanelVisible = ref(false)
const configSaving = ref(false)
const configError = ref<string>('')
```

### 5.2 新增方法

| 方法名 | 说明 |
|--------|------|
| `switchMode(mode)` | 切换 chat / build 激活模式 |
| `setModelForMode(mode, modelName)` | 分别设置两种模式的模型选择，不互相覆盖 |
| `sendSmartBuild(input)` | 调用 `/api/ai/smart`，结果追加到 `buildHistory` |
| `saveCustomModel(config)` | 调用 `/api/ai/config` 写入 Key，成功后刷新模型列表并关闭配置面板 |
| `clearBuild()` | 清空 Build 历史记录 |
| `openConfigPanel()` | 打开配置弹窗 |
| `closeConfigPanel()` | 关闭配置弹窗并清除错误信息 |

### 5.3 `fetchModelList` 同步初始化双模式模型

```typescript
// 模型列表加载后，若两种模式均未选中，自动选中第一个模型
if (modelList.value.length > 0) {
  if (!modelForMode.value.chat) modelForMode.value.chat = modelList.value[0].modelName
  if (!modelForMode.value.build) modelForMode.value.build = modelList.value[0].modelName
}
```

---

## 六、组件详细设计

### 6.1 `AiPanel.vue` — 主面板

**布局结构：**

```
┌──────────────────────────────────────┐
│  [✕ 关闭]              AI 助手       │  ← 标题栏
│  ──────────────────────────────────  │
│  [ Chat 智能体 ]  [ Build 智能问答 ]  │  ← Tab 切换
│  ──────────────────────────────────  │
│  模型：[下拉选择器 ▼]  [⚙️]           │  ← ModelSelector（mode 绑定）
│  ──────────────────────────────────  │
│                                      │
│      <ChatView /> 或 <BuildView />    │  ← 根据 activeMode 切换
│                                      │
└──────────────────────────────────────┘
```

**关键实现要点：**
- 面板以 `position: fixed` 右侧侧边栏形式呈现，宽度 `360px`，高度 `100vh`
- `v-if="aiStore.panelVisible"` 控制显示
- Tab 切换调用 `aiStore.switchMode(mode)`
- `ModelSelector` 传入 `:mode="aiStore.activeMode"`，实现两个 Tab 模型各自独立

### 6.2 `ChatView.vue` — 智能体对话视图

**功能：**
- 气泡流消息列表，区分 `user`（右对齐蓝色）和 `assistant`（左对齐灰色）角色
- 底部输入框 + 发送按钮，支持 `Enter` 发送（`Shift+Enter` 换行）
- 发送中显示"思考中..."动画占位气泡
- 右上角清空对话按钮

**数据来源：** `aiStore.chatHistory`，调用 `aiStore.sendChat(message)`

### 6.3 `BuildView.vue` — 智能问答视图

**功能：**
- 顶部输入框（多行 textarea）+ "生成"按钮
- 结果以卡片形式展示，每张卡片包含：
  - 状态标记（✅ 成功 / ❌ 失败）
  - 使用的模型（`usedModel`）
  - 质量评分进度条（`qualityScore / 100`）
  - 缓冲层名称（`bufferLayer`）
  - 处理耗时（`processingTime ms`）
  - 可折叠的处理日志（`logs[]`）
  - 若成功，展示返回的思维导图数据（`data`）

**数据来源：** `aiStore.buildHistory`，调用 `aiStore.sendSmartBuild(input)`

### 6.4 `ModelConfigPanel.vue` — API Key 配置弹窗

**布局：**

```
┌─────────────────────────────────────┐
│ ⚙️ 配置自定义模型              [✕]  │
│ ─────────────────────────────────── │
│ [OpenRouter] [Gemini] [Qwen] [自定义]│  ← Provider Tab
│ ─────────────────────────────────── │
│ API Key: [••••••••••••••]  [👁]     │  ← 密码框 + 显示切换
│          获取 API Key →              │  ← 链接跳转官方
│ 模型名称: [openai/gpt-4o      ]     │  ← 可选，默认填充
│ ─────────────────────────────────── │
│ ❌ 错误信息（若有）                  │
│           [取消]  [保存并刷新]       │
└─────────────────────────────────────┘
```

**预设 Provider 列表：**

| Provider | 默认模型 | Key 获取链接 |
|----------|----------|-------------|
| OpenRouter | `openai/gpt-4o` | https://openrouter.ai/keys |
| Google Gemini | `gemini-2.0-flash` | https://aistudio.google.com/app/apikey |
| DashScope (Qwen) | `qwen-max` | https://dashscope.console.aliyun.com/apiKey |
| ERNIE (Baidu) | `ernie-4.5` | https://console.bce.baidu.com/iam/ |
| 自定义 | 用户填写 | — |

**安全约束：**
- API Key 使用 `type="password"` 输入框，默认不可见
- **前端不持久化 API Key**（不存入 localStorage、不放入 Pinia 持久化）
- Key 仅在点击"保存"时通过 HTTPS POST 发送给后端一次
- 保存成功后自动调用 `fetchModelList()` 刷新可用模型，配置面板自动关闭

### 6.5 `ModelSelector.vue` 改造

**改造点：**

```typescript
// 新增 mode prop
const props = defineProps<{ mode?: AiMode }>()

// computed 绑定到对应模式的选择
const selectedModel = computed({
  get: () => props.mode ? aiStore.modelForMode[props.mode] : aiStore.currentModel,
  set: (value) => {
    if (props.mode) aiStore.setModelForMode(props.mode, value)
    else aiStore.switchModel(value)
  }
})
```

**UI 新增：** 选择器右侧添加 `⚙️` 按钮，点击打开 `ModelConfigPanel`（调用 `aiStore.openConfigPanel()`）

### 6.6 `MindMapCanvas.vue` 改造

新增内容：

```html
<!-- AI 面板 -->
<AiPanel />

<!-- 右下角 FAB 浮动按钮 -->
<button class="ai-fab" @click="aiStore.togglePanel" title="AI 助手">
  🤖
</button>
```

FAB 按钮样式：`position: fixed; right: 24px; bottom: 24px; z-index: 900;`

---

## 七、后端配合改动

### 7.1 新增接口：`POST /api/ai/config`

**`AiRestController.java` 新增方法：**

```
请求体：
{
  "providerName": "openrouter",
  "apiKey": "sk-or-xxx",
  "modelName": "openai/gpt-4o"    // 可选
}

响应：
{
  "success": true,
  "providerName": "openrouter",
  "message": "配置已保存，刷新模型列表后生效"
}
```

**实现逻辑：**
- 从请求体取 `providerName`、`apiKey`、`modelName`
- property key 规则：`ai_{providerName}_key`（如 `ai_openrouter_key`）
- 调用 `ResourceController.setProperty(key, apiKey)` 写入 Freeplane 偏好
- 若指定 `modelName`，同步写入 `ai_{providerName}_model`
- 与 `handleGetModels` 的读取逻辑完全兼容，写入后立即生效

### 7.2 路由注册：`RestApiRouter.java`

在 `buildAiHandler()` 的 switch-case 中追加：

```java
case "/api/ai/config":
    aiController.handleSaveConfig(exchange);
    break;
```

路由注释表更新：

```
POST /api/ai/config             → AiRestController.handleSaveConfig
```

---

## 八、数据流图

### Chat 模式数据流

```
用户在 ChatView 输入消息
  → aiStore.sendChat(message)
  → aiApi.aiChat({ message, modelSelection: modelForMode.chat })
  → POST /api/ai/chat
  → 后端 AiRestController.handleChat()
  → 返回 { reply, tokenUsage }
  → chatHistory 追加 assistant 消息
  → ChatView 气泡流更新
```

### Build 模式数据流

```
用户在 BuildView 输入自然语言
  → aiStore.sendSmartBuild(input)
  → aiApi.smartRequest({ input, modelSelection: modelForMode.build })
  → POST /api/ai/smart
  → 后端 BufferLayerRouter → MindMapBufferLayer 处理管道
  → 返回 SmartResponse { usedModel, qualityScore, bufferLayer, logs, data }
  → buildHistory 追加结果卡片
  → BuildView 卡片列表更新
```

### API Key 配置数据流

```
用户打开 ModelConfigPanel → 选择 Provider → 填写 API Key
  → 点击"保存并刷新"
  → aiStore.saveCustomModel(config)
  → aiApi.saveModelConfig({ providerName, apiKey, modelName })
  → POST /api/ai/config
  → 后端 ResourceController.setProperty("ai_openrouter_key", apiKey)
  → 返回 { success: true }
  → aiStore.fetchModelList()  ← 刷新模型列表
  → ModelSelector 下拉自动出现新模型
  → 配置面板关闭
```

---

## 九、设计约束与权衡

| 约束 | 说明 |
|------|------|
| **原有接口零变更** | `handleChat`、`handleGetModels` 等现有接口不修改，新功能通过新路由扩展 |
| **API Key 不持久化于前端** | 仅在提交时一次性 POST，避免 XSS 泄漏风险 |
| **Build 模型选择为"偏好覆盖"** | 若用户选了模型，后端优先使用；若未选，缓冲层自动路由，保留智能调度能力 |
| **Chat 模型选择直接透传** | Chat 模式不经过缓冲层，modelSelection 直接由 `handleChat` 使用 |
| **双模式模型互不干扰** | `modelForMode.chat` 和 `modelForMode.build` 独立维护，Tab 切换不重置 |
| **FAB 不遮挡画布交互** | z-index 低于 Modal（1000+），高于 VueFlow 控件（100-） |
| **配置面板自定义 provider** | 选择"自定义"时，providerName 由用户手动输入，key 规则同样适用 `ai_{name}_key` |

---

## 十、实现优先级建议

| 优先级 | 内容 | 理由 |
|--------|------|------|
| P0 | `AiPanel.vue` + `ChatView.vue` + FAB 按钮 + `MindMapCanvas` 改造 | 用户最直接感知的功能入口 |
| P0 | `aiStore.ts` 扩展 + `types/ai.ts` 扩展 | 其他组件的基础依赖 |
| P1 | `ModelConfigPanel.vue` + 后端 `/api/ai/config` 接口 | 让无桌面端访问权限的用户也能配置模型 |
| P1 | `ModelSelector.vue` 改造（mode prop + ⚙️ 按钮） | 模型选择与配置入口的打通 |
| P2 | `BuildView.vue` + `sendSmartBuild` | 依赖缓冲层后端完善程度 |
| P2 | `apiApi.ts` 中 `smartRequest` 增加 `modelSelection` | Build 模式模型覆盖能力 |
