# Freeplane Web REST API 接口文档

**版本**：1.0  
**更新日期**：2026年4月8日  
**基础地址**：`http://localhost:6299`

---

## 一、接口概览

### 1.1 接口分类

| 分类 | 数量 | 说明 |
|------|------|------|
| 导图数据接口 | 2 个 | 获取导图数据、节点详情 |
| 节点操作接口 | 5 个 | 增删改查、折叠展开 |
| AI 功能接口 | 6 个 | 模型列表、对话、生成、展开、摘要、标签 |
| **智能缓冲层接口** | **1 个** | **智能路由、自动优化** |

**总计**：14 个接口

---

## 二、通用说明

### 2.1 跨域配置

所有接口均已配置 CORS，支持跨域请求：

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type
```

### 2.2 响应格式

**成功响应**：
```json
{
  "success": true,
  "data": { ... }
}
```

**错误响应**：
```json
{
  "error": "错误信息"
}
```

### 2.3 通用错误码

| HTTP 状态码 | 说明 |
|------------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 三、导图数据接口

### 3.1 获取当前导图

**接口**：`GET /api/map/current`

**说明**：返回当前打开的思维导图完整节点树

**请求**：无

**响应**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "我的思维导图",
  "root": {
    "id": "node_1",
    "text": "中心主题",
    "parentId": null,
    "folded": false,
    "note": "",
    "attributes": [],
    "children": [
      {
        "id": "node_2",
        "text": "分支 1",
        "parentId": "node_1",
        "folded": false,
        "note": "备注信息",
        "attributes": [
          {"key": "优先级", "value": "高"}
        ],
        "children": []
      }
    ]
  }
}
```

---

### 3.2 获取节点详情

**接口**：`GET /api/nodes/{nodeId}`

**说明**：返回指定节点的详细信息

**路径参数**：
- `nodeId`：节点 ID

**响应**：
```json
{
  "id": "node_2",
  "text": "分支 1",
  "parentId": "node_1",
  "folded": false,
  "note": "备注信息",
  "attributes": [
    {"key": "优先级", "value": "高"}
  ],
  "children": []
}
```

---

## 四、节点操作接口

### 4.1 创建节点

**接口**：`POST /api/nodes/create`

**请求体**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "parentId": "node_1",
  "text": "新节点"
}
```

**响应**：
```json
{
  "nodeId": "node_3",
  "text": "新节点",
  "parentId": "node_1"
}
```

---

### 4.2 编辑节点

**接口**：`POST /api/nodes/edit`

**请求体**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "nodeId": "node_2",
  "text": "修改后的文本"
}
```

**响应**：
```json
{
  "nodeId": "node_2",
  "text": "修改后的文本"
}
```

---

### 4.3 删除节点

**接口**：`POST /api/nodes/delete`

**请求体**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "nodeId": "node_2"
}
```

**响应**：
```json
{
  "deleted": true,
  "nodeId": "node_2"
}
```

**注意**：不能删除根节点

---

### 4.4 折叠/展开节点

**接口**：`POST /api/nodes/toggle-fold`

**请求体**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "nodeId": "node_1",
  "folded": true
}
```

**响应**：
```json
{
  "nodeId": "node_1",
  "folded": true
}
```

---

### 4.5 搜索节点

**接口**：`POST /api/nodes/search`

