# AI 功能集成到 freeplane_web 实施计划

**目标**：将 AI 插件的完整功能集成到 freeplane_web 网页版中

**实施周期**：2-3 天

**责任人**：成员 C

---

## 📋 集成范围

### 需要集成的 AI 功能

| 功能 | 接口 | 优先级 | 说明 |
|------|------|--------|------|
| AI 对话 | POST /api/ai/chat | 🔴 P0 | 流式对话，Markdown 渲染 |
| 模型选择 | GET /api/ai/models | 🔴 P0 | 动态加载可用模型 |
| 节点扩展 | POST /api/ai/expand-node | 🟡 P1 | AI 生成子节点 |
| 分支摘要 | POST /api/ai/summarize | 🟡 P1 | 生成分支摘要 |
| 语义搜索 | POST /api/nodes/search | 🟡 P1 | 关键词搜索节点 |
| 智能缓冲层 | POST /api/ai/smart | 🟢 P2 | 自然语言理解 |

---

## 🏗️ 架构设计

### 目录结构

```
freeplane_web/src/
├── api/
│   ├── mapApi.ts              ← 已有
│   ├── nodeApi.ts             ← 已有
│   └── aiApi.ts               ← 新增：AI 接口封装
├── stores/
│   ├── mapStore.ts            ← 已有
│   └── aiStore.ts             ← 新增：AI 状态管理
├── components/
│   ├── MindMapCanvas.vue      ← 已有
│   ├── Toolbar.vue            ← 已有
│   ├── NodeContextMenu.vue    ← 已有
│   ├── NodeEditPanel.vue      ← 已有
│   ├── ActionModal.vue        ← 已有
│   └── ai/                    ← 新增：AI 组件目录
│       ├── AiChatPanel.vue    ← AI 对话面板
│       ├── ModelSelector.vue  ← 模型选择器
│       ├── ExpandNodeDialog.vue ← 节点扩展弹窗
│       ├── SummarizePanel.vue ← 分支摘要面板
│       └── SemanticSearchBar.vue ← 语义搜索栏
├── types/
│   └── ai.ts                  ← 新增：AI 类型定义
└── utils/
    ├── treeToFlow.ts          ← 已有
    └── aiHelpers.ts           ← 新增：AI 辅助工具
```

---

## 🎯 实施阶段

### Phase 1：基础设施（半天）

#### 1.1 安装依赖

```bash
cd freeplane_web
npm install marked highlight.js
npm install -D @types/marked
```

#### 1.2 创建类型定义

**文件**：`src/types/ai.ts`

```typescript
// AI 模型
export interface AIModel {
  providerName: string
  providerDisplayName: string
  modelName: string
  displayName: string
  isFree: boolean
}

// 对话消息
export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: number
  nodeId?: string
}

// Token 使用统计
export interface TokenUsage {
  inputTokens: number
  outputTokens: number
}

// 节点扩展结果
export interface ExpandNodeResult {
  success: boolean
  nodeId: string
  createdNodes: Array<{
    nodeId: string
    text: string
  }>
  summary: string
}

// 分支摘要结果
export interface SummarizeResult {
  success: boolean
  nodeId: string
  summary: string
  wordCount: number
  writtenToNote: boolean
}

// 语义搜索结果
export interface SearchResult {
  nodeId: string
  text: string
  path: string
}

// 标签结果
export interface TagResult {
  nodeId: string
  tags: string[]
}
```

#### 1.3 创建 API 封装

**文件**：`src/api/aiApi.ts`

