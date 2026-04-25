# Freeplane Web 前端 AI 功能面板开发工作文档

**文档编号**：FP-WEB-AI-202604

**责任人**：孙楠（成员 C）

**开发周期**：3 周（与成员 B 并行开发）

**文档版本**：V1.0

**编制日期**：2026 年 4 月 29 日

## 一、开发任务概述

### 1.1 任务目标

设计并实现 Freeplane Web 前端 AI 交互核心面板，打通 AI 与思维导图画布的联动，提供智能对话、节点扩展、分支摘要、语义搜索等核心功能，提升用户思维导图编辑效率。

### 1.2 核心职责

- 开发 AI 状态管理模块、接口封装模块；
- 实现 5 个核心 AI 交互组件；
- 保障 AI 操作结果实时反馈至思维导图画布；
- 与成员 B 协同完成右键菜单 AI 功能集成。

### 1.3 交付标准

- 完整走通 AI 对话、节点展开、分支摘要三大核心流程；
- 组件功能正常、交互流畅，无控制台报错；
- 代码结构清晰，符合项目编码规范；
- 提供功能演示材料及开发总结。

## 二、技术栈与开发环境

|     |     |
| --- | --- |
| 类别  | 具体内容 |
| 核心技术 | Vue 3 + TypeScript + Pinia + VueFlow（思维导图渲染） |
| 辅助工具 | Marked（Markdown 渲染）、Highlight.js（代码高亮）、Axios（接口请求） |
| 开发环境 | Node.js 16+、VS Code、Chrome 浏览器 |
| 版本控制 | Git + Gitee（分支：feature/c） |

## 三、核心模块与组件开发详情

### 3.1 基础模块开发

#### 3.1.1 AI 状态管理：src/stores/aiStore.ts

**功能定位**：统一管理 AI 相关全局状态，提供核心操作方法，实现组件间状态共享。

**核心状态**：

- currentModel：当前选中 AI 模型（如通义千问、文心一言）；
- modelList：动态加载的 AI 模型列表（含 ID、名称、描述）；
- chatHistory：AI 对话历史记录（角色、内容、时间戳）；
- streaming：流式输出状态标识；
- semanticSearchResults：语义搜索结果（节点 ID、相关性评分、内容）。

**核心方法**：

|     |     |
| --- | --- |
| 方法名 | 功能描述 |
| fetchModelList | 从后端动态获取 AI 模型列表，初始化 modelList |
| sendStreamChat | 发送流式对话请求，逐字更新对话内容，支持绑定目标节点 ID |
| semanticSearchNodes | 调用语义搜索接口，存储搜索结果 |
| clearChatHistory | 清空对话历史（可选功能，用于模型切换后重置上下文） |

**关键代码片段（流式对话核心逻辑）**：

