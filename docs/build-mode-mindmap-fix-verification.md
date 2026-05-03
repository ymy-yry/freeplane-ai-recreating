# Build 模式思维导图生成功能修复验证指南

## 📋 修改内容

### 修改文件
- `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/restapi/AiRestController.java`

### 核心改动
将 `handleGenerateMindMap` 方法从使用 `AIService` 改为使用 `BufferLayerRouter`，与 Auto 模式保持一致。

**修改前**：
```java
// 使用 AIService 处理请求（只返回 JSON，不创建节点）
AIService service = AIServiceLoader.selectService(request);
AIServiceResponse serviceResponse = service.processRequest(request);
sendJson(exchange, 200, serviceResponse.getData());
```

**修改后**：
```java
// 使用 BufferLayerRouter 处理请求（在后端直接创建节点）
BufferRequest bufferRequest = new BufferRequest("生成思维导图：" + topic);
bufferRequest.setRequestType(BufferRequest.RequestType.MINDMAP_GENERATION);
bufferRequest.addParameter("topic", topic);
bufferRequest.addParameter("maxDepth", maxDepth);
BufferResponse bufferResponse = bufferLayerRouter.processRequest(bufferRequest);
sendJson(exchange, 200, responseData);
```

### 优势
1. ✅ **后端直接创建节点**：通过 `MindMapBufferLayer.createMindMapNodes()` 直接调用 Freeplane 原生 API
2. ✅ **避免前端依赖**：不再依赖 `/api/nodes/create` 接口
3. ✅ **架构一致性**：Auto 模式和 Build 模式使用相同的节点创建逻辑
4. ✅ **性能提升**：减少前端多次 HTTP 请求

---

## 🚀 验证步骤

### 步骤 1：重启 Freeplane

1. 关闭当前运行的 Freeplane
2. 重新编译并启动：
   ```bash
   # 在项目根目录
   gradle :freeplane_plugin_ai:build
   # 启动 Freeplane
   BIN\freeplane.bat  # Windows
   # 或
   BIN/freeplane.sh   # Linux/macOS
   ```

### 步骤 2：打开 Web 界面

1. 确保 `freeplane_web` 开发服务器运行：
   ```bash
   cd freeplane_web
   npm run dev
   ```
2. 访问 `http://localhost:5173`

### 步骤 3：测试 Build 模式生成思维导图

1. **切换到 Build 模式**：
   - 点击 AI 面板的模式切换按钮
   - 选择 "Build" 模式

2. **输入测试主题**：
   - 在"生成思维导图"区域输入主题，例如：`人工智能应用`
   - 设置深度为 `3`
   - 点击"一键生成"

3. **预期结果**：
   - ✅ AI 面板显示"处理中..."
   - ✅ 后端日志显示：`MindMapBufferLayer: step 6 - creating mindmap nodes`
   - ✅ Freeplane 桌面端自动创建节点（无需前端调用 `/api/nodes/create`）
   - ✅ Web 界面通过轮询自动刷新显示新节点
   - ✅ 结果预览区域显示节点数量

### 步骤 4：对比 Auto 模式

1. **切换到 Auto 模式**
2. **输入相同指令**：`生成人工智能应用的思维导图`
3. **验证两种模式行为一致**：
   - 都应在后端直接创建节点
   - 都不依赖前端 `/api/nodes/create` 接口

---

## 🔍 调试技巧

### 查看后端日志

在 Freeplane 控制台查找以下关键日志：

```
AiRestController.handleGenerateMindMap: topic=人工智能应用, maxDepth=3
BufferLayerRouter: selected layer - MindMapBufferLayer
MindMapBufferLayer: step 1 - requirement analysis
MindMapBufferLayer: step 2 - prompt optimization
MindMapBufferLayer: step 3 - model selection
MindMapBufferLayer: step 4 - calling AI
MindMapBufferLayer: step 4.5 - structural validation
MindMapBufferLayer: step 5 - result optimization
MindMapBufferLayer: step 6 - creating mindmap nodes  ← 关键：节点创建
MindMapBufferLayer: processing completed successfully in XXXms
```

### 前端控制台检查

打开浏览器开发者工具（F12），应该看到：
- ✅ **没有** `POST /api/nodes/create` 请求（不再需要）
- ✅ **只有** `POST /api/ai/build/generate-mindmap` 请求
- ✅ 轮询请求 `GET /api/map/current` 返回更新后的节点树

---

## ⚠️ 可能的问题

### 问题 1：节点未创建

**症状**：AI 返回成功，但导图上没有新节点

**排查**：
1. 检查后端日志是否有 `MindMapBufferLayer: step 6` 
2. 检查是否有错误：`MindMapBufferLayer: failed to create mindmap nodes`
3. 确认 Freeplane 当前打开了一个导图

### 问题 2：缓存问题

**症状**：返回的结果与之前相同

**原因**：BufferLayerRouter 有 LRU 缓存（10 分钟过期）

**解决**：
- 修改主题内容
- 或调整深度参数
- 或等待 10 分钟后重试

### 问题 3：AI 模型未配置

**症状**：`没有可用的 AI 模型，请检查 API Key 配置`

**解决**：
1. 在 Freeplane 中配置 AI 模型
2. 或通过 ModelConfigPanel 配置

---

## 📊 成功标志

✅ **Build 模式生成思维导图后**：
1. Freeplane 桌面端自动显示新节点
2. Web 界面 3 秒内自动刷新显示
3. 无需手动点击"应用到导图"
4. 前端控制台无 404 错误

---

## 🔄 回滚方案

如果修改导致问题，可以回滚到原始实现：

```bash
git checkout HEAD -- freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/restapi/AiRestController.java
gradle :freeplane_plugin_ai:build
```

---

## 📝 技术细节

### BufferLayerRouter 工作流程

```
handleGenerateMindMap()
  ↓
BufferRequest (MINDMAP_GENERATION)
  ↓
BufferLayerRouter.processRequest()
  ↓ 检查缓存（LRU，10分钟过期）
  ↓ 选择缓冲层（MindMapBufferLayer）
  ↓
MindMapBufferLayer.process()
  ├─ 步骤 1：需求分析
  ├─ 步骤 2：提示词优化
  ├─ 步骤 3：模型选择
  ├─ 步骤 4：调用 AI
  ├─ 步骤 4.5：结构验证（环检测）
  ├─ 步骤 5：结果优化
  └─ 步骤 6：createMindMapNodes() ← 关键
      └─ MMapController.addNewNode() ← Freeplane 原生 API
```

### 与 Auto 模式的对比

| 特性 | Auto 模式 | Build 模式（修改前） | Build 模式（修改后） |
|------|----------|---------------------|---------------------|
| 路由 | BufferLayerRouter | AIService | BufferLayerRouter ✅ |
| 节点创建 | 后端直接创建 | 前端调用 API | 后端直接创建 ✅ |
| 依赖 /api/nodes/create | 否 | 是（404 错误） | 否 ✅ |
| 性能 | 高（1次请求） | 低（N+1次请求） | 高（1次请求） ✅ |

---

## 🎯 下一步优化建议

1. **为 expandNode 和 summarize 应用相同策略**
   - 将 `handleExpandNode` 和 `handleSummarize` 也改为使用 BufferLayerRouter
   
2. **移除前端节点创建逻辑**
   - 简化 `aiStore.ts` 中的 `generateMindMap` 函数
   - 不再需要 `createNodesRecursive`

3. **统一错误处理**
   - Auto 和 Build 模式使用相同的错误码和消息

---

**修改完成时间**：2026-04-28  
**修改人**：AI Assistant  
**状态**：✅ 编译成功，待验证