```typescript
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 1. 获取模型列表
export function getAiModels() {
  return api.get<{ models: import('@/types/ai').AIModel[] }>('/ai/models')
}

// 2. AI 对话
export function aiChat(data: {
  message: string
  model?: string
  nodeId?: string
}) {
  return api.post<{
    reply: string
    tokenUsage: import('@/types/ai').TokenUsage
  }>('/ai/chat', data)
}

// 3. 节点扩展
export function expandNode(data: {
  nodeId: string
  count?: number
  depth?: number
  focus?: string
}) {
  return api.post<import('@/types/ai').ExpandNodeResult>('/ai/expand-node', data)
}

// 4. 分支摘要
export function summarizeBranch(data: {
  nodeId: string
  maxWords?: number
  writeToNote?: boolean
}) {
  return api.post<import('@/types/ai').SummarizeResult>('/ai/summarize', data)
}

// 5. 节点搜索
export function searchNodes(data: {
  query: string
  caseSensitive?: boolean
}) {
  return api.post<{
    results: import('@/types/ai').SearchResult[]
    totalCount: number
  }>('/nodes/search', data)
}

// 6. 自动标签
export function autoTag(data: {
  nodeIds: string[]
}) {
  return api.post<{
    results: import('@/types/ai').TagResult[]
    message: string
  }>('/ai/tag', data)
}

// 7. 智能缓冲层
export function smartRequest(data: {
  input: string
}) {
  return api.post('/ai/smart', data)
}

export default api
```

#### 1.4 创建 AI Store

**文件**：`src/stores/aiStore.ts`

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AIModel, ChatMessage } from '@/types/ai'
import * as aiApi from '@/api/aiApi'
import { useMapStore } from './mapStore'

export const useAIStore = defineStore('ai', () => {
  // 状态
  const modelList = ref<AIModel[]>([])
  const currentModel = ref<string>('')
  const chatHistory = ref<ChatMessage[]>([])
  const streaming = ref(false)
  const loading = ref(false)

  // 获取模型列表
  const fetchModelList = async () => {
    try {
      const response = await aiApi.getAiModels()
      modelList.value = response.data.models
      if (modelList.value.length > 0 && !currentModel.value) {
        currentModel.value = modelList.value[0].modelName
      }
    } catch (error) {
      console.error('Failed to fetch models:', error)
    }
  }

  // AI 对话
  const sendChat = async (message: string, nodeId?: string) => {
    if (!message.trim()) return

    // 添加用户消息
    chatHistory.value.push({
      role: 'user',
      content: message,
      timestamp: Date.now(),
      nodeId
    })

    // 添加助手占位
    const assistantIndex = chatHistory.value.push({
      role: 'assistant',
      content: '',
      timestamp: Date.now()
    }) - 1

    streaming.value = true
    try {
      const response = await aiApi.aiChat({
        message,
        model: currentModel.value,
        nodeId
      })
      
      chatHistory.value[assistantIndex].content = response.data.reply
    } catch (error: any) {
      chatHistory.value[assistantIndex].content = `对话失败：${error.message}`
    } finally {
      streaming.value = false
    }
  }

  // 清空对话
  const clearChat = () => {
    chatHistory.value = []
  }

  // 初始化
  const init = async () => {
    await fetchModelList()
  }

  return {
    modelList,
    currentModel,
    chatHistory,
    streaming,
    loading,
    fetchModelList,
    sendChat,
    clearChat,
    init
  }
})
```

---

### Phase 2：核心组件（1天）

#### 2.1 AI 对话面板

**文件**：`src/components/ai/AiChatPanel.vue`

**功能**：
- 流式对话展示
- Markdown 渲染
- 代码高亮
- 对话历史

**关键实现**：
```vue
<template>
  <div class="ai-chat-panel">
    <div class="chat-header">
      <ModelSelector />
      <button @click="clearChat" class="clear-btn">清空</button>
    </div>
    
    <div class="chat-messages" ref="messagesContainer">
      <div v-for="(msg, index) in chatHistory" :key="index"
           :class="['message', msg.role]">
        <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
      </div>
      <div v-if="streaming" class="message assistant">
        <div class="message-content typing">
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
        </div>
      </div>
    </div>
    
    <div class="chat-input">
      <textarea
        v-model="inputMessage"
        @keydown.enter.exact.prevent="sendMessage"
        :disabled="streaming"
        placeholder="输入消息... (Enter 发送)"
      ></textarea>
      <button @click="sendMessage" :disabled="streaming || !inputMessage.trim()">
        发送
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useAIStore } from '@/stores/aiStore'
import ModelSelector from './ModelSelector.vue'
import { marked } from 'marked'
import hljs from 'highlight.js'