**请求体**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "query": "关键词",
  "caseSensitive": false
}
```

**响应**：
```json
{
  "results": [
    {
      "nodeId": "node_2",
      "text": "包含关键词的节点",
      "path": "中心主题 > 分支 1 > 包含关键词的节点"
    }
  ],
  "totalCount": 1
}
```

---

## 五、AI 功能接口

### 5.1 获取可用模型列表

**接口**：`GET /api/ai/models`

**说明**：返回已配置 API Key 的 AI 模型列表

**响应**：
```json
{
  "models": [
    {
      "providerName": "openrouter",
      "providerDisplayName": "OpenRouter",
      "modelName": "openai/gpt-4o",
      "displayName": "OpenRouter: openai/gpt-4o",
      "isFree": false
    },
    {
      "providerName": "gemini",
      "providerDisplayName": "Google Gemini",
      "modelName": "gemini-2.0-flash",
      "displayName": "Gemini: gemini-2.0-flash",
      "isFree": false
    }
  ]
}
```

---

### 5.2 AI 对话

**接口**：`POST /api/ai/chat`

**请求体**：
```json
{
  "message": "你好，请介绍一下自己",
  "modelSelection": "openai/gpt-4o",
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "selectedNodeId": "node_1"
}
```

**响应**：
```json
{
  "reply": "你好！我是 AI 助手...",
  "tokenUsage": {
    "inputTokens": 15,
    "outputTokens": 120
  }
}
```

**状态**：⚠️ 框架已通，待接入实际 AI 服务

---

### 5.3 一键生成思维导图

**接口**：`POST /api/ai/generate-mindmap`

**请求体**：
```json
{
  "topic": "Java 学习路线",
  "modelSelection": "openai/gpt-4o",
  "maxDepth": 3
}
```

**响应**：
```json
{
  "success": true,
  "topic": "Java 学习路线",
  "nodeCount": 23,
  "mapId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**状态**：✅ 已实现

---

### 5.4 AI 展开节点

**接口**：`POST /api/ai/expand-node`

**请求体**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "nodeId": "node_1",
  "depth": 2,
  "count": 5,
  "focus": "技术方向"
}
```

**响应**：
```json
{
  "nodeId": "node_1",
  "createdNodes": [],
  "summary": "[stub] expand-node 接口已就绪，待接入 ExpandNodeTool"
}
```

**状态**：🔧 接口已就绪，待实现

---

### 5.5 分支摘要

**接口**：`POST /api/ai/summarize`

**请求体**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "nodeId": "node_1",
  "maxWords": 100,
  "writeToNote": true
}
```

**响应**：
```json
{
  "nodeId": "node_1",
  "summary": "[stub] summarize 接口已就绪，待接入 SummarizeBranchTool",
  "wordCount": 0
}
```

**状态**：🔧 接口已就绪，待实现

---

### 5.6 自动标签

**接口**：`POST /api/ai/tag`

**请求体**：
```json
{
  "mapId": "550e8400-e29b-41d4-a716-446655440000",
  "nodeIds": ["node_1", "node_2", "node_3"]
}
```

**响应**：
```json
{
  "results": [],
  "message": "[stub] tag 接口已就绪，待接入 AutoTagNodesTool"
}
```

**状态**：🔧 接口已就绪，待实现

---

## 六、智能缓冲层接口（新增）

### 6.1 智能 AI 请求

**接口**：`POST /api/ai/smart`

**说明**：智能缓冲层入口，自动识别意图、优化提示词、选择最优模型、返回高质量结果

**特性**：
- ✅ 自动识别任务类型（思维导图/对话/摘要等）
- ✅ 自动优化提示词（使用领域模板）
- ✅ 智能选择模型（基于能力评分）
- ✅ 结果质量评估
- ✅ 完整处理日志

**请求体**：
```json
{
  "input": "帮我做个 Java 学习路线"
}
```

**成功响应**：
```json
{
  "success": true,
  "data": {
    "text": "Java 学习路线",
    "children": [
      {
        "text": "基础语法",
        "children": [
          {"text": "变量与数据类型"},
          {"text": "运算符"},
          {"text": "控制流程"}
        ]
      },
      {
        "text": "面向对象",
        "children": [
          {"text": "类与对象"},
          {"text": "继承与多态"},
          {"text": "接口与抽象类"}
        ]
      }
    ]
  },
  "usedModel": "openai/gpt-4o",
  "qualityScore": 88.5,
  "bufferLayer": "MindMapBufferLayer",
  "processingTime": 5234,
  "logs": [
    "需求识别: MINDMAP_GENERATION",
    "提示词优化: 512 字符",
    "模型选择: openai/gpt-4o",
    "质量评分: 88.5"
  ]
}
```

**错误响应**：
```json
{
  "success": false,
  "errorMessage": "未找到合适的缓冲层处理该请求",
  "processingTime": 120
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | boolean | 是否成功 |
| `data` | object | 处理结果（思维导图数据/对话内容等） |
| `usedModel` | string | 系统自动选择的模型 |
| `qualityScore` | number | 质量评分（0-100） |
| `bufferLayer` | string | 使用的缓冲层名称 |
| `processingTime` | number | 处理耗时（毫秒） |
| `logs` | string[] | 处理日志（透明化展示） |
| `errorMessage` | string | 错误信息（失败时） |

**支持的缓冲层**：

| 缓冲层 | 功能 | 状态 |
|--------|------|------|
| MindMapBufferLayer | 思维导图生成 | ✅ 已实现 |
| CodeBufferLayer | 代码生成 | 📋 计划中 |
| TranslationBufferLayer | 翻译 | 📋 计划中 |

**使用示例**：

```javascript
// 前端调用示例
const response = await fetch('http://localhost:6299/api/ai/smart', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    input: '帮我做个 Java 学习路线'
  })
});

