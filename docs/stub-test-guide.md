# Stub 接口快速测试指南

## 🚀 测试前准备

### 1. 编译项目

```bash
cd c:\Users\zengming\Desktop\free\freeplane-1.13.x
gradle :freeplane_plugin_ai:build
```

### 2. 启动 Freeplane

```bash
# Windows
BIN\freeplane.bat

# 或使用 Gradle
gradle run
```

### 3. 配置 AI Provider

1. 打开 Freeplane
2. 进入 `工具` → `偏好设置` → `AI Chat`
3. 配置至少一个 Provider 的 API Key:
   - OpenRouter Key
   - Google Gemini Key
   - Ollama 服务地址
   - DashScope (通义千问) Key
   - ERNIE (文心一言) Key

### 4. 验证 REST API 启动

查看控制台日志，应显示:
```
RestApiServer: started on port 6299
```

---

## 🧪 接口测试

### 测试 1: AI 对话接口

**请求**:
```bash
curl -X POST http://localhost:6299/api/ai/chat \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"你好，请介绍一下自己\"}"
```

**预期响应**:
```json
{
  "reply": "你好！我是一个AI助手...",
  "tokenUsage": {
    "inputTokens": 0,
    "outputTokens": 50
  }
}
```

**验证点**:
- ✅ `reply` 字段包含 AI 回复内容
- ✅ 不是 Stub 文本（如 "[AI Response]"）
- ✅ 回复内容合理

---

### 测试 2: 获取模型列表

**请求**:
```bash
curl http://localhost:6299/api/ai/models
```

**预期响应**:
```json
{
  "models": [
    {
      "providerName": "openrouter",
      "providerDisplayName": "OpenRouter",
      "modelName": "openai/gpt-4o",
      "displayName": "OpenRouter: openai/gpt-4o",
      "isFree": false
    }
  ]
}
```

**验证点**:
- ✅ 返回已配置的 Provider
- ✅ 未配置的 Provider 不显示

---

### 测试 3: 节点扩展接口

**准备**: 
1. 在 Freeplane 中打开或创建一个思维导图
2. 选择一个节点，记录其 ID（右键 → 节点属性）

**请求**:
```bash
curl -X POST http://localhost:6299/api/ai/expand-node \
  -H "Content-Type: application/json" \
  -d "{
    \"nodeId\": \"你的节点ID\",
    \"count\": 3,
    \"focus\": \"技术实现\"
  }"
```

**预期响应**:
```json
{
  "success": true,
  "nodeId": "你的节点ID",
  "createdNodes": [
    {
      "nodeId": "node-xxx",
      "text": "子节点1"
    },
    {
      "nodeId": "node-yyy",
      "text": "子节点2"
    }
  ],
  "summary": "成功创建 2 个子节点"
}
```

**验证点**:
- ✅ `success` 为 true
- ✅ `createdNodes` 包含新创建的节点
- ✅ 在 Freeplane 画布中能看到新节点

---

### 测试 4: 分支摘要接口

**准备**:
1. 选择一个有多个子节点的分支
2. 记录根节点 ID

**请求**:
```bash
curl -X POST http://localhost:6299/api/ai/summarize \
  -H "Content-Type: application/json" \
  -d "{
    \"nodeId\": \"你的节点ID\",
    \"maxWords\": 150,
    \"writeToNote\": false
  }"
```

**预期响应**:
```json
{
  "success": true,
  "nodeId": "你的节点ID",
  "summary": "该分支主要介绍了...",
  "wordCount": 85,
  "writtenToNote": false
}
```

**验证点**:
- ✅ `summary` 字段包含摘要内容
- ✅ `wordCount` 符合预期
- ✅ 摘要内容准确反映分支主题

**可选验证** (writeToNote=true):
```bash
curl -X POST http://localhost:6299/api/ai/summarize \
  -H "Content-Type: application/json" \
  -d "{
    \"nodeId\": \"你的节点ID\",
    \"writeToNote\": true
  }"
```

在 Freeplane 中查看节点备注，应包含摘要内容。

---

### 测试 5: 自动标签接口

**准备**:
1. 选择 2-3 个节点
2. 记录节点 ID

**请求**:
```bash
curl -X POST http://localhost:6299/api/ai/tag \
  -H "Content-Type: application/json" \
  -d "{
    \"nodeIds\": [\"节点ID1\", \"节点ID2\"]
  }"
```