const aiStore = useAIStore()
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement>()

const chatHistory = computed(() => aiStore.chatHistory)
const streaming = computed(() => aiStore.streaming)

const sendMessage = async () => {
  if (!inputMessage.value.trim() || streaming.value) return
  
  const message = inputMessage.value
  inputMessage.value = ''
  
  // 获取当前选中节点
  const mapStore = useMapStore()
  const selectedNode = mapStore.getSelectedNode()
  
  await aiStore.sendChat(message, selectedNode?.id)
}

const renderMarkdown = (content: string) => {
  if (!content) return ''
  
  marked.setOptions({
    highlight: (code, lang) => {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, { language: lang }).value
      }
      return hljs.highlightAuto(code).value
    }
  })
  
  return marked.parse(content)
}

const clearChat = () => {
  aiStore.clearChat()
}

watch(() => chatHistory.value.length, async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
})
</script>

<style scoped>
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
}

.chat-header {
  padding: 12px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
}

.message.user {
  background: #e3f2fd;
  margin-left: 40px;
}

.message.assistant {
  background: #f5f5f5;
  margin-right: 40px;
}

.chat-input {
  padding: 12px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  gap: 8px;
}

.chat-input textarea {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  min-height: 60px;
}
</style>
```

#### 2.2 模型选择器

**文件**：`src/components/ai/ModelSelector.vue`

#### 2.3 节点扩展弹窗

**文件**：`src/components/ai/ExpandNodeDialog.vue`

#### 2.4 分支摘要面板

**文件**：`src/components/ai/SummarizePanel.vue`

---

### Phase 3：集成到画布（半天）

#### 3.1 修改 Toolbar 添加 AI 入口

**文件**：`src/components/Toolbar.vue`

添加 AI 功能按钮：
- AI 对话面板切换
- 节点扩展
- 分支摘要
- 语义搜索

#### 3.2 修改右键菜单集成 AI

**文件**：`src/components/NodeContextMenu.vue`

添加右键菜单项：
- AI 扩展此节点
- AI 生成分支摘要
- AI 为此节点添加标签
- 基于此节点对话

#### 3.3 修改 MindMapCanvas 集成 AI 面板

**文件**：`src/components/MindMapCanvas.vue`

```vue
<template>
  <div class="mindmap-container">
    <VueFlow>...</VueFlow>
    
    <!-- AI 对话面板（可切换显示） -->
    <div v-if="showAiPanel" class="ai-panel-sidebar">
      <AiChatPanel />
    </div>
    
    <!-- AI 功能弹窗 -->
    <ExpandNodeDialog v-model:visible="expandDialogVisible" />
    <SummarizePanel v-model:visible="summarizeDialogVisible" />
    <SemanticSearchBar v-model:visible="searchBarVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AiChatPanel from './ai/AiChatPanel.vue'
import ExpandNodeDialog from './ai/ExpandNodeDialog.vue'
import SummarizePanel from './ai/SummarizePanel.vue'
import SemanticSearchBar from './ai/SemanticSearchBar.vue'

const showAiPanel = ref(false)
const expandDialogVisible = ref(false)
const summarizeDialogVisible = ref(false)
const searchBarVisible = ref(false)
</script>

