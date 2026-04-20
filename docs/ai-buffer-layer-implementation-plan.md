# AI 智能缓冲层完整实施计划

**负责人**：曾鸣（成员A）  
**开始日期**：2026年4月8日  

---

## 一、实施概览

### 1.1 目标

在不影响现有功能的前提下，新增可插拔式智能缓冲层，实现：
- 用户输入自然语言需求
- 系统自动理解、优化提示词
- 智能选择最合适的 AI 模型
- 返回高质量结果

### 1.2 工作量统计

| 类别 | 数量 | 说明 |
|------|------|------|
| 新增 Java 文件 | 12 个 | 缓冲层核心组件 |
| 修改 Java 文件 | 2 个 | RestApiRouter、AiRestController |
| 新增前端文件 | 1 个 | AISmartPanel.vue |
| 修改前端文件 | 2 个 | mapApi.ts、Toolbar.vue |


---

## 二、后端实施计划

### 阶段 1：核心接口定义

#### Day 1：创建基础接口和对象

**任务 1.1**：`IBufferLayer.java` - 缓冲层标准接口
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/IBufferLayer.java`
- 工作量：50 行
- 内容：定义缓冲层必须实现的方法

**任务 1.2**：`BufferRequest.java` - 请求上下文
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/BufferRequest.java`
- 工作量：80 行
- 内容：在缓冲层各组件间传递的请求对象

**任务 1.3**：`BufferResponse.java` - 响应对象
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/BufferResponse.java`
- 工作量：100 行
- 内容：包含处理结果、质量评分、优化日志等

#### Day 2：创建路由器

**任务 1.4**：`BufferLayerRouter.java` - 缓冲层路由器
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/BufferLayerRouter.java`
- 工作量：150 行
- 内容：识别功能类型，选择合适的缓冲层

---

### 阶段 2：思维导图缓冲层实现

#### Day 3：需求理解引擎

**任务 2.1**：`MindMapRequirementAnalyzer.java`
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/mindmap/MindMapRequirementAnalyzer.java`
- 工作量：200 行
- 功能：
  - 识别任务类型（生成/展开/摘要）
  - 提取主题、层级、语言等参数
  - 识别隐含需求

#### Day 4：提示词优化器

**任务 2.2**：`MindMapPromptOptimizer.java`
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/mindmap/MindMapPromptOptimizer.java`
- 工作量：250 行
- 功能：
  - 维护领域 Prompt 模板库（5-10个）
  - 根据需求选择模板
  - 填充参数并优化

**任务 2.3**：创建 Prompt 模板配置文件
- 位置：`freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/buffer/prompts.properties`
- 工作量：100 行
- 内容：各领域 Prompt 模板

#### Day 5：模型路由器

**任务 2.4**：`MindMapModelRouter.java`
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/mindmap/MindMapModelRouter.java`
- 工作量：200 行
- 功能：
  - 维护模型能力画像
  - 实现评分算法
  - 智能选择最优模型

#### Day 6：结果优化器 + 完整缓冲层

**任务 2.5**：`MindMapResultOptimizer.java`
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/mindmap/MindMapResultOptimizer.java`
- 工作量：150 行
- 功能：
  - JSON 格式验证
  - 质量评估
  - 结果优化

**任务 2.6**：`MindMapBufferLayer.java` - 完整缓冲层
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/mindmap/MindMapBufferLayer.java`
- 工作量：150 行
- 功能：整合上述 4 个组件，实现 IBufferLayer 接口

---

### 阶段 3：集成到现有系统

#### Day 7：修改 REST API

**任务 3.1**：修改 `AiRestController.java`
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/restapi/AiRestController.java`
- 工作量：+80 行
- 内容：
  - 新增 `handleSmartRequest()` 方法
  - 注入 BufferLayerRouter
  - 保持原有方法不变

