# Freeplane Web - 智能思维导图编辑器

> 基于 Freeplane 开源项目的现代化 Web 版本，集成 AI 智能辅助功能

[![License](https://img.shields.io/badge/License-GPL%202.0-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7-blue.svg)](https://www.typescriptlang.org/)

---

## 📖 项目概述

**Freeplane Web** 是一款基于开源思维导图编辑器 [Freeplane](https://www.freeplane.org/) 开发的现代化 Web 应用。项目在保留 Freeplane 核心功能的基础上，进行了全面的技术栈升级和功能增强：

- 🤖 **AI 智能集成**：基于 LangChain4j 的多模型 AI 对话、节点智能扩展、分支摘要生成
- 🌐 **前后端分离架构**：REST API 桥接层，支持 Web 端思维导图编辑
- 🎨 **现代化 UI**：基于 Vue 3 + TypeScript + Vue Flow 的响应式界面
- ⚡ **实时交互**：流式 SSE 输出、打字机效果、智能状态同步
- 🔌 **可扩展插件系统**：继承 Freeplane OSGi 插件架构，支持功能模块化扩展

本项目旨在为用户提供更加便捷、智能的思维导图编辑体验，同时保持与原始 Freeplane 项目的数据格式兼容性（`.mm` 格式）。

---

## 🔗 源项目信息

### 原始项目

- **项目名称**：Freeplane
- **官方网站**：https://www.freeplane.org/
- **GitHub 仓库**：https://github.com/freeplane/freeplane
- **SourceForge**：https://sourceforge.net/projects/freeplane/
- **开源协议**：GPL-2.0-or-later
- **当前版本**：1.13.x

### 项目继承关系

```
Freeplane 1.13.x (原始桌面版)
    ↓
    ├─ 继承核心数据模型 (MapModel, NodeModel)
    ├─ 继承 OSGi 插件架构
    ├─ 继承文件格式 (.mm XML)
    └─ 继承多语言支持 (i18n)
    
Freeplane Web (本项目)
    ↓
    ├─ 新增 REST API 桥接层
    ├─ 新增 Vue 3 Web 前端
    ├─ 新增 AI 智能辅助功能
    └─ 新增现代化交互体验
```

---

## 🛠️ 技术栈说明

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | 3.5.13 | 前端框架，Composition API + `<script setup>` |
| **TypeScript** | 5.7.3 | 类型安全的 JavaScript 超集 |
| **Vite** | 6.2.0 | 现代化前端构建工具 |
| **Pinia** | 2.3.1 | Vue 3 官方状态管理库 |
| **Vue Flow** | 1.41.3 | 基于 Vue 3 的流程图/思维导图渲染库 |
| **Vue Router** | 4.5.0 | 前端路由管理 |
| **Axios** | 1.7.9 | HTTP 客户端，API 调用 |

**前端核心依赖**：
```json
{
  "@vue-flow/core": "^1.41.3",
  "@vue-flow/background": "^1.3.2",
  "@vue-flow/controls": "^1.1.2",
  "pinia": "^2.3.1",
  "vue": "^3.5.13",
  "vue-router": "^4.5.0"
}
```

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 8 (核心) / 17 (AI插件) | 主要开发语言 |
| **OSGi** | Knopflerfish 8.0.11 | 模块化插件容器 |
| **LangChain4j** | 1.10.0 | Java AI 框架，支持多模型 |
| **Groovy** | 4.0.27 | 脚本引擎支持 |
| **JDK HTTP Server** | 内置 | 轻量级 REST API 服务器 |
| **JUnit 4** | - | 单元测试框架 |
| **AssertJ + Mockito** | - | 断言和 Mock 测试 |

**AI 模型支持**：
- OpenRouter（GPT-4o、Claude 3.5、Llama 3 等数百个模型）
- Google Gemini（gemini-2.0-flash、gemini-3-* 等）
- Ollama（本地部署模型）

### 构建工具与开发环境

| 工具 | 用途 |
|------|------|
| **Gradle** | 多模块项目构建工具 |
| **biz.aQute.bnd** | OSGi Bundle 清单自动生成 |
| **native2ascii** | 国际化文件编码转换 |

**开发环境要求**：
- Java：`~/.sdkman/candidates/java/21.0.5-zulu`（推荐）或 Java 8/11-23
- Node.js：16+（前端开发）
- Gradle：项目内置 Wrapper

---

## ✨ 主要功能特性

### 🧠 思维导图可视化

- **树形结构渲染**：基于 Vue Flow 的交互式思维导图展示
- **节点样式定制**：支持图标、颜色、字体、边框等样式设置
- **折叠/展开**：智能节点折叠，支持逐级展开
- **拖拽操作**：直观的节点位置调整
- **缩放和平移**：画布自由缩放和平移浏览

### 📝 节点 CRUD 操作

- **创建节点**：Tab 键快速创建子节点，Enter 键创建兄弟节点
- **编辑节点**：双击节点进入编辑模式，支持富文本
- **删除节点**：支持单个/批量删除，带撤销功能
- **移动节点**：拖拽调整节点层级关系
- **节点属性**：支持键值对扩展数据

### 🤖 AI 集成功能

#### AI 对话
- 多模型切换（OpenRouter/Gemini/Ollama）
- 流式 SSE 输出，打字机效果
- 上下文感知，支持节点引用
- 对话历史管理

#### 节点智能扩展
- 选中节点，AI 自动生成子节点
- 支持指定扩展数量和深度
- 上下文感知生成（父节点、兄弟节点参考）
- 质量验证（MECE 原则、命名规范）

#### 分支摘要
- 自动生成节点分支内容摘要
- 流式输出，实时显示进度
- Markdown 格式化输出
- 画布联动高亮

#### 语义搜索
- 自然语言搜索节点内容
- 智能匹配相关节点
- 搜索结果高亮显示

#### 智能路由
- **Chat 模式**：纯对话交互
- **Build 模式**：AI 工具调用（节点操作）
- **Auto 模式**：自动识别意图，智能选择模式

### 🔄 实时协作和状态同步

- **状态管理**：Pinia 统一状态管理
- **画布联动**：AI 操作结果实时反映到画布
- **消息通信**：`postMessage` 跨组件通信
- **错误处理**：完善的错误提示和降级策略

### 💡 智能交互功能

- **快捷键支持**：Tab、Enter、Delete 等常用操作
- **右键菜单**：节点操作快捷入口
- **智能提示**：输入建议和自动补全
- **响应式设计**：适配不同屏幕尺寸

---

## 🚀 相对于原项目的改进

### 功能增强对比

| 功能 | 原始 Freeplane | Freeplane Web |
|------|---------------|---------------|
| 界面类型 | Java Swing 桌面应用 | 现代化 Web 应用 |
| AI 集成 | ❌ 无 | ✅ LangChain4j 多模型支持 |
| 节点扩展 | 手动创建 | AI 智能生成子节点 |
| 内容摘要 | ❌ 无 | ✅ 自动分支摘要 |
| 语义搜索 | 关键字匹配 | AI 语义理解搜索 |
| 协作能力 | 本地单用户 | Web 架构支持多用户 |
| 跨平台 | 需安装客户端 | 浏览器即开即用 |
| 实时更新 | 本地保存 | 实时状态同步 |

### Web 前端界面优势

1. **零安装门槛**
   - 浏览器直接访问，无需下载客户端
   - 支持 Windows/macOS/Linux 全平台

2. **响应式设计**
   - 自适应桌面端和移动端
   - 触控友好的交互方式

3. **现代化用户体验**
   - 流畅的动画和过渡效果
   - 直观的拖拽操作
   - 实时预览和反馈

4. **易于集成**
   - REST API 支持第三方集成
   - 可嵌入其他 Web 应用
   - 支持自定义主题

### AI 功能创新实现

#### 1. 三层服务架构

```
用户请求
  ↓
┌─────────────────────┐
│   AiRestController  │  REST API 路由层
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│   AIService 接口    │  业务逻辑层
│  ├─ ChatService     │  对话服务
│  ├─ AgentService    │  工具调用服务
│  └─ AutoService     │  自动路由服务
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│   LangChain4j       │  AI 框架层
│  ├─ ChatModel       │  对话模型
│  ├─ ToolDefinition  │  工具定义
│  └─ AiServices      │  服务代理
└─────────────────────┘
```

#### 2. 智能降级策略

- **模型降级**：主模型不可用时自动切换到备用模型
- **功能降级**：AI 服务不可用时保留基础编辑功能
- **网络降级**：离线模式支持本地缓存操作

#### 3. 工具调用系统

AI 可调用的导图操作工具：
- `CreateNodesTool`：创建节点
- `EditNodeContentTool`：编辑节点内容
- `DeleteNodesTool`：删除节点
- `MoveNodesTool`：移动节点
- `ReadNodesTool`：读取节点信息
- `SearchNodesTool`：搜索节点
- `CreateSummaryTool`：创建摘要节点

### 用户体验改进

#### 交互方式优化
- **键盘导航**：完整的键盘快捷键支持
- **即时反馈**：操作结果实时显示
- **撤销/重做**：完整的操作历史管理
- **智能提示**：上下文感知的操作建议

#### 响应速度提升
- **流式输出**：SSE 实时推送，无需等待完整响应
- **懒加载**：大型导图按需加载节点
- **缓存策略**：智能缓存减少重复请求
- **异步处理**：后台任务不阻塞界面

#### 视觉设计改进
- **现代 UI**：Material Design 风格组件
- **主题切换**：支持浅色/深色模式
- **动画效果**：平滑的过渡和状态变化
- **可访问性**：符合 WCAG 2.1 标准

---

## 🏗️ 项目架构

### 前后端分离架构

```
┌──────────────────────────────────────────────┐
│              前端 (Port 5173)                 │
│  Vue 3 + TypeScript + Pinia + Vue Flow       │
│  ┌─────────────────────────────────────┐    │
│  │  MindMapCanvas.vue  (画布组件)      │    │
│  │  NodeContextMenu.vue (右键菜单)     │    │
│  │  ai/                                │    │
│  │   ├─ AiChatPanel.vue (对话面板)    │    │
│  │   ├─ ExpandNodeDialog.vue (扩展)   │    │
│  │   ├─ SummarizePanel.vue (摘要)     │    │
│  │   └─ SemanticSearchBar.vue (搜索)  │    │
│  └─────────────────────────────────────┘    │
└──────────────────┬───────────────────────────┘
                   │ HTTP / REST API
                   │ (Vite Proxy 开发环境)
                   ▼
┌──────────────────────────────────────────────┐
│         REST API 服务器 (Port 6299)           │
│  JDK HttpServer + CORS Filter                │
│  ┌─────────────────────────────────────┐    │
│  │  RestApiRouter (路由分发)           │    │
│  │  ├─ MapRestController (导图接口)   │    │
│  │  ├─ NodeRestController (节点接口)  │    │
│  │  └─ AiRestController (AI 接口)     │    │
│  └─────────────────────────────────────┘    │
└──────────────────┬───────────────────────────┘
                   │ 内部调用
                   ▼
┌──────────────────────────────────────────────┐
│       Freeplane 核心 (OSGi Bundle)            │
│  ┌─────────────────────────────────────┐    │
│  │  MapModel / NodeModel (数据模型)    │    │
│  │  MapController (业务逻辑)           │    │
│  │  freeplane_plugin_ai (AI 插件)      │    │
│  │   ├─ AIChatService (对话服务)      │    │
│  │   ├─ AgentService (工具调用)       │    │
│  │   └─ MCP Server (外部集成)         │    │
│  └─────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

### REST API 桥接层设计

#### API 路由分类

| 路由前缀 | 功能 | 示例接口 |
|---------|------|---------|
| `/api/maps` | 导图管理 | `GET /api/maps` 获取导图列表 |
| `/api/map/current` | 当前导图 | `GET /api/map/current` 获取当前导图数据 |
| `/api/node/**` | 节点操作 | `POST /api/node/create` 创建节点 |
| `/api/ai/**` | AI 功能 | `POST /api/ai/chat/stream` 流式对话 |

#### AI 接口详解

```
POST /api/ai/chat/message        # 普通对话
POST /api/ai/chat/stream         # 流式对话 (SSE)
GET  /api/ai/chat/models         # 获取可用模型列表
POST /api/ai/build/generate-mindmap  # AI 生成思维导图
POST /api/ai/build/expand-node   # AI 展开节点
POST /api/ai/build/summarize     # 分支摘要 (同步)
POST /api/ai/build/summarize-stream  # 分支摘要 (流式)
POST /api/ai/build/search        # 语义搜索
POST /api/ai/build/tag           # 自动标签
POST /api/ai/chat/smart          # 智能路由 (Auto 模式)
```

#### 数据流示例：AI 节点扩展

```
1. 用户选中节点，点击"AI 展开"
   ↓
2. 前端发送 POST /api/ai/build/expand-node
   {
     "nodeId": "node_123",
     "mapId": "map_456",
     "depth": 2,
     "count": 5,
     "focus": "应用场景"
   }
   ↓
3. AiRestController 接收请求
   ↓
4. DefaultAgentService.handleExpandNode()
   ├─ 读取节点上下文（父节点、兄弟节点）
   ├─ 构建 Prompt（6步展开流程）
   ├─ 调用 AI 模型生成子节点内容
   └─ 验证生成结果（MECE 原则）
   ↓
5. 返回 JSON 结果
   {
     "success": true,
     "data": {
       "nodeId": "node_123",
       "result": "[{\"text\":\"场景1\",...},...]",
       "tokenUsage": {"inputTokens": 150, "outputTokens": 320}
     }
   }
   ↓
6. 前端解析结果，通过 Vue Flow API 创建子节点
   ↓
7. 画布更新，显示新生成的节点
```

---

## 📂 项目结构

```
freeplane-1.13.x/
├── 📦 核心模块
│   ├── freeplane/                  # 应用核心（地图模型、UI、特性）
│   ├── freeplane_api/              # 公共 Java API（脚本/外部集成接口）
│   └── freeplane_framework/        # 启动器 + OSGi 容器入口
│
├── 🤖 AI 与插件
│   ├── freeplane_plugin_ai/        # AI 对话 & 工具调用插件 ⭐
│   │   ├── src/main/java/          # Java 后端实现
│   │   │   ├── chat/               # 对话子系统
│   │   │   ├── tools/              # AI 工具集
│   │   │   ├── service/            # AIService 三层架构
│   │   │   └── restapi/            # REST API 控制器
│   │   └── frontend/               # AI 前端组件（待迁移）
│   ├── freeplane_plugin_script/    # Groovy 脚本引擎插件
│   ├── freeplane_plugin_formula/   # 公式计算插件
│   ├── freeplane_plugin_latex/     # LaTeX 渲染插件
│   └── freeplane_plugin_markdown/  # Markdown 渲染插件
│
├── 🌐 Web 前端
│   └── freeplane_web/              # Vue3 + TypeScript 前端应用 ⭐
│       ├── src/
│       │   ├── api/                # API 调用层
│       │   │   ├── mapApi.ts       # 导图数据接口
│       │   │   ├── nodeApi.ts      # 节点操作接口
│       │   │   └── aiApi.ts        # AI 接口封装
│       │   ├── components/         # Vue 组件
│       │   │   ├── MindMapCanvas.vue
│       │   │   ├── NodeContextMenu.vue
│       │   │   └── ai/             # AI 组件
│       │   ├── stores/             # Pinia 状态管理
│       │   │   ├── mapStore.ts
│       │   │   └── aiStore.ts
│       │   ├── types/              # TypeScript 类型定义
│       │   └── utils/              # 工具函数
│       ├── package.json
│       ├── vite.config.ts
│       └── tsconfig.json
│
├── 🛠️ 辅助模块
│   ├── freeplane_ant/              # Ant 构建辅助任务
│   ├── freeplane_debughelper/      # 开发调试配置
│   ├── freeplane_mac/              # macOS 平台适配
│   └── JOrtho_0.4_freeplane/       # 拼写检查库
│
├── 📄 构建配置
│   ├── build.gradle                # 根构建配置（OSGi/BND/版本统一）
│   ├── settings.gradle             # 子模块注册
│   ├── dist.gradle                 # 发行版打包入口
│   ├── win.dist.gradle             # Windows 安装包
│   ├── mac.dist.gradle             # macOS 应用包
│   └── linux-packages.gradle       # Linux DEB/RPM 包
│
└── 📚 文档
    ├── docs/                       # 项目文档
    └── ai-specs/                   # AI 功能规格与任务
```

---

## 🚀 快速开始

### 环境准备

1. **安装 Java**
   ```bash
   # 推荐使用 SDKMAN 安装 Java 21
   sdk install java 21.0.5-zulu
   ```

2. **安装 Node.js**（16+）
   ```bash
   # 使用 nvm 安装
   nvm install 18
   nvm use 18
   ```

3. **安装 Gradle**（可选，项目内置 Wrapper）
   ```bash
   sdk install gradle
   ```

### 构建项目

```bash
# 克隆项目
git clone https://gitee.com/zm050329/freeplane-recreating.git
cd freeplane-recreating

# 编译所有模块
gradle build

# 或仅编译核心模块
gradle :freeplane:compileJava
```

### 启动服务

#### 1. 启动 Freeplane 后端

```bash
# Windows
BIN\freeplane.bat

# Unix/macOS
BIN/freeplane.sh
```

后端启动后会加载 OSGi 插件，自动启动：
- **MCP Server**：端口 6298（外部 AI 客户端调用）
- **REST API Server**：端口 6299（Web 前端调用）

#### 2. 启动前端开发服务器

```bash
cd freeplane_web
npm install
npm run dev
```

前端访问地址：http://localhost:5173

#### 3. 配置 AI 模型

在 Freeplane 桌面端中：
1. 打开 **工具 → 偏好设置 → AI Chat**
2. 配置至少一个 Provider 的 API Key：
   - **OpenRouter**：从 [openrouter.ai](https://openrouter.ai) 获取
   - **Google Gemini**：从 [Google AI Studio](https://aistudio.google.com) 获取
   - **Ollama**：本地服务地址（默认 `http://localhost:11434`）

---

## 🔧 开发指南

### 前端开发

```bash
# 开发模式（热重载）
npm run dev

# 类型检查 + 构建
npm run build

# 预览生产构建
npm run preview
```

### 后端开发

```bash
# 运行测试
gradle test

# 详细日志输出
gradle test -PTestLoggingFull

# 编译 AI 插件
gradle :freeplane_plugin_ai:build
```

### 常用构建命令

```bash
# 打包发行版
gradle dist              # 所有平台
gradle win.dist          # Windows 安装包
gradle mac.dist          # macOS .app 包
gradle linux-packages    # DEB/RPM 包

# 翻译格式化（修改翻译文件后必须运行）
gradle format_translation
```

---

## 📊 端口说明

| 服务 | 端口 | 协议 | 用途 |
|------|------|------|------|
| **MCP Server** | 6298 | SSE + JSON-RPC | 外部 AI 客户端调用 |
| **REST API** | 6299 | HTTP/JSON | Web 前端调用 |
| **Vite Dev** | 5173 | HTTP | 前端开发服务器 |

---

## ❓ 常见问题

### 端口冲突

```bash
# Windows 查看端口占用
netstat -ano | findstr :6299

# 杀掉占用进程
taskkill /PID <PID> /F
```

### 跨域问题

- **开发环境**：Vite 代理自动处理（`vite.config.ts` 配置）
- **生产环境**：后端已实现 CORS 头（`CorsFilter`）

### AI 服务不可用

1. 检查 API Key 配置是否正确
2. 查看 Freeplane 日志输出
3. 验证网络连接（特别是 OpenRouter/Gemini）
4. 尝试切换到其他模型 Provider

---

## 📝 许可证

本项目继承 Freeplane 的 GPL-2.0-or-later 开源许可证。

- **原始项目**：[Freeplane](https://www.freeplane.org/) - GPL-2.0-or-later
- **本项目**：GPL-2.0-or-later

详细信息请参阅 [LICENSE](LICENSE) 文件。

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 📧 联系方式

- **项目仓库**：https://gitee.com/zm050329/freeplane-recreating
- **原始项目**：https://www.freeplane.org/
- **问题反馈**：提交 Issue 或联系开发团队

---

## 🙏 致谢

- **Freeplane 团队**：提供强大的开源思维导图引擎
- **LangChain4j**：优秀的 Java AI 框架
- **Vue 团队**：现代化的前端框架
- **所有贡献者**：感谢你们的付出

---

<div align="center">
  <p>Made with ❤️ by the Freeplane Web Team</p>
  <p>基于 <a href="https://www.freeplane.org/">Freeplane</a> 开源项目构建</p>
</div>
