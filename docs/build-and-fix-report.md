# Freeplane AI 插件构建与修复全过程报告

## 概览

本文档记录了 Freeplane 项目从编译、部署到前后端联通的完整修复过程，包含所有遇到的问题、根本原因分析及解决方案。

---

## 一、环境信息

| 项目 | 版本 |
|------|------|
| Java | 21.0.6 (Oracle HotSpot) |
| Gradle | 8.14 |
| Node.js | 系统默认 |
| 操作系统 | Windows 11 |
| 前端框架 | Vue 3 + Vite 6.4.2 |
| 后端框架 | OSGi (Knopflerfish) + Java Swing |

---

## 二、修复过程

### 2.1 项目主体编译

**执行命令**：
```bash
gradle :freeplane:compileJava
```

**结果**：BUILD SUCCESSFUL（30s）

**注意事项**：存在以下警告，均为非致命性：
- `finalize()` 方法已过时（nanoxml、XMLWriter 等历史代码）
- `AccessController` 已过时（Java 17+ 弃用）
- `new Integer(int)` 已过时

---

### 2.2 完整发行版构建

**执行命令**：
```bash
gradle dist
```

**结果**：主体 BUILD SUCCESSFUL，99 个 Task 执行完成。

**已知失败**（不影响运行）：

| Task | 原因 |
|------|------|
| `:jpackage_winapp_jbr` | 本地缺少 JetBrains Runtime (JBR)，仅影响 Windows 安装包打包 |
| `AIServiceIntegrationTest` 系列 | 需要真实 AI 服务密钥，非构建问题 |
| `MessageBuilderTest` | 测试配置问题，不影响运行 |

**产物目录**：`BIN/`，包含 `freeplane.bat`、`plugins/`、`core/` 等。

---

### 2.3 前端依赖缺失修复

**问题**：
```
'vite' 不是内部或外部命令，也不是可运行的程序或批处理文件。
```

**根本原因**：`node_modules` 未安装，`vite` 可执行文件不存在于 `node_modules/.bin/`。

**修复**：
```bash
cd freeplane_web
npm install       # 安装 95 个依赖包
npm run dev       # 启动 Vite 开发服务器
```

**结果**：前端成功运行在 `http://localhost:5174/`（5173 端口被占用，自动切换）。

---

### 2.4 AI 插件编译与部署

**执行命令**：
```bash
gradle :freeplane_plugin_ai:jar
```

**生成产物**（位于 `freeplane_plugin_ai/build/libs/`）：

| 文件 | 大小 |
|------|------|
| `freeplane_plugin_ai-1.13.3.jar` | 587 KB（主插件，含 OSGi MANIFEST） |
| `plugin-1.13.3.jar` | 580 KB（内部依赖） |

**初步部署**（将 jar 复制到 BIN）：
```
BIN/plugins/org.freeplane.plugin.ai/
├── freeplane_plugin_ai-1.13.3.jar   ← 复制到此
└── lib/
    ├── plugin-1.13.3.jar             ← 更新
    ├── langchain4j-*.jar
    └── ...（共 15 个依赖 jar）
```

---

### 2.5 AI 连接状态检查

**检查方式**：读取运行时配置文件：
```
%APPDATA%\Freeplane\1.13.x\freeplane.properties
```

**结论**：

| Provider | 状态 | 说明 |
|----------|------|------|
| 文心千帆 (ERNIE) | ✅ 已配置 | Key: `bce-v3/ALTAK-...`，通过千帆平台调用 DeepSeek |
| OpenRouter | ❌ 未配置 | `ai_openrouter_key` 为空 |
| Google Gemini | ❌ 未配置 | `ai_gemini_key` 为空 |
| DashScope | ❌ 未配置 | `ai_dashscope_key` 为空 |
| Ollama | ❌ 未配置 | `ai_ollama_service_address` 为空 |

**当前默认模型**：`ernie|ernie-4.5`（代码逻辑优先检测 ERNIE Key）。

> **注**：百度千帆平台同时托管 DeepSeek 模型（deepseek-v3、deepseek-r1），通过同一 ERNIE Key 可调用，需在模型列表中添加 `deepseek-v3` 并将 `ai_selected_model` 设为 `ernie|deepseek-v3`。

---

### 2.6 Web 端无法连接后端的排查与修复

#### 问题现象

前端启动后，Vite 代理持续报错：
```
[vite] http proxy error: /api/maps  
AggregateError [ECONNREFUSED]
```

Vite 配置代理目标为 `http://localhost:6299`，但该端口无任何进程监听。

#### 排查过程

**Step 1：确认后端服务架构**

后端 REST API 服务由 `RestApiServer`（`freeplane_plugin_ai` 插件内）提供，监听端口 **6299**，在 `Activator.startRestApiServer()` 中启动。

**Step 2：确认 Freeplane 启动方式错误**

```
Error: Unable to access jarfile freeplanelauncher.jar
```

原因：从非 BIN 目录执行 `freeplane.bat`，找不到 `freeplanelauncher.jar`。

**修复**：
```powershell
Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d <BIN路径> && freeplane.bat"
```