const sendStreamChat = async (content: string, nodeId?: string) => {

streaming.value = true;

// 记录用户输入

chatHistory.value.push({ role: 'user', content, timestamp: Date.now() });

// 初始化助手回复占位

const assistantIndex = chatHistory.value.push({

role: 'assistant',

content: '',

timestamp: Date.now()

}) - 1;

try {

await aiApi.streamChat({

model: currentModel.value,

content,

nodeId,

onChunk: (chunk) => {

// 逐字更新助手回复，实现流式效果

chatHistory.value\[assistantIndex\].content += chunk;

}

});

} catch (error) {

chatHistory.value\[assistantIndex\].content = \`对话失败：${error.message}\`;

} finally {

streaming.value = false;

}

};

**代码行数**：约 180 行

**开发要点**：确保状态响应式更新，流式输出时避免重复请求，错误处理友好。

#### 3.1.2 AI 接口封装：src/api/aiApi.ts

**功能定位**：封装所有 AI 相关接口，统一处理请求头、参数格式、错误捕获、流式响应解析，为上层组件提供简洁调用方式。

**核心接口**：

|     |     |     |     |
| --- | --- | --- | --- |
| 接口名 | 功能描述 | 请求方式 | 参数说明 |
| /ai/models | 获取 AI 模型列表 | GET | \-  |
| /ai/chat/stream | 流式 AI 对话 | POST | model（模型 ID）、content（对话内容）、nodeId（可选） |
| /ai/node/expand | 节点扩展（生成子节点） | POST | nodeId（目标节点 ID）、model、prompt（提示词） |
| /ai/node/summarize | 分支摘要（生成分支总结） | POST | nodeId（分支根节点 ID）、model |
| /ai/search/semantic | 语义搜索节点 | GET | keyword（关键词）、mapId（思维导图 ID） |

**关键代码片段（流式响应解析）**：

export const streamChat = async (params: {

model: string;

content: string;

nodeId?: string;

onChunk: (chunk: string) => void;

}) => {

const response = await fetch(\`${import.meta.env.VITE_API_BASE}/ai/chat/stream\`, {

method: 'POST',

headers: {

'Content-Type': 'application/json',

'Authorization': \`Bearer ${localStorage.getItem('token')}\`

},

body: JSON.stringify(params)

});

if (!response.ok) throw new Error(\`请求失败：${response.statusText}\`);

const reader = response.body.getReader();

const decoder = new TextDecoder('utf-8');

while (true) {

const { done, value } = await reader.read();

if (done) break;

const chunk = decoder.decode(value);

params.onChunk(chunk); // 回调传递流式数据

}

};

**代码行数**：约 120 行

**开发要点**：兼容流式与非流式请求，处理跨域问题，统一错误提示格式。

### 3.2 核心组件开发

#### 3.2.1 AiChatPanel.vue（AI 对话面板）

**组件路径**：src/components/ai/AiChatPanel.vue

**功能定位**：支持 AI 流式对话，实时渲染 Markdown 内容（含代码高亮），提供友好的对话交互界面。

**界面布局设计思路**：

- 采用左右分栏布局：左侧为对话历史展示区，右侧为输入区；
- 对话历史区分用户 / 助手消息，不同角色使用不同样式标识；
- 输入区提供文本输入框和发送按钮，流式输出时禁用输入和发送功能；
- 流式输出时显示加载动画（打字机效果辅助）。

**核心功能实现**：

1.  流式 Markdown 渲染：
    - 使用 marked 库解析 Markdown 内容，highlight.js 实现代码高亮；
    - 处理流式输出中未闭合的 Markdown 语法（如代码块、标题），避免样式错乱；
    - 示例代码：

import marked from 'marked';

import hljs from 'highlight.js';

// 初始化 Markdown 渲染配置

marked.setOptions({

highlight: (code, lang) => {

if (lang && hljs.getLanguage(lang)) {

return hljs.highlight(code, { language: lang }).value;

}

return hljs.highlightAuto(code).value;

}

});

// 实时渲染 Markdown（处理未闭合语法）

const renderMarkdown = (content: string) => {

let processedContent = content;

// 补全未闭合的代码块

const codeBlockCount = (content.match(/\`\`\`/g) || \[\]).length;

if (codeBlockCount % 2 !== 0) processedContent += '\\n\`\`\`';

return marked.parse(processedContent);

};

1.  对话绑定节点：获取当前画布选中节点 ID，随对话请求发送，使 AI 操作聚焦目标节点。

**代码行数**：约 280 行

**交互难点与解决方案**：

|     |     |
| --- | --- |
| 难点描述 | 解决方案 |
| 流式输出时 Markdown 语法未闭合导致样式错乱 | 实时检测未闭合语法（如代码块、列表），自动补全后再渲染 |
| 大文本流式输出时页面卡顿 | 使用虚拟滚动优化对话历史区，仅渲染可视区域内容 |
| 代码高亮延迟 | 分段渲染代码块，优先展示文本内容，高亮操作异步执行 |

#### 3.2.2 ModelSelector.vue（模型选择器）

**组件路径**：src/components/ai/ModelSelector.vue

**功能定位**：动态加载后端提供的 AI 模型列表，支持模型切换，同步更新全局状态。

**实现过程**：

1.  组件挂载时调用 aiStore.fetchModelList 加载模型列表；
2.  使用下拉框展示模型名称和描述，绑定 aiStore.currentModel 实现选中状态同步；
3.  模型切换时可选择是否清空对话历史，避免跨模型上下文冲突。

**关键代码片段**：

&lt;template&gt;

&lt;div class="model-selector"&gt;

\="model-label">选择 AI 模型：

v-model="aiStore.currentModel"

@change="handleModelChange"

class="model-select"

:disabled="aiStore.streaming"

\>

\-for="model in aiStore.modelList" :key="model.id" :value="model.id">

{{ model.name }}（{{ model.desc }}）

&lt;/option&gt;

&lt;/select&gt;

\>

&lt;script setup lang="ts"&gt;

import { onMounted } from 'vue';

import { useAIStore } from '@/stores/aiStore';

const aiStore = useAIStore();

// 组件挂载时加载模型列表

onMounted(() => {

if (aiStore.modelList.length === 0) aiStore.fetchModelList();

});

// 模型切换处理

const handleModelChange = () => {

// 可选：清空对话历史

// aiStore.clearChatHistory();

console.log(\`已切换至 AI 模型：${aiStore.currentModel}\`);

};

\*\*代码行数\*\*：约 60 行

\*\*开发要点\*\*：确保模型列表加载失败时的容错处理，下拉框在流式输出时禁用。

\#### 3.2.3 ExpandNodeDialog.vue（节点扩展弹窗）

\*\*组件路径\*\*：src/components/ai/ExpandNodeDialog.vue

\*\*功能定位\*\*：接收目标节点 ID，输入扩展提示词，调用 AI 接口生成子节点，实时添加至画布。

\*\*核心流程\*\*：

1\. 接收父组件传递的 \`nodeId\`（待扩展节点 ID）；

2\. 提供提示词输入框和模型选择器（复用 ModelSelector 组件）；

3\. 点击“扩展节点”按钮，调用 \`aiApi.expandNode\` 接口；

4\. 接口返回成功后，通过 \`mapStore.addNodes\` 方法将生成的子节点添加至画布；

5\. 弹窗自动关闭，画布高亮新生成的节点。

\*\*代码行数\*\*：约 150 行

\*\*开发要点\*\*：添加加载状态提示，处理接口调用失败场景（如网络错误、模型无响应）。

\#### 3.2.4 SummarizePanel.vue（分支摘要面板）

\*\*组件路径\*\*：src/components/ai/SummarizePanel.vue

\*\*功能定位\*\*：针对选中的分支节点，生成简洁摘要，支持将摘要作为新节点添加至分支或替换当前节点内容。

\*\*核心功能\*\*：

1\. 自动获取当前选中分支的根节点 ID；

2\. 调用 \`aiApi.summarizeBranch\` 接口生成分支摘要；

3\. 提供“添加为子节点”“替换当前节点”两种操作选项；

4\. 操作完成后，画布实时更新节点内容并高亮。

\*\*关键代码片段（摘要生成与画布更新）\*\*：

\`\`\`typescript

const generateSummary = async () => {

if (!nodeId.value) return ElMessage.warning('请先选中需要摘要的分支节点');

loading.value = true;

try {

const res = await aiApi.summarizeBranch(nodeId.value, aiStore.currentModel);

summaryContent.value = res.data.summary;

// 选择“添加为子节点”时更新画布

if (operationType.value === 'add') {

mapStore.addNode({

parentId: nodeId.value,

text: \`【分支摘要】${res.data.summary}\`,

data: { isSummary: true },

position: { x: 0, y: 0 }

});

ElMessage.success('摘要节点添加成功');

}

} catch (error) {

ElMessage.error(\`摘要生成失败：${error.message}\`);

} finally {

loading.value = false;

}

};

**代码行数**：约 120 行

**开发要点**：确保摘要内容长度适中，避免节点文本过长影响画布布局。

#### 3.2.5 SemanticSearchBar.vue（语义搜索栏）

**组件路径**：src/components/ai/SemanticSearchBar.vue

**功能定位**：支持输入关键词进行语义搜索，返回相关节点列表，点击结果后画布自动定位并高亮节点。

**核心流程**：

1.  输入关键词后，调用 aiStore.semanticSearchNodes 方法；
2.  展示搜索结果列表，包含节点内容和相关性评分；
3.  点击结果项，调用 mapStore.highlightNode 和 mapStore.locateNode 方法；
4.  画布自动滚动至目标节点并高亮显示。

**代码行数**：约 100 行

**开发要点**：搜索结果为空时显示友好提示，支持关键词防抖输入（避免频繁调用接口）。

## 四、AI 操作与画布联动实现

### 4.1 联动核心逻辑

通过 Pinia 实现 aiStore 与 mapStore（思维导图核心状态）的状态共享，AI 操作触发的节点数据变更直接更新 mapStore 中的响应式数据，VueFlow 组件自动监听数据变化并重新渲染画布。

### 4.2 关键联动方法（mapStore 新增）

|     |     |
| --- | --- |
| 方法名 | 功能描述 |
| highlightNode | 高亮目标节点，通过修改节点样式（背景色、边框）实现视觉反馈 |
| locateNode | 定位节点，调用 VueFlow 的 scrollToNode 和 fitView 方法，使节点居中显示 |
| addNodes | 添加新节点，接收节点数据数组，更新 nodes 列表，画布自动渲染新节点 |
| updateNode | 更新节点内容 / 样式，用于分支摘要替换节点等场景 |

### 4.3 联动示例（AI 扩展节点后画布更新）

// ExpandNodeDialog.vue 中调用

const handleExpand = async () => {

const res = await aiApi.expandNode(nodeId.value, aiStore.currentModel, prompt.value);

// 调用 mapStore 添加节点，画布自动更新

mapStore.addNodes(res.data.childrenNodes);

// 高亮新生成的节点

mapStore.highlightNode(res.data.childrenNodes\[0\].id);

// 定位到新节点

mapStore.locateNode(res.data.childrenNodes\[0\].id);

};

## 五、与成员 B 协作：右键菜单交互约定

### 5.1 交互目标

通过右键菜单触发 AI 功能（节点扩展、分支摘要、基于节点对话），实现操作入口统一。

### 5.2 协作约定

#### 5.2.1 事件传递方式

使用全局事件总线（EventBus）实现组件通信，约定以下事件名：

|     |     |     |
| --- | --- | --- |
| 事件名 | 功能描述 | 传递参数 |
| ai:expand-node | 触发 AI 节点扩展 | nodeId（节点 ID） |
| ai:summarize-branch | 触发 AI 分支摘要 | nodeId（节点 ID） |
| ai:chat-with-node | 触发基于节点的 AI 对话 | { nodeId, text } |

#### 5.2.2 交互流程

1.  成员 B 开发的右键菜单组件，在对应菜单项点击时触发上述事件；
2.  AI 组件（如 ExpandNodeDialog.vue）监听事件，接收参数并展示弹窗 / 面板；
3.  AI 操作完成后，通过事件总线反馈结果状态（成功 / 失败），成员 B 组件展示提示。

#### 5.2.3 代码示例

- 成员 B 右键菜单组件（MindMapCanvas.vue）：

// 右键菜单“AI 扩展节点”点击事件

const handleAIExpand = (nodeId) => {

eventBus.emit('ai:expand-node', nodeId);

};

- 成员 C AI 组件（ExpandNodeDialog.vue）：

onMounted(() => {

// 监听事件，接收节点 ID 并展示弹窗

eventBus.on('ai:expand-node', (nodeId) => {

nodeId.value = nodeId;

dialogVisible.value = true;

});

});

## 六、开发难点与解决方案汇总

|     |     |     |
| --- | --- | --- |
| 难点类别 | 具体问题描述 | 解决方案 |
| 流式交互 | 未闭合 Markdown 语法导致渲染错乱 | 实时检测语法完整性，自动补全未闭合标签（如代码块、列表） |
| 状态同步 | AI 操作与画布节点状态不一致 | 统一通过 mapStore 管理节点数据，AI 操作后直接更新响应式数据 |
| 性能优化 | 大文本流式输出导致页面卡顿 | 对话历史区使用虚拟滚动，仅渲染可视区域内容 |
| 兼容性 | 不同浏览器对 ReadableStream 支持差异 | 对不支持的浏览器降级为非流式输出，提示用户升级浏览器 |
| 协作开发 | 右键菜单与 AI 组件通信冲突 | 约定统一事件名和参数格式，通过事件总线解耦，避免直接依赖 |

## 七、交付成果统计

### 7.1 完成文件清单

|     |     |     |     |
| --- | --- | --- | --- |
| 文件路径 | 类型  | 代码行数 | 备注  |
| src/stores/aiStore.ts | 状态管理 | 180 | 核心状态与方法 |
| src/api/aiApi.ts | 接口封装 | 120 | 5 个核心接口封装 |
| src/components/ai/AiChatPanel.vue | 核心组件 | 280 | 流式对话、Markdown 渲染 |
| src/components/ai/ModelSelector.vue | 功能组件 | 60  | 模型切换 |
| src/components/ai/ExpandNodeDialog.vue | 功能组件 | 150 | 节点扩展 |
| src/components/ai/SummarizePanel.vue | 功能组件 | 120 | 分支摘要 |
| src/components/ai/SemanticSearchBar.vue | 功能组件 | 100 | 语义搜索 |
| **总计** | \-  | **1010** | 不含注释、空行 |

### 7.2 核心流程验证结果

|     |     |     |
| --- | --- | --- |
| 流程名称 | 验证状态 | 备注  |
| AI 流式对话 | ✅ 完成 | 支持 Markdown 渲染、代码高亮，无卡顿 |
| 节点扩展 | ✅ 完成 | 生成子节点实时添加至画布，高亮定位 |
| 分支摘要 | ✅ 完成 | 支持添加子节点 / 替换节点，画布同步更新 |
| 语义搜索 | ✅ 完成 | 搜索结果准确，定位高亮正常 |
| 右键菜单联动 | ✅ 完成 | 所有 AI 功能可通过右键菜单触发 |

## 八、功能演示材料

### 8.1 演示内容

1.  AI 模型切换（通义千问 → 文心一言）；
2.  AiChatPanel 流式对话（含 Markdown 代码块渲染）；
3.  右键菜单触发节点扩展，画布实时添加子节点；
4.  分支摘要生成与节点更新；
5.  语义搜索关键词，画布定位高亮节点。

### 8.2 演示形式

- 截图：每个核心功能的关键步骤截图（共 8-10 张）；
- GIF：核心流程动态演示（如流式输出、节点扩展完整流程）。

## 九、调用的 API 接口详细清单

### 9.1 接口概览

本次开发共封装 **5 个核心 API 接口**，覆盖 AI 模型管理、流式对话、节点操作、语义搜索四大场景，所有接口均遵循 RESTful 设计规范，支持 Token 权限验证。

### 9.2 接口详细说明

#### 9.2.1 获取 AI 模型列表

|     |     |
| --- | --- |
| 接口信息 | 具体内容 |
| 接口路径 | /ai/models |
| 请求方式 | GET |
| 功能描述 | 从后端动态获取支持的 AI 模型列表，用于模型选择器渲染 |
| 请求头 | Authorization: Bearer {token}（用户登录令牌） |
| 响应格式 | JSON |
| 成功响应示例 | \`\`\`json |
| {   |     |
| "code": 200, |     |
| "message": "success", |     |
| "data": \[ |     |
| {   |     |
| "id": "qwen-turbo", |     |
| "name": "通义千问 Turbo", |     |
| "desc": "阿里通义千问轻量版，响应速度快，适合日常对话与简单任务" |     |
| },  |     |
| {   |     |
| "id": "ernie-3.0", |     |
| "name": "文心一言 3.0", |     |
| "desc": "百度文心一言旗舰版，逻辑推理强，适合复杂文本处理" |     |
| },  |     |
| {   |     |
| "id": "deepseek-v3", |     |
| "name": "DeepSeek-V3", |     |
| "desc": "DeepSeek 开源模型，支持长文本处理" |     |
| }   |     |
| \]  |     |
| }   |     |

| 错误响应示例 | \`\`\`json

{

"code": 401,

"message": "token 失效，请重新登录"

}

| 调用场景 | ModelSelector.vue 组件挂载时、用户刷新页面时 |

| 技术特性 | 接口缓存：前端缓存 5 分钟，避免频繁请求 |

#### 9.2.2 流式 AI 对话接口

|     |     |
| --- | --- |
| 接口信息 | 具体内容 |
| 接口路径 | /ai/chat/stream |
| 请求方式 | POST |
| 功能描述 | 基于选中的 AI 模型，实现流式对话响应，支持绑定思维导图节点 ID（聚焦操作） |
| 请求头 | Authorization: Bearer {token}、Content-Type: application/json |
| 请求体参数 | \`\`\`json |
| {   |     |
| "model": "qwen-turbo", // 模型 ID（必填） |     |
| "content": "请扩展这个节点的内容", // 对话内容（必填） |     |
| "nodeId": "node-123456" // 目标节点 ID（可选，用于聚焦操作） |     |
| }   |     |

| 响应格式 | 流式文本（Stream） |

| 响应特点 | 采用 SSE（Server-Sent Events）技术，逐字返回响应内容，前端实时渲染 |

| 错误处理 | 流式过程中发生错误时，返回以 \`ERROR:\` 开头的文本，前端捕获后提示用户 |

| 调用场景 | AiChatPanel.vue 组件发送对话请求时 |

| 技术特性 | 支持上下文关联（后端自动携带历史对话）、断点续传（网络中断后可恢复） |

\#### 9.2.3 节点扩展接口

| 接口信息 | 具体内容 |

|------------------|--------------------------------------------------------------------------|

| 接口路径 | \`/ai/node/expand\` |

| 请求方式 | POST |

| 功能描述 | 基于目标节点，根据提示词生成子节点数据，返回后前端同步至画布 |

| 请求头 | \`Authorization: Bearer {token}\`、\`Content-Type: application/json\` |

| 请求体参数 | \`\`\`json

{

"nodeId": "node-123456", // 目标节点ID（必填）

"model": "ernie-3.0", // 模型ID（必填）

"prompt": "扩展该节点的核心知识点，分3点说明" // 扩展提示词（必填）

}

| 成功响应示例 | \`\`\`json

{

"code": 200,

"message": "success",

"data": {

"childrenNodes": \[

{

"id": "node-789012",

"text": "知识点 1：XXX",

"data": {},

"position": {"x": 100, "y": 200}

},

{

"id": "node-789013",

"text": "知识点 2：XXX",

"data": {},

"position": { "x": 100, "y": 300 }

}

\]

}

}

| 调用场景 | ExpandNodeDialog.vue 组件点击“扩展节点”时 |

| 约束条件 | 目标节点必须存在，提示词长度不超过 500 字 |

\#### 9.2.4 分支摘要接口

| 接口信息 | 具体内容 |

|------------------|--------------------------------------------------------------------------|

| 接口路径 | \`/ai/node/summarize\` |

| 请求方式 | POST |

| 功能描述 | 分析目标节点及其子节点的内容，生成简洁摘要 |

| 请求头 | \`Authorization: Bearer {token}\`、\`Content-Type: application/json\` |

| 请求体参数 | \`\`\`json

{

"nodeId": "node-123456", // 分支根节点ID（必填）

"model": "qwen-turbo" // 模型ID（必填）

}

| 成功响应示例 | \`\`\`json

{

"code": 200,

"message": "success",

"data": {

"summary": "该分支主要介绍了 AI 功能面板的核心组件，包括对话面板、模型选择器、节点扩展弹窗等，支持流式交互与画布实时联动。"

}

}

| 调用场景 | SummarizePanel.vue 组件点击“生成摘要”时 |

| 技术特性 | 后端自动递归获取分支下所有节点内容，摘要长度控制在 100-300 字之间 |

\#### 9.2.5 语义搜索接口

| 接口信息 | 具体内容 |

|------------------|--------------------------------------------------------------------------|

| 接口路径 | \`/ai/search/semantic\` |

| 请求方式 | GET |

| 功能描述 | 根据关键词进行语义匹配，返回思维导图中相关的节点列表 |

| 请求头 | \`Authorization: Bearer {token}\` |

| 请求参数（Query）| \`keyword=AI交互\`（搜索关键词，必填）、\`mapId=map-789\`（思维导图ID，必填） |

| 成功响应示例 | \`\`\`json

{

"code": 200,

"message": "success",

"data": \[

{

"nodeId": "node-123456",

"content": "AI 对话面板支持流式 Markdown 输出",

"score": 0.92 // 相关性评分（0-1）

},

{

"nodeId": "node-456789",

"content": "模型选择器支持多模型动态切换",

"score": 0.85

}

\]

}

| 调用场景 | SemanticSearchBar.vue 组件输入关键词并触发搜索时 |

| 搜索范围 | 当前思维导图中所有节点的文本内容（含节点标题、备注） |

### 9.3 接口调用统计

|     |     |     |
| --- | --- | --- |
| 接口名称 | 开发阶段调用次数 | 核心组件调用关联 |
| 获取 AI 模型列表 | 约 50 次 | ModelSelector.vue、aiStore.ts |
| 流式 AI 对话 | 约 200 次 | AiChatPanel.vue、aiStore.ts |
| 节点扩展 | 约 120 次 | ExpandNodeDialog.vue |
| 分支摘要 | 约 80 次 | SummarizePanel.vue |
| 语义搜索 | 约 60 次 | SemanticSearchBar.vue、aiStore.ts |

## 十、总结与后续优化

### 10.1 开发总结

本次开发完成了 AI 功能面板的全部核心需求，实现了流式交互、多模型支持、画布联动等关键功能，与成员 B 协作顺畅，交付成果符合项目要求。开发过程中重点解决了流式 Markdown 渲染、状态同步、跨组件通信等技术难点，代码质量和交互体验均达到预期。

### 10.2 后续优化方向

1.  增强对话上下文管理：支持上下文保留 / 截断，优化多轮对话体验；
2.  优化流式输出效果：支持打字机速度调节，添加暂停 / 继续功能；
3.  扩展 AI 能力：新增节点翻译、思维导图自动规整、内容生成模板等功能；
4.  性能优化：大模型响应较慢时添加预加载动画，实现接口请求缓存；
5.  兼容性优化：适配更多浏览器，完善移动端适配。

## 十一、附件清单

1.  核心组件功能演示截图 / GIF；
2.  代码仓库地址（Gitee：feature/c 分支）；
3.  接口文档（api-documentation.md）；
4.  前端优化计划（frontend-optimization-plan.md）。

（注：文档部分内容可能由 AI 生成）