const result = await response.json();

if (result.success) {
  console.log('使用的模型:', result.usedModel);
  console.log('质量评分:', result.qualityScore);
  console.log('思维导图数据:', result.data);
  console.log('处理日志:', result.logs);
}
```

**与原有接口的区别**：

| 维度 | 原有接口 | 智能缓冲层接口 |
|------|---------|---------------|
| 输入方式 | 结构化参数 | 自然语言 |
| 模型选择 | 手动指定 | 自动选择 |
| 提示词 | 用户编写 | 自动优化 |
| 输出质量 | 依赖用户 | 系统保障 |
| 透明度 | 低 | 高（完整日志） |

---

## 七、向后兼容性

### 7.1 原有接口状态

| 接口 | 状态 | 说明 |
|------|------|------|
| `/api/map/current` | ✅ 不变 | 继续使用 |
| `/api/nodes/{id}` | ✅ 不变 | 继续使用 |
| `/api/nodes/search` | ✅ 不变 | 继续使用 |
| `/api/nodes/create` | ✅ 不变 | 继续使用 |
| `/api/nodes/edit` | ✅ 不变 | 继续使用 |
| `/api/nodes/delete` | ✅ 不变 | 继续使用 |
| `/api/nodes/toggle-fold` | ✅ 不变 | 继续使用 |
| `/api/ai/models` | ✅ 不变 | 继续使用 |
| `/api/ai/chat` | ✅ 不变 | 继续使用 |
| `/api/ai/generate-mindmap` | ✅ 不变 | 继续使用 |
| `/api/ai/expand-node` | ✅ 不变 | 继续使用 |
| `/api/ai/summarize` | ✅ 不变 | 继续使用 |
| `/api/ai/tag` | ✅ 不变 | 继续使用 |

### 7.2 新增接口

| 接口 | 状态 | 说明 |
|------|------|------|
| `/api/ai/smart` | 🆕 新增 | 智能缓冲层入口 |

**重要**：所有原有接口保持不变，智能缓冲层接口为可选使用。

---

## 八、配置说明

### 8.1 AI 模型配置

在 Freeplane 偏好设置中配置 API Key：

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `ai_openrouter_key` | OpenRouter API Key | `sk-or-xxx` |
| `ai_gemini_key` | Google Gemini API Key | `AIza-xxx` |
| `ai_dashscope_key` | 通义千问 API Key | `sk-xxx` |
| `ai_ernie_key` | 文心一言 API Key | `xxx` |

### 8.2 智能缓冲层配置

智能缓冲层自动读取上述配置，无需额外配置。

---

## 九、错误处理

### 9.1 常见错误

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| `No map is currently open` | 未打开思维导图 | 先在 Freeplane 中打开思维导图 |
| `nodeId is required` | 缺少节点 ID | 检查请求参数 |
| `Cannot delete root node` | 尝试删除根节点 | 根节点不能删除 |
| `Parent node not found` | 父节点不存在 | 检查 parentId 是否正确 |
| `未找到合适的缓冲层处理该请求` | 无法识别意图 | 使用更明确的输入 |

### 9.2 重试策略

前端应实现重试机制：

```javascript
const fetchWithRetry = async (url, options, retries = 3) => {
  for (let i = 0; i < retries; i++) {
    try {
      const response = await fetch(url, options);
      if (response.ok) return response.json();
    } catch (error) {
      if (i === retries - 1) throw error;
      await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
    }
  }
};
```

---

## 十、测试工具

### 10.1 使用 cURL 测试

```bash
# 获取当前导图
curl http://localhost:6299/api/map/current

# 创建节点
curl -X POST http://localhost:6299/api/nodes/create \
  -H "Content-Type: application/json" \
  -d '{"mapId":"xxx","parentId":"node_1","text":"新节点"}'

# 智能生成
curl -X POST http://localhost:6299/api/ai/smart \
  -H "Content-Type: application/json" \
  -d '{"input":"帮我做个 Java 学习路线"}'
```

### 10.2 使用 Postman 测试

导入集合后直接测试，无需额外配置。

---

## 十一、更新日志

### 2026-04-08

- ✅ 新增智能缓冲层接口 `/api/ai/smart`
- ✅ 新增折叠/展开节点接口 `/api/nodes/toggle-fold`
- ✅ 完善接口文档

---

**文档维护**：曾鸣（成员A）、赵佳艺（成员B）  
**最后更新**：2026年4月8日