**预期响应**:
```json
{
  "success": true,
  "results": [
    {
      "nodeId": "节点ID1",
      "tags": ["AI", "对话", "流式输出"]
    },
    {
      "nodeId": "节点ID2",
      "tags": ["节点", "扩展", "思维导图"]
    }
  ],
  "message": "成功为 2 个节点提取标签"
}
```

**验证点**:
- ✅ 每个节点都有 `tags` 数组
- ✅ 标签数量在 2-5 个之间
- ✅ 标签与节点内容相关

---

### 测试 6: 智能缓冲层接口

**请求**:
```bash
curl -X POST http://localhost:6299/api/ai/smart \
  -H "Content-Type: application/json" \
  -d "{
    \"input\": \"帮我生成一个关于人工智能的思维导图\"
  }"
```

**预期响应**:
```json
{
  "success": true,
  "usedModel": "openrouter/openai/gpt-4o",
  "qualityScore": 0.92,
  "bufferLayer": "MindMapBufferLayer",
  "processingTime": 1234,
  "logs": [
    "需求识别: 生成思维导图",
    "提示词优化: 120 字符",
    "模型选择: openrouter/openai/gpt-4o"
  ],
  "data": {
    "text": "人工智能",
    "children": [...]
  }
}
```

**验证点**:
- ✅ `success` 为 true
- ✅ `usedModel` 显示使用的模型
- ✅ `bufferLayer` 为 "MindMapBufferLayer"
- ✅ `logs` 包含处理步骤

---

## ❌ 错误场景测试

### 测试 7: 缺少必填参数

**请求**:
```bash
curl -X POST http://localhost:6299/api/ai/chat \
  -H "Content-Type: application/json" \
  -d "{}"
```

**预期响应**:
```json
{
  "error": "message is required"
}
```

**HTTP 状态码**: 400

---

### 测试 8: 节点不存在

**请求**:
```bash
curl -X POST http://localhost:6299/api/ai/expand-node \
  -H "Content-Type: application/json" \
  -d "{
    \"nodeId\": \"不存在的节点ID\"
  }"
```

**预期响应**:
```json
{
  "error": "Node not found: 不存在的节点ID"
}
```

**HTTP 状态码**: 404

---

### 测试 9: 未配置 API Key

**操作**:
1. 删除所有 Provider 的 API Key
2. 重启 Freeplane
3. 调用对话接口

**预期响应**:
```json
{
  "reply": "[错误] AI 服务未初始化，请检查 API Key 配置和模型选择"
}
```

---

## 📊 测试检查清单

| 测试项 | 状态 | 备注 |
|--------|------|------|
| AI 对话返回真实内容 | ⬜ | |
| 模型列表显示已配置 Provider | ⬜ | |
| 节点扩展创建真实节点 | ⬜ | |
| 分支摘要生成准确内容 | ⬜ | |
| 自动标签提取合理关键词 | ⬜ | |
| 智能缓冲层正确识别意图 | ⬜ | |
| 错误参数返回友好提示 | ⬜ | |
| 节点不存在返回 404 | ⬜ | |
| 未配置 Key 返回错误提示 | ⬜ | |
| 不影响 Swing UI 使用 | ⬜ | |

---

## 🔍 调试技巧

### 查看日志

Freeplane 控制台会输出详细日志:
```
AiRestController.handleChat: received request
REST API chat success, message length: 20, reply length: 150
```

### 测试 JSON 解析

如果 AI 返回格式错误，检查:
1. AI 响应是否包含 Markdown 代码块
2. JSON 是否完整
3. 查看日志中的警告信息

### 节点创建失败

检查:
1. 父节点是否存在
2. MMapController 是否可用
3. 是否在 EDT 线程执行

---

## 📝 测试报告模板

```markdown
## 测试报告

**测试日期**: 2026-04-14
**测试人**: 
**Freeplane 版本**: 
**AI Provider**: 

### 测试结果

| 接口 | 状态 | 响应时间 | 备注 |
|------|------|---------|------|
| /api/ai/chat | ✅/❌ | xxx ms | |
| /api/ai/expand-node | ✅/❌ | xxx ms | |
| /api/ai/summarize | ✅/❌ | xxx ms | |
| /api/ai/tag | ✅/❌ | xxx ms | |
| /api/ai/smart | ✅/❌ | xxx ms | |

### 问题记录

1. 问题描述...
2. 问题描述...

### 建议

1. 优化建议...
2. 优化建议...
```

---

## 🎯 下一步

测试通过后:
1. ✅ 前端对接这些接口
2. ✅ 实现流式输出（可选）
3. ✅ 添加更多 AI 工具