<style>
.ai-panel-sidebar {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 400px;
  box-shadow: -2px 0 8px rgba(0,0,0,0.1);
  z-index: 1000;
}
</style>
```

---

### Phase 4：节点操作联动（半天）

#### 4.1 扩展节点后更新画布

```typescript
// ExpandNodeDialog.vue 中
const handleExpand = async () => {
  loading.value = true
  try {
    const response = await aiApi.expandNode({
      nodeId: nodeId.value,
      count: count.value,
      focus: focus.value
    })
    
    if (response.data.success) {
      // 通知 mapStore 刷新
      await mapStore.refreshMap()
      
      // 高亮新节点
      response.data.createdNodes.forEach(node => {
        highlightNode(node.nodeId)
      })
      
      ElMessage.success('节点扩展成功')
    }
  } finally {
    loading.value = false
  }
}
```

#### 4.2 摘要写入后更新节点

```typescript
// SummarizePanel.vue 中
const handleSummarize = async () => {
  loading.value = true
  try {
    const response = await aiApi.summarizeBranch({
      nodeId: nodeId.value,
      writeToNote: true
    })
    
    if (response.data.success) {
      await mapStore.refreshMap()
      ElMessage.success('摘要生成成功')
    }
  } finally {
    loading.value = false
  }
}
```

---

## 📦 依赖清单

### 新增依赖

```json
{
  "dependencies": {
    "marked": "^12.0.0",
    "highlight.js": "^11.9.0"
  },
  "devDependencies": {
    "@types/marked": "^5.0.0"
  }
}
```

---

## ✅ 验收标准

### 功能验收

| 功能 | 验收标准 | 状态 |
|------|---------|------|
| AI 对话 | 能正常对话，Markdown 渲染正确 | ⬜ |
| 模型选择 | 显示已配置模型，切换正常 | ⬜ |
| 节点扩展 | AI 生成子节点，画布自动更新 | ⬜ |
| 分支摘要 | 生成摘要，可写入节点备注 | ⬜ |
| 语义搜索 | 搜索结果准确，点击定位节点 | ⬜ |
| 右键菜单 | 所有 AI 功能可通过右键触发 | ⬜ |

### 性能验收

| 指标 | 标准 | 状态 |
|------|------|------|
| 首屏加载 | < 2 秒 | ⬜ |
| AI 响应 | < 5 秒（不含 AI 处理时间） | ⬜ |
| 画布刷新 | < 500ms | ⬜ |

### 兼容性验收

| 浏览器 | 版本 | 状态 |
|--------|------|------|
| Chrome | 最新版 | ⬜ |
| Firefox | 最新版 | ⬜ |
| Safari | 最新版 | ⬜ |
| Edge | 最新版 | ⬜ |

---

## ⚠️ 注意事项

### 1. API 代理配置

**文件**：`vite.config.ts`（已有 ✅）

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:6299',
      changeOrigin: true,
      secure: false
    }
  }
}
```

### 2. 跨域问题

后端已实现 CORS 过滤器，无需额外配置。

### 3. 流式输出

当前后端不支持流式输出，后续需要：
- 后端实现 Server-Sent Events (SSE)
- 前端使用 EventSource 接收

### 4. 状态同步

AI 操作后必须调用 `mapStore.refreshMap()` 刷新画布。

---

## 📝 实施时间线

| 阶段 | 内容 | 预计时间 | 完成日期 |
|------|------|---------|---------|
| Phase 1 | 基础设施 | 半天 | Day 1 上午 |
| Phase 2 | 核心组件 | 1 天 | Day 1 下午 - Day 2 |
| Phase 3 | 集成到画布 | 半天 | Day 3 上午 |
| Phase 4 | 节点联动 | 半天 | Day 3 下午 |
| 测试 | 功能测试 | 半天 | Day 4 上午 |

---

## 🚀 快速启动

```bash
# 1. 安装依赖
cd freeplane_web
npm install

# 2. 启动 Freeplane 桌面端
cd ..
BIN\freeplane.bat

# 3. 启动前端开发服务器
cd freeplane_web
npm run dev

# 4. 访问 http://localhost:5173
```

---

## 📚 参考文档

- [Stub 接口完善报告](./stub-implementation-report.md)
- [API 测试指南](./stub-test-guide.md)
- [前端优化计划](./frontend-optimization-plan.md)