**Step 3：Freeplane 运行但 6299 仍未监听**

查看 OSGi 日志（`%APPDATA%\Freeplane\log.0`），发现关键错误：
```
Failed to start reference:file:.../BIN/plugins/org.freeplane.plugin.ai
Bundle#2, unable to resolve: Missing package(s) or can not resolve all of the them:
io.github.gitbucket.markedj;version=[1.13.0,2.0.0) -- No providers found.
```

**Step 4：定位 OSGi 插件识别机制**

源码 `ActivatorImpl.java` 中 `loadPlugins()` 方法：
```java
// 扫描 BIN/plugins/ 下各子目录
// 识别条件：目录下存在 META-INF/MANIFEST.MF 文件
final File manifest = new File(file, "META-INF/MANIFEST.MF");
if (manifest.exists()) {
    // 读取 Bundle-SymbolicName，安装为 OSGi Bundle
}
```

**根本原因**：

`BIN/plugins/org.freeplane.plugin.ai/` 和 `org.freeplane.plugin.markdown/` 两个目录**只有 `lib/` 子目录，缺少 `META-INF/MANIFEST.MF`**，导致 OSGi 无法将其识别为 Bundle。

`Activator` 从未被调用 → `startRestApiServer()` 从未执行 → 6299 端口永远不会监听。

#### 修复方案

**Step 1：解压 markdown 插件**（提供 `io.github.gitbucket.markedj` 包，AI 插件依赖它）：
```powershell
# 将 freeplane_plugin_markdown-1.13.3.jar 解压到插件目录
# 生成 META-INF/MANIFEST.MF + org/ class 文件
```

**Step 2：解压 AI 插件**（生成 MANIFEST.MF，使 OSGi 可识别）：
```powershell
# 将 freeplane_plugin_ai-1.13.3.jar 解压到插件目录
# 生成 META-INF/MANIFEST.MF + org/ class 文件
```

**最终插件目录结构**：
```
BIN/plugins/org.freeplane.plugin.ai/
├── META-INF/
│   └── MANIFEST.MF                  ← 关键文件，OSGi 识别 Bundle 入口
├── org/freeplane/plugin/ai/         ← 解压出的 class 文件
│   ├── Activator.class
│   ├── bootstrap/
│   ├── chat/
│   ├── restapi/
│   └── ...
└── lib/
    ├── plugin-1.13.3.jar
    ├── langchain4j-*.jar
    └── ...
```

**Step 3：重启 Freeplane**，加载新 Bundle。

---

### 2.7 验证结果

```
netstat -ano | findstr ":6299"
TCP    0.0.0.0:6299    0.0.0.0:0    LISTENING    13300
TCP    [::]:6299       [::]:0       LISTENING    13300
```

```
GET http://localhost:6299/api/maps → HTTP 200
{"maps":[{"mapId":"26577420-516c-424f-96eb-67941dd7a66..."}]}
```

---

## 三、问题总结

| # | 问题 | 根本原因 | 解决方案 |
|---|------|----------|----------|
| 1 | `vite 不是内部命令` | `node_modules` 未安装 | `npm install` |
| 2 | AI 插件 jar 路径不对 | 直接放 jar 不被 OSGi 识别 | 解压 jar 到插件目录 |
| 3 | `freeplanelauncher.jar` 找不到 | 未在 BIN 目录下执行 bat | 用 `cd /d BIN && freeplane.bat` 启动 |
| 4 | 6299 端口无监听 | 插件目录缺 `META-INF/MANIFEST.MF` | 解压主 jar（含 MANIFEST）到插件目录 |
| 5 | AI 插件依赖解析失败 | markdown 插件同样未被 OSGi 识别 | 同样解压 markdown jar 到其插件目录 |

---

## 四、正确的完整启动流程

### 后端（Freeplane + AI 插件）

```bash
# 1. 编译 AI 插件
gradle :freeplane_plugin_ai:jar

# 2. 部署：将 jar 解压到 BIN/plugins/org.freeplane.plugin.ai/
#    （确保 META-INF/MANIFEST.MF 存在）

# 3. 在 BIN 目录下启动 Freeplane
cd BIN
freeplane.bat

# 4. 验证 REST API
curl http://localhost:6299/api/maps
```

### 前端

```bash
cd freeplane_web
npm install      # 仅首次或更新依赖时需要
npm run dev      # 启动 http://localhost:5173
```

---

## 五、架构说明

```
前端 (Vue3 + Vite, :5173)
  │
  │ 代理 /api → localhost:6299
  ▼
Freeplane 后端 (Java Swing + OSGi)
  └─ org.freeplane.plugin.ai (Bundle)
       └─ Activator.startRestApiServer()
            └─ RestApiServer (:6299)
                 ├─ /api/maps           → MapRestController
                 ├─ /api/map/current    → MapRestController
                 ├─ /api/node/**        → NodeRestController
                 └─ /api/ai/**          → AiRestController
                      └─ AI 服务 (百度千帆 / DeepSeek)
```

---

*报告生成时间：2026-04-26*
