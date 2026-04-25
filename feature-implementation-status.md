# Freeplane AI 插件功能实现状态文档

> 生成时间：2026-04-25  
> 后端服务：`freeplane_plugin_ai`（REST API 端口 6299）  
> 新前端：`freeplane_web`（Vue3 + VueFlow）  
> 旧前端：`freeplane_plugin_ai/frontend`（Vue3，内嵌 WebView）

---

## 一、后端已实现的全部 API

### 1.1 模型管理

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/ai/chat/models` | GET | 返回当前配置下可用的 AI 模型列表（支持 OpenRouter / Gemini / DashScope(Qwen) / ERNIE(Baidu) / Ollama） |

### 1.2 对话功能

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/ai/chat/message` | POST | AI 同步对话，支持传入 `modelSelection`、`mapId`、`selectedNodeId`，返回 `reply` 和 `tokenUsage` |
| `/api/ai/chat/smart` | POST | 智能缓冲层入口，自然语言输入自动路由到 MindMapBufferLayer（思维导图相关请求）或普通 Chat 服务 |

### 1.3 思维导图构建功能（Build 区）

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/ai/build/generate-mindmap` | POST | 一键生成完整思维导图；参数：`topic`（主题）、`maxDepth`（层数）、`modelSelection` |
| `/api/ai/build/expand-node` | POST | 展开节点，AI 生成子节点；参数：`nodeId`、`count`（数量）、`depth`（展开深度）、`focus`（关注方向） |
| `/api/ai/build/summarize` | POST | 分支摘要，对指定节点的整个子树内容生成摘要；参数：`nodeId`、`maxWords`、`writeToNote`（写入备注） |
| `/api/ai/build/tag` | POST | 批量自动标签，对节点提取关键词标签；参数：`nodeIds`（数组） |

### 1.4 地图与节点管理 API（非 AI）

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/map/current` | GET | 获取当前打开的思维导图结构 |
| `/api/maps` | GET | 获取所有打开的导图列表 |
| `/api/maps/{mapId}` | GET | 获取指定导图 |
| `/api/maps/create` | POST | 新建导图 |
| `/api/maps/switch` | POST | 切换当前导图 |
| `/api/nodes/{nodeId}` | GET | 获取指定节点 |
| `/api/nodes/search` | POST | 关键词搜索节点 |
| `/api/nodes/create` | POST | 创建新节点 |
| `/api/nodes/edit` | POST | 编辑节点文本 |
| `/api/nodes/delete` | POST | 删除节点 |
| `/api/nodes/toggle-fold` | POST | 折叠/展开节点 |

