# 成员A 开发进度记录

**负责人**：曾鸣（成员A）  
**工作模块**：Java 后端 REST API 桥接层  
**所在目录**：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/restapi/`  
**更新日期**：2026-03-26

---

## 一、已完成工作概览

### 新建文件（6个）

| 文件 | 行数 | 说明 |
|------|------|------|
| `RestApiServer.java` | 42 | HTTP 服务器入口，监听端口 6299 |
| `CorsFilter.java` | 39 | 跨域处理，统一添加 CORS 响应头 |
| `RestApiRouter.java` | 168 | 路由分发，将请求转发到对应 Controller |
| `MapRestController.java` | 140 | 导图数据接口（获取节点树、单节点详情） |
| `NodeRestController.java` | 275 | 节点操作接口（增、删、改、查、搜索） |
| `AiRestController.java` | 260 | AI 接口（模型列表、对话、展开、摘要、标签） |

### 修改文件（1个）

| 文件 | 改动 | 说明 |
|------|------|------|
| `Activator.java` | +20 行 | 新增 REST API 服务器的启动与停止逻辑 |

**新增代码总量：约 924 行**

---

## 二、已实现接口列表（共 11 个）

### 导图数据接口

| 方法 | 路径 | 状态 | 说明 |
|------|------|------|------|
| GET | `/api/map/current` | ✅ 已实现 | 返回当前导图完整节点树 JSON |
| GET | `/api/nodes/{nodeId}` | ✅ 已实现 | 返回单节点详情（含 attributes 字段） |

### 节点操作接口

| 方法 | 路径 | 状态 | 说明 |
|------|------|------|------|
| POST | `/api/nodes/search` | ✅ 已实现 | 关键词全文搜索，支持大小写控制，支持 mapId 定位导图 |
| POST | `/api/nodes/create` | ✅ 已实现 | 在指定父节点下新建子节点，支持 mapId |
| POST | `/api/nodes/edit` | ✅ 已实现 | 修改节点文本，通过 MMapController 触发 UI 刷新 |
| POST | `/api/nodes/delete` | ✅ 已实现 | 删除节点（含根节点保护），支持 mapId |

### AI 接口

| 方法 | 路径 | 状态 | 说明 |
|------|------|------|------|
| GET | `/api/ai/models` | ✅ 已实现 | 动态读取配置，返回已配置 API Key 的模型列表 |
| POST | `/api/ai/chat` | ✅ 框架已通 | 接口结构完整，读取 message/modelSelection/mapId/selectedNodeId，返回 stub 响应，待接入 AIChatService |
| POST | `/api/ai/expand-node` | ✅ 框架已通 | stub，读取 mapId/nodeId/depth/count/focus，待成员D接入 ExpandNodeTool |
| POST | `/api/ai/summarize` | ✅ 框架已通 | stub，读取 mapId/nodeId/maxWords/writeToNote，待成员D接入 SummarizeBranchTool |
| POST | `/api/ai/tag` | ✅ 框架已通 | stub，读取 mapId/nodeIds[]，含空值校验，待成员D接入 AutoTagNodesTool |

---

## 三、核心实现要点

### 3.1 服务器启动机制（Activator.java）

REST API 服务器随 Freeplane 插件一同启动，在 OSGi `installExtension` 回调中执行：

```java
startModelContextProtocolServer(aiChatPanel, modeController);  // 原有 MCP（port 6298）
startRestApiServer(aiChatPanel, modeController);               // 新增 REST（port 6299）
```

`stop()` 方法中新增 `restApiServer.stop()`，确保插件卸载时释放端口。

### 3.2 mapId 路由机制（NodeRestController）

所有节点操作接口支持通过请求体中的 `mapId` 定位目标导图：
- 传入合法 UUID → `AvailableMaps.findMapModel(uuid)` 查找对应导图
- mapId 缺失或格式非法 → 自动回退到当前激活的导图

### 3.3 节点序列化结构

`GET /api/map/current` 返回的节点 JSON 结构与前端 `types/mindmap.ts` 约定一致：

```json
{
  "id": "ID_ROOT",
  "text": "中心节点",
  "parentId": null,
  "folded": false,
  "note": "",
  "children": [ ... ]
}
```

### 3.4 跨域处理

所有响应统一由 `CorsFilter.addCorsHeaders()` 添加：
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Content-Type: application/json; charset=UTF-8
```
OPTIONS 预检请求返回 204，浏览器同源策略不再阻断 Vue 前端（port 5173）访问后端（port 6299）。

---

## 四、接口文档匹配状态

已对照《Freeplane Web REST API 接口文档》完成以下修复：

| 修复项 | 内容 |
|--------|------|
| NodeRestController mapId | search/create/edit/delete 四个接口均支持 mapId 字段，自动查找目标导图 |
| handleChat 上下文参数 | 补充读取 mapId、selectedNodeId |
| handleExpandNode 扩展参数 | 补充读取 mapId、depth、count、focus |
| handleSummarize 扩展参数 | 补充读取 mapId、maxWords、writeToNote |
| handleTag 参数 | 补充读取 mapId、nodeIds 数组，新增非空校验 |

**当前代码与接口文档完全匹配。**

---

## 五、待后续接入的 stub 接口

以下三个 AI 功能接口已完成路径注册和参数读取，等待成员D实现具体工具类后替换 stub 响应：

| 接口 | 等待接入的工具类 |
|------|----------------|
| `POST /api/ai/chat` | AIChatService |
| `POST /api/ai/expand-node` | ExpandNodeTool |
| `POST /api/ai/summarize` | SummarizeBranchTool |
| `POST /api/ai/tag` | AutoTagNodesTool |

---

## 六、与其他成员的联调约定

### 与成员B（前端）约定的 JSON 数据结构

```typescript
// types/mindmap.ts
interface MindMapNode {
  id: string;
  text: string;
  parentId: string | null;
  folded: boolean;
  note: string;
  children: MindMapNode[];
}

interface MindMap {
  mapId: string;
  title: string;
  root: MindMapNode;
}
```

### 与成员D（AI工具）的接入点约定

成员D实现工具类后，在 `AiRestController` 对应方法的 stub 注释处替换调用逻辑，接口路径和请求/响应格式无需变更。