**任务 3.2**：修改 `RestApiRouter.java`
- 位置：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/restapi/RestApiRouter.java`
- 工作量：+5 行
- 内容：
  - 注册 `/api/ai/smart` 路由
  - 保持原有路由不变

#### Day 8：单元测试

**任务 3.3**：编写单元测试
- 位置：`freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/buffer/`
- 工作量：200 行
- 内容：
  - 测试需求理解引擎
  - 测试提示词优化器
  - 测试模型路由器
  - 测试完整流程

---

### 阶段 4：联调测试

#### Day 9：后端集成测试

**任务 4.1**：端到端测试
- 测试完整流程：用户输入 → 缓冲层 → 模型 → 返回结果
- 验证质量评分准确性
- 验证模型选择合理性
- 性能测试（响应时间 < 10秒）

#### Day 10：修复与优化

**任务 4.2**：根据测试结果修复问题
- 优化 Prompt 模板
- 调整模型评分权重
- 修复边界情况

---

## 三、前端实施计划

### 阶段 1：API 层

#### Day 11：新增智能接口调用

**任务 5.1**：修改 `mapApi.ts`
- 位置：`freeplane_web/src/api/mapApi.ts`
- 工作量：+30 行
- 内容：
  ```typescript
  export const smartGenerateMindMap = async (input: string) => {
    const res = await api.post('/ai/smart', { input })
    return res.data
  }
  ```

---

### 阶段 2：UI 组件

#### Day 12：智能输入面板

**任务 5.2**：创建 `AISmartPanel.vue`
- 位置：`freeplane_web/src/components/AISmartPanel.vue`
- 工作量：150 行
- 功能：
  - 自然语言输入框
  - 一键生成按钮
  - 显示处理进度
  - 展示优化日志

#### Day 13：集成到工具栏

**任务 5.3**：修改 `Toolbar.vue`
- 位置：`freeplane_web/src/components/Toolbar.vue`
- 工作量：+50 行
- 内容：
  - 添加"AI 智能生成"按钮
  - 点击打开 AISmartPanel
  - 保持原有功能不变

---

### 阶段 3：联调测试

#### Day 14：前后端联调

**任务 5.4**：完整流程测试
- 测试用户输入自然语言
- 验证缓冲层正确处理
- 验证结果渲染
- 测试错误处理

---

## 四、文件清单

### 4.1 新增文件（13个）

#### 后端（12个）
```
freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/
├── IBufferLayer.java                    # 缓冲层接口
├── BufferRequest.java                   # 请求对象
├── BufferResponse.java                  # 响应对象
├── BufferLayerRouter.java               # 路由器
└── mindmap/
    ├── MindMapBufferLayer.java          # 思维导图缓冲层
    ├── MindMapRequirementAnalyzer.java  # 需求分析器
    ├── MindMapPromptOptimizer.java      # 提示词优化器
    ├── MindMapModelRouter.java          # 模型路由器
    └── MindMapResultOptimizer.java      # 结果优化器

freeplane_plugin_ai/src/main/resources/org/freeplane/plugin/ai/buffer/
└── prompts.properties                   # Prompt 模板配置

freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/buffer/
└── BufferLayerTest.java                 # 单元测试
```

#### 前端（1个）
```
freeplane_web/src/components/
└── AISmartPanel.vue                     # 智能输入面板
```

### 4.2 修改文件（4个）

#### 后端（2个）
```
freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/restapi/
├── AiRestController.java                # +80 行（新增 handleSmartRequest）
└── RestApiRouter.java                   # +5 行（注册新路由）
```

#### 前端（2个）
```
freeplane_web/src/api/
└── mapApi.ts                            # +30 行（新增智能接口）

freeplane_web/src/components/
└── Toolbar.vue                          # +50 行（添加智能生成按钮）
```

---

## 五、接口规格

### 5.1 新增接口

#### POST `/api/ai/smart`

**请求**：
```json
{
  "input": "帮我做个 Java 学习路线"
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "text": "Java 学习路线",
    "children": [...]
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

### 5.2 原有接口（完全不变）

| 接口 | 方法 | 状态 |
|------|------|------|
| `/api/ai/models` | GET | ✅ 不变 |
| `/api/ai/chat` | POST | ✅ 不变 |
| `/api/ai/generate-mindmap` | POST | ✅ 不变 |
| `/api/ai/expand-node` | POST | ✅ 不变 |
| `/api/ai/summarize` | POST | ✅ 不变 |
| `/api/ai/tag` | POST | ✅ 不变 |

---

## 六、验收标准

### 6.1 功能验收

- [ ] 用户输入自然语言能正确识别意图
- [ ] Prompt 优化后质量明显提升
- [ ] 模型选择合理（思维导图选 GPT-4o）
- [ ] 结果质量评分准确
- [ ] 优化日志完整展示

### 6.2 兼容性验收

- [ ] 原有接口 100% 可用
- [ ] 原有前端功能不受影响
- [ ] 新旧接口可以同时工作

### 6.3 性能验收

- [ ] 缓冲层处理时间 < 1 秒
- [ ] 总响应时间 < 10 秒
- [ ] 内存占用增加 < 50MB

### 6.4 质量验收

- [ ] 单元测试覆盖率 > 80%
- [ ] 无编译错误
- [ ] 无运行时异常
- [ ] 代码符合规范

---

## 七、风险与应对

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| Prompt 模板不准确 | 中 | 中 | 持续优化 + 用户反馈 |
| 模型选择算法偏差 | 中 | 低 | 人工可调权重 |
| 性能下降 | 高 | 低 | 性能监控 + 优化 |
| 兼容性问题 | 高 | 低 | 充分测试 + 回滚机制 |

---

## 八、后续扩展

### 8.1 可扩展的缓冲层

- [ ] 代码生成缓冲层
- [ ] 翻译缓冲层
- [ ] 数据分析缓冲层
- [ ] 图像生成缓冲层

### 8.2 高级功能

- [ ] 用户偏好学习
- [ ] 多模型协作
- [ ] 自动降级策略
- [ ] 成本优化

---

**计划编制**：曾鸣（成员A）  
**编制日期**：2026年4月8日  
**下次更新**：阶段 1 完成后
