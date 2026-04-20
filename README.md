# Freeplane 1.13.x — 源码架构说明

> Freeplane 是一款开源的思维导图编辑器，基于 Java/Swing + OSGi 插件框架构建，支持 Windows、macOS、Linux 三平台。

---

## 目录

- [技术栈概览](#技术栈概览)
- [模块结构](#模块结构)
- [核心模块详解](#核心模块详解)
  - [freeplane_framework — 启动层](#freeplane_framework--启动层)
  - [freeplane — 应用核心层](#freeplane--应用核心层)
  - [freeplane_api — 公共 API 层](#freeplane_api--公共-api-层)
- [插件模块详解](#插件模块详解)
  - [freeplane_plugin_ai — AI 插件](#freeplane_plugin_ai--ai-插件)
  - [freeplane_plugin_script — 脚本插件](#freeplane_plugin_script--脚本插件)
  - [freeplane_plugin_formula — 公式插件](#freeplane_plugin_formula--公式插件)
  - [freeplane_plugin_latex — LaTeX 插件](#freeplane_plugin_latex--latex-插件)
  - [freeplane_plugin_markdown — Markdown 插件](#freeplane_plugin_markdown--markdown-插件)
  - [freeplane_plugin_codeexplorer — 代码探索插件](#freeplane_plugin_codeexplorer--代码探索插件)
  - [其他插件](#其他插件)
- [辅助模块](#辅助模块)
- [构建系统](#构建系统)
- [运行方式](#运行方式)
- [测试](#测试)
- [国际化 (i18n)](#国际化-i18n)
- [项目版本](#项目版本)

---

## 技术栈概览

| 层次 | 技术 |
|------|------|
| 语言 | Java 8（编译目标），AI 插件使用 Java 17 |
| UI 框架 | Java Swing |
| 插件容器 | OSGi (Knopflerfish 8.0.11) |
| 构建工具 | Gradle（多模块项目） |
| AI 框架 | LangChain4j 1.10.0 |
| 脚本引擎 | Apache Groovy 4.0.27 |
| 测试框架 | JUnit 4 + AssertJ + Mockito |
| 文件格式 | `.mm`（FreeMind 兼容 XML 格式） |

---

## 模块结构

```
freeplane-1.13.x/
├── freeplane_framework/        # 启动器 + OSGi 容器入口
├── freeplane/                  # 应用核心（地图模型、UI、特性）
├── freeplane_api/              # 公共 Java API（脚本/外部集成接口）
├── freeplane_ant/              # Ant 构建辅助任务
├── freeplane_debughelper/      # 开发调试配置
├── freeplane_mac/              # macOS 平台适配
├── JOrtho_0.4_freeplane/       # 内嵌拼写检查库
│
├── freeplane_plugin_ai/        # AI 对话 & 工具调用插件
├── freeplane_plugin_script/    # Groovy 脚本引擎插件
├── freeplane_plugin_formula/   # 公式计算插件
├── freeplane_plugin_latex/     # LaTeX 渲染插件
├── freeplane_plugin_markdown/  # Markdown 渲染插件
├── freeplane_plugin_codeexplorer/ # Java 代码依赖分析插件
├── freeplane_plugin_bugreport/ # Bug 报告插件
├── freeplane_plugin_openmaps/  # OpenStreetMap 地图插件
├── freeplane_plugin_svg/       # SVG 渲染插件
├── freeplane_plugin_jsyntaxpane/ # 代码语法高亮组件
│
├── build.gradle                # 根构建配置（OSGi/BND/版本统一）
├── settings.gradle             # 子模块注册
├── dist.gradle                 # 发行版打包入口
├── bin.dist.gradle             # BIN 目录构建
├── win.dist.gradle             # Windows 安装包
├── mac.dist.gradle             # macOS 应用包
├── linux-packages.gradle       # Linux DEB/RPM 包
└── src.dist.gradle             # 源码压缩包
```

---

## 核心模块详解

### freeplane_framework — 启动层

**职责**：应用程序入口，初始化 OSGi 容器，引导插件加载。

关键类：

| 类 | 说明 |
|----|------|
| `org.freeplane.launcher.Launcher` | 主启动类，可嵌入外部 Java 程序（headless 或 GUI 模式） |
| `org.freeplane.launcher.Utils` | 路径/环境工具方法 |

启动流程：

```
Launcher.create()
  └─ 初始化 Knopflerfish OSGi 框架
       └─ 加载 freeplane bundle（核心）
            └─ 加载所有 freeplane_plugin_* bundles
                 └─ 各插件 Activator.start() 注册扩展点
```

脚本与配置：
- `freeplane_framework/script/freeplane.sh` / `freeplane.bat` — 启动脚本
- `freeplane_framework/launch4j/` — Windows EXE 包装配置
- `freeplane_framework/mac-appbundler/` — macOS .app 包配置

---

### freeplane — 应用核心层

**Bundle-SymbolicName**: `org.freeplane.core`

**依赖关系**：`freeplane_api`（实现其接口）

核心包结构：

```
org.freeplane.core/
├── resources/      # ResourceController：配置/属性/i18n 管理
├── ui/             # UITools、菜单系统、工具栏、选项面板
├── io/             # 文件读写基础设施
├── extension/      # IExtension：可插拔扩展点机制
├── util/           # LogUtils、TextUtils、HtmlUtils 等工具类
└── undo/           # 撤销/重做框架

org.freeplane.features/
├── map/            # 核心数据模型：MapModel、NodeModel、MapController
│   ├── NodeModel   # 节点数据（文本、子节点、ID、样式引用）
│   ├── MapModel    # 导图数据（根节点、元数据）
│   └── MMapController # MindMap 编辑控制器
├── text/           # 节点文本渲染与编辑（支持 HTML、纯文本）
├── styles/         # 节点样式、条件样式、主题
├── icon/           # 图标系统（内置图标 + 自定义图标）
├── attribute/      # 节点属性（键值对扩展数据）
├── link/           # 超链接与节点间连线（connectors）
├── filter/         # 筛选/过滤节点
├── note/           # 节点备注
├── encrypt/        # 节点加密
├── export/         # 导出框架（XSLT 基础）
├── print/          # 打印支持
└── mode/           # 模式系统（MindMap 模式 / 文件管理器模式）
    └── MModeController # 主编辑模式控制器

org.freeplane.main/
├── application/    # 主应用程序初始化、命令行解析
└── osgi/           # OSGi 服务接口（IModeControllerExtensionProvider 等）
```

**关键扩展点**：`IModeControllerExtensionProvider` — 各插件通过此接口在 MindMap 模式下注册功能。

---

### freeplane_api — 公共 API 层

**职责**：定义面向脚本开发者和外部集成的稳定 Java 接口，不包含实现。

核心接口：

| 接口/类 | 说明 |
|---------|------|
| `Node` / `NodeRO` | 节点读写 / 只读接口（文本、属性、图标、样式、子节点操作） |
| `MindMap` / `MindMapRO` | 导图读写 / 只读接口 |
| `Controller` / `ControllerRO` | 应用控制器（获取当前节点、打开/关闭文件等） |
| `Attributes` / `AttributesRO` | 节点属性操作 |
| `Icons` / `IconsRO` | 图标操作 |
| `Launcher` | 嵌入式启动（外部 Java 程序调用 Freeplane） |
| `HeadlessMapCreator` | 无 GUI 创建/编辑导图 |

**设计原则**：`*RO` 接口提供只读视图，`*` 接口继承并添加写操作，防止意外修改。

---

## 插件模块详解

所有插件均遵循相同的 OSGi 结构：
- `Activator.java` — 实现 `BundleActivator`，注册扩展
- `build.gradle` — 声明 `ext.bundleActivator`、`ext.bundleImports`、`ext.bundleExports`
- `src/main/resources/` — 偏好设置 XML、默认配置、图标资源

---

### freeplane_plugin_ai — AI 插件

**Bundle**: `org.freeplane.plugin.ai`  
**Java 版本**: Java 17（单独升级，其余模块为 Java 8）  
**依赖**: LangChain4j 1.10.0

#### 整体架构

```
freeplane_plugin_ai/
├── bootstrap/          # Java 8 兼容引导层（Java8BootstrapActivator）
│                       # 负责检测 Java 版本，Java 17+ 才加载主模块
└── main/
    ├── Activator.java  # 插件入口：注册聊天面板 + MCP 服务 + AI 编辑功能
    ├── chat/           # 聊天对话子系统
    ├── tools/          # AI 可调用的导图操作工具集
    ├── edits/          # AI 编辑标记（高亮已 AI 修改的节点）
    ├── maps/           # 多导图访问封装
    └── mcpserver/      # Model Context Protocol (MCP) 服务器
```

#### chat/ — 对话子系统

```
chat/
├── AIChatPanel.java              # 主 UI 面板（Swing，嵌入右侧标签页）
├── AIChatService.java            # LangChain4j AiServices 封装，执行 chat()
├── AIChatModelFactory.java       # 根据 provider 名称创建 ChatModel 实例
│                                 #   支持: openrouter / gemini / ollama
├── AIProviderConfiguration.java  # 从 ResourceController 读取 API Key / 地址
├── AIModelCatalog.java           # 从远端 API 获取并缓存可用模型列表
├── AIModelDescriptor.java        # 模型描述（provider + modelName + displayName）
├── AIModelSelection.java         # 选中模型的序列化值（"provider:modelName"）
├── AssistantProfile.java         # 助手配置文件（系统指令 + 记忆设置）
├── AssistantProfileChatMemory.java # 聊天记忆管理（token 上限驱逐策略）
├── ChatRequestFlow.java          # 请求生命周期（开始/取消/结束/快照回滚）
├── ChatTokenUsageTracker.java    # Token 用量统计
├── LiveChatController.java       # 当前活跃对话的控制器
└── history/                      # 对话历史持久化
    └── ChatTranscriptStore.java  # 对话记录的 JSON 序列化存储
```

**模型 Provider 支持**：

| Provider | API 路由 | 支持模型示例 |
|----------|---------|------------|
| `openrouter` | `https://openrouter.ai/api/v1`（可自定义） | GPT-4o、Claude 3.5、Llama 3 等数百个模型 |
| `gemini` | Google AI Gemini API | gemini-2.0-flash、gemini-3-* |
| `ollama` | 本地 Ollama 服务（可自定义地址） | 任意本地模型 |

#### tools/ — 导图操作工具集

AI 通过 LangChain4j `@Tool` 注解调用这些工具，直接操作思维导图：

```
tools/
├── AIToolSet.java          # 所有 @Tool 方法的容器类（注册到 AiServices）
├── AIToolSetBuilder.java   # AIToolSet 的构造器（依赖注入）
├── create/                 # 创建节点：CreateNodesTool
├── edit/                   # 编辑节点内容：文本/图标/属性/标签/样式/超链接
├── delete/                 # 删除节点：DeleteNodesTool
├── move/                   # 移动节点/创建摘要节点：MoveNodesTool、CreateSummaryTool
├── read/                   # 读取节点内容：ReadNodesWithDescendantsTool
├── search/                 # 搜索节点：SearchNodesTool（关键字/正则匹配）
├── selection/              # 获取/设置选中节点
├── content/                # 节点内容读写抽象（文本/属性/图标/标签/样式）
└── utilities/              # ToolCallSummary、ToolCaller、ToolExecutorFactory
```

#### mcpserver/ — MCP 服务器

将导图操作工具通过 **Model Context Protocol** 暴露为 HTTP 服务，供外部 AI 客户端（Claude Desktop、Cursor 等）调用：

```
mcpserver/
├── ModelContextProtocolServer.java     # HTTP 服务器（SSE + JSON-RPC 2.0）
├── ModelContextProtocolToolRegistry.java # 工具注册与路由
├── ModelContextProtocolToolDispatcher.java # 工具调用分发
└── MCPAuthenticator.java               # Token 鉴权
```

#### edits/ — AI 编辑标记

追踪 AI 修改过的节点，在节点旁显示状态图标，支持清除标记操作。

---

### freeplane_plugin_script — 脚本插件

**Bundle**: `org.freeplane.plugin.script`  
**依赖**: Apache Groovy 4.0.27 + Apache Ivy 2.5.3（依赖管理）

- 支持在节点中内嵌 Groovy 脚本，或从外部 `scripts/` 目录加载
- 通过 `freeplane_api` 中的 `Node`、`MindMap`、`Controller` 接口操作导图
- 暴露 `Proxy.java`（Groovy 脚本的 DSL 入口）、`FreeplaneScriptBaseClass`
- **公共导出**：`org.freeplane.plugin.script`、`org.freeplane.plugin.script.proxy` 等包供其他插件依赖
- 内置 Groovy 示例脚本位于 `scripts/` 目录

---

### freeplane_plugin_formula — 公式插件

**Bundle**: `org.freeplane.plugin.formula`  
**依赖**: `freeplane_plugin_script`（复用 Groovy 引擎）

- 支持节点文本以 `=` 开头时作为公式求值（类似电子表格）
- 节点值可引用其他节点（通过节点 ID 或别名）
- 公式结果实时显示，支持字符串/数字/日期运算

---

### freeplane_plugin_latex — LaTeX 插件

- 节点内容支持 LaTeX 数学公式渲染（通过内嵌 LaTeX 引擎）
- 图标自动识别 `$...$` 或 `$$...$$` 标记并渲染为图片

---

### freeplane_plugin_markdown — Markdown 插件

- 节点文本支持 Markdown 语法渲染（`markedj` 库）
- 与 `freeplane_plugin_ai` 共用此依赖（AI 回复的 Markdown 渲染）

---

### freeplane_plugin_codeexplorer — 代码探索插件

- 分析 Java 项目的包/类依赖关系，以思维导图形式可视化
- 支持 ArchUnit 风格的架构规则检查

---

### 其他插件

| 插件 | 功能 |
|------|------|
| `freeplane_plugin_bugreport` | 异常捕获与 Bug 报告提交 |
| `freeplane_plugin_openmaps` | 嵌入 OpenStreetMap 地图节点 |
| `freeplane_plugin_svg` | SVG 图形渲染支持 |
| `freeplane_plugin_jsyntaxpane` | 代码语法高亮组件（被 script/formula 插件依赖） |

---

## 辅助模块

| 模块 | 说明 |
|------|------|
| `freeplane_ant` | 自定义 Ant 任务（格式校验等，供 CI 使用） |
| `freeplane_debughelper` | IDEA/Eclipse 运行配置、日志配置、安全策略 |
| `freeplane_mac` | macOS 原生菜单适配（`NSApplicationListener`） |
| `JOrtho_0.4_freeplane` | 拼写检查库（Freeplane 定制版 JOrtho） |

---

## 构建系统

### 全局构建配置（`build.gradle`）

- **版本来源**：`freeplane/src/viewer/resources/version.properties`
- **OSGi 工具**：`biz.aQute.bnd.gradle 7.1.0` 自动生成 `MANIFEST.MF`
- **插件结构规范**：
  - `pluginid` = `org.freeplane.plugin.<name>`
  - `bundleActivator` = `pluginid + ".Activator"`
  - 每个插件同时生成两个 JAR：`plugin-<ver>.jar`（无 OSGi Manifest）和 OSGi 主 JAR
- **构建产物目录**：`BIN/`（开发运行）、`DIST/`（发行版）

### 常用构建命令

```bash
# 编译所有模块
gradle build

# 仅编译核心模块
gradle :freeplane:compileJava

# 运行所有测试
gradle test

# 带详细错误输出的测试
gradle test -PTestLoggingFull

# 打包所有发行版
gradle dist

# 单独打包
gradle win.dist      # Windows 安装包
gradle mac.dist      # macOS .app 包
gradle linux-packages # DEB/RPM 包

# 翻译格式化（修改翻译文件后必须运行）
gradle format_translation
```

### 构建后运行

```bash
# Windows
BIN\freeplane.bat

# Unix/macOS
BIN/freeplane.sh
```

---

## 测试

- **测试框架**：JUnit 4 + AssertJ + Mockito
- **命名规范**：测试类以 `*Test` 结尾，或行为描述风格（如 `RuleReferenceShould`）
- **测试位置**：每个模块的 `src/test/java/`

关键测试模块：

| 路径 | 内容 |
|------|------|
| `freeplane_plugin_ai/src/test/` | AI 插件单元测试（chat/tools/mcpserver） |
| `freeplane/src/test/` | 核心功能测试 |
| `freeplane_api/src/test/` | API 接口测试 |
| `freeplane_uitest/` | UI 集成测试（HtmlUtils 等） |

---

## 国际化 (i18n)

- **翻译文件位置**：
  - `freeplane/src/editor/resources/translations/Resources_*.properties`
  - `freeplane/src/viewer/resources/translations/Resources_en.properties`
- **编码**：ISO-8859-1，非 ASCII 字符使用 `\uXXXX` 转义
- **修改后必须运行**：`gradle format_translation`
- **验证方式**：
  ```bash
  file Resources_*.properties | grep -v "ASCII text"   # 应无输出
  ```

---

## 项目版本

版本号定义在：`freeplane/src/viewer/resources/version.properties`

```properties
freeplane_version=1.13.x
freeplane_version_status=...
```

所有模块统一使用根项目版本号，由 `build.gradle` 从该文件自动读取并注入。