### 1.5 模型配置

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/ai/config/save` | POST | 保存 AI 模型配置（Provider / API Key / Base URL / Model Name） |

---

## 二、新前端（freeplane_web）已实现的功能

### 2.1 思维导图画布

| 功能 | 组件 | 说明 |
|---|---|---|
| 思维导图可视化渲染 | `MindMapCanvas.vue` | 使用 VueFlow 渲染，支持缩放、平移、背景点阵 |
| 节点拖拽 | `MindMapCanvas.vue` | `nodes-draggable="true"` |
| 双击编辑节点 | `NodeEditPanel.vue` | 双击节点弹出编辑面板 |
| 右键上下文菜单 | `NodeContextMenu.vue` | 包含：编辑节点、新建子节点、删除节点、AI 展开节点、AI 分支摘要 |
| 节点折叠/展开 | `ActionModal.vue` | 通过操作模态框确认后折叠/展开 |

### 2.2 工具栏

| 功能 | 说明 |
|---|---|
| 适应画布（fitView） | 一键让所有节点适应视口 |
| 居中导图 | 将视口中心移到原点 |
| 刷新导图 | 重新从后端拉取导图数据 |
| 切换 AI 面板 | 显示/隐藏 AI 功能侧边栏 |
| AI 模式切换 | Auto / Chat / Build 三模式切换 |
| 导出为 `.mm` | Freeplane 原生格式导出 |
| 导出为 JSON | JSON 备份导出 |
| 节点搜索 | 输入关键词搜索节点并定位 |

### 2.3 AI Auto 模式（`AiAutoPanel.vue`）

| 功能 | 说明 |
|---|---|
| 自然语言智能路由 | 输入自然语言，自动判断是对话还是思维导图操作（`/api/ai/chat/smart`） |
| 结果预览 | 展示 AI 返回结果 |
| 面板宽度拖拽调整 | 左边缘可拖拽改变侧边栏宽度 |

### 2.4 AI Chat 模式（`AiChatPanel.vue`）

| 功能 | 说明 |
|---|---|
| AI 对话 | 发送消息调用 `/api/ai/chat/message`，逐字打字机效果展示 |
| 消息历史 | 显示完整对话记录，区分用户/AI 角色 |
| Markdown 渲染 | AI 回复支持 Markdown 格式渲染 |
| 打字机流式效果 | 前端模拟逐字显示动画 |

### 2.5 AI Build 模式（`AiBuildPanel.vue`）

| 功能 | 说明 |
|---|---|
| 一键生成思维导图 | 输入主题 + 深度，调用 `/api/ai/build/generate-mindmap`，直接写入 Freeplane 画布 |
| 展开节点 | 选中节点，设置数量，调用 `/api/ai/build/expand-node` |
| 分支摘要 | 选中节点，调用 `/api/ai/build/summarize`，结果写入节点备注 |
| 自动标签 | 选中节点，调用 `/api/ai/build/tag`，批量打标签 |
| 结果预览 & 应用 | "AI 建议已生成，是否应用？" 确认后应用到画布 |

### 2.6 模型配置（`ModelConfigPanel.vue`）

| 功能 | 说明 |
|---|---|
| Provider 选择 | 支持 OpenRouter / Gemini / DashScope / ERNIE / Ollama / 自定义 |
| API Key 配置 | 在前端填写并保存到 Freeplane 配置文件 |
| Base URL 配置 | 可选，用于私有部署或代理 |
| 模型名配置 | 可选，默认各 Provider 的推荐模型 |

---

## 三、老前端有、新前端缺失的功能

> 老前端：`freeplane_plugin_ai/frontend`（仅 5 个组件）  
> 对比新前端，以下是**老前端的原生 Freeplane 能力，新前端尚未实现**的部分：

### 3.1 节点样式操作（Freeplane 原生能力）

| 功能 | 老 Freeplane 桌面端有 | 新前端状态 | 说明 |
|---|---|---|---|
| 节点颜色（背景色） | ✅ | ❌ 未实现 | 需调用 `/api/nodes/style` 类接口，后端亦未暴露 |
| 节点字体颜色 | ✅ | ❌ 未实现 | 同上 |
| 节点字体大小 | ✅ | ❌ 未实现 | 同上 |
| 节点字体加粗/斜体 | ✅ | ❌ 未实现 | 同上 |
| 节点形状（气泡/方框/圆形） | ✅ | ❌ 未实现 | 同上 |
| 节点边框样式 | ✅ | ❌ 未实现 | 同上 |
| 节点图标（内置表情/图标集） | ✅ | ❌ 未实现 | 同上 |

### 3.2 连接线/关系

| 功能 | 老 Freeplane 桌面端有 | 新前端状态 | 说明 |
|---|---|---|---|
| 节点间箭头/关系连线 | ✅ | ❌ 未实现 | 非父子关系的跨节点连线 |
| 连线标签 | ✅ | ❌ 未实现 | 连线上显示关系说明文字 |
| 连线颜色/样式 | ✅ | ❌ 未实现 | - |

### 3.3 节点内容增强

| 功能 | 老 Freeplane 桌面端有 | 新前端状态 | 说明 |
|---|---|---|---|
| 节点备注（Note） | ✅ | ❌ 未渲染 | 后端 summarize 可写入备注，但前端不显示 |
| 节点超链接 | ✅ | ❌ 未实现 | - |
| 节点附件 | ✅ | ❌ 未实现 | - |
| 富文本节点（HTML） | ✅ | ❌ 未实现 | - |

### 3.4 导图操作

| 功能 | 老 Freeplane 桌面端有 | 新前端状态 | 说明 |
|---|---|---|---|
| 多导图标签页切换 | ✅ | ❌ 未实现 | 后端已有 `/api/maps/switch`，前端未接入 |
| 新建导图 | ✅ | ❌ 未实现 | 后端已有 `/api/maps/create`，前端未接入 |
| 撤销/重做 | ✅ | ❌ 未实现 | - |
| 复制/粘贴节点 | ✅ | ❌ 未实现 | - |
| 节点排序 | ✅ | ❌ 未实现 | - |

### 3.5 AI 语义搜索（老前端有，新前端未接入）

| 功能 | 老前端 | 新前端状态 | 说明 |
|---|---|---|---|
| 语义搜索节点 | `SemanticSearchBar.vue` ✅ | ❌ 新前端只有关键词搜索 | 后端已有 `/api/ai/build/search`，新前端未调用 |

---

## 四、需优先实现的缺失功能（建议优先级）

| 优先级 | 功能 | 涉及接口 | 涉及组件 |
|---|---|---|---|
| P0 | 节点备注（Note）显示 | 后端需在节点数据中带 `note` 字段 | `MindMapCanvas.vue` |
| P0 | 多导图切换 | `/api/maps`、`/api/maps/switch` | 新增 `MapTabBar.vue` |
| P1 | 语义搜索 | `/api/ai/build/search` | 新增或复用 `SemanticSearchBar.vue` |
| P1 | 节点颜色/字体颜色 | 后端需新增 `/api/nodes/style` 接口 | `NodeContextMenu.vue`、`NodeEditPanel.vue` |
| P2 | 节点图标 | 后端需暴露图标读写 | 工具栏或右键菜单 |
| P2 | 撤销/重做 | 后端或前端本地状态 | 工具栏 |
| P3 | 节点间关系连线 | 后端需暴露箭头/关系 API | `MindMapCanvas.vue` |

---

## 五、架构说明

```
浏览器（freeplane_web: Vue3 + VueFlow）
    ↓ HTTP (端口 6299)
freeplane_plugin_ai REST API（HttpServer）
    ├── /api/ai/chat/smart   → BufferLayerRouter → MindMapBufferLayer (AI生成)
    ├── /api/ai/build/*      → DefaultAgentService (直接AI调用)
    ├── /api/ai/chat/message → DefaultChatService (对话)
    ├── /api/maps/*          → MapRestController (导图管理)
    └── /api/nodes/*         → NodeRestController (节点管理)
    ↓
Freeplane 原生 Java API（MMapController / NodeModel / MapModel）
    ↓
.mm 文件（Freeplane 思维导图格式）
```
