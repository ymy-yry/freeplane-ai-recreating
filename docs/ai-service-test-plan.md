# AI功能测试方案

## 📋 测试概览

基于最新代码改动，本测试方案覆盖三种AI使用模式的完整功能验证。

### 改动背景
1. **系统提示词优化** - 新增角色定义、工具调用指导、响应格式标准、质量保证检查
2. **Prompt模板增强** - 思维导图生成、节点展开、分支摘要三大模板全面升级
3. **工具注册修复** - 简化执行器创建流程，直接使用toolSet
4. **前端新增组件** - AiAutoPanel、AiBuildPanel、ModelConfigPanel

---

## 🎯 测试模式分类

### 一、Chat模式（智能对话）

**服务类**: `DefaultChatService`  
**测试文件**: `AIServiceIntegrationTest.java` - Chat-TC01~06

| 测试用例 | 测试目标 | 验证要点 | 关联改动 |
|---------|---------|---------|---------|
| Chat-TC01 | 基础对话功能 | 回复生成、token统计 | 基础功能 |
| Chat-TC02 | 空消息验证 | 错误提示友好性 | 输入校验 |
| Chat-TC03 | 工具调用能力 | AI识别工具场景并调用 | 工具调用指导(6条规范) |
| Chat-TC04 | 多轮对话上下文 | 历史记忆、连贯性 | 对话记忆系统 |
| Chat-TC05 | 响应格式验证 | Markdown格式输出 | 响应格式指导(4条标准) |
| Chat-TC06 | 错误处理 | 未配置provider提示 | 错误处理 |

**关键验证点**:
- ✅ 系统提示词包含角色定义："Freeplane AI Assistant"
- ✅ 工具调用遵循规范：request参数包装、参数验证、错误处理
- ✅ 响应使用Markdown格式
- ✅ Token统计准确（inputTokens, outputTokens）

---

### 二、Build模式（结构化操作）

**服务类**: `DefaultAgentService`  
**测试文件**: `AIServiceIntegrationTest.java` - Build-TC01~09

| 测试用例 | 测试目标 | 验证要点 | 关联改动 |
|---------|---------|---------|---------|
| Build-TC01 | 思维导图生成 | JSON结构、主题、深度 | Prompt模板增强（角色+任务+标准） |
| Build-TC02 | 生成参数验证 | 空主题错误提示 | 输入校验 |
| Build-TC03 | 节点展开 | 子节点生成、数量、深度 | 节点展开模板（6步流程） |
| Build-TC04 | 展开参数验证 | 缺少nodeId错误 | 输入校验 |
| Build-TC05 | 分支摘要 | 摘要长度、内容质量 | 摘要模板（6步流程+质量标准） |
| Build-TC06 | 自动标签 | 标签批量应用 | Agent功能 |
| Build-TC07 | 未知action | 错误提示 | 错误处理 |
| Build-TC08 | 缺失action | 错误提示 | 错误处理 |
| Build-TC09 | 不同深度生成 | 1-6层深度支持 | 前端AiBuildPanel深度选择器 |

**关键验证点**:
- ✅ 思维导图生成返回严格JSON：`{"text": "主题", "children": [...]}`
- ✅ 节点展开返回：`{"children": [{"text": "子节点1"}, ...]}`
- ✅ 摘要长度不超过maxWords限制
- ✅ Prompt模板包含：角色定义、详细指令、质量标准、返回格式

**支持的操作**:
```java
"generate-mindmap"  // 生成思维导图
"expand-node"       // 展开节点
"summarize"         // 分支摘要
"tag"              // 自动标签
```

---

### 三、Auto模式（智能路由）

**服务类**: `DefaultChatService` + `DefaultAgentService`  
**测试文件**: `AIServiceIntegrationTest.java` - Auto-TC01~08

| 测试用例 | 测试目标 | 验证要点 | 关联改动 |
|---------|---------|---------|---------|
| Auto-TC01 | 路由到Chat | 普通对话识别 | 自然语言理解 |
| Auto-TC02 | 路由到Build | 结构化操作识别 | 意图识别 |
| Auto-TC03 | 解析展开指令 | "展开这个节点，生成5个子节点" | AiAutoPanel示例指令 |
| Auto-TC04 | 解析摘要指令 | "帮我总结这个分支内容" | AiAutoPanel示例指令 |
| Auto-TC05 | 解析问答指令 | "这个节点讲的是什么" | AiAutoPanel示例指令 |
| Auto-TC06 | 当前节点上下文 | 使用selectedNodeId、mapId | AiAutoPanel当前节点显示 |
| Auto-TC07 | 模型配置集成 | 使用选定模型 | ModelConfigPanel(6种Provider) |
| Auto-TC08 | 结果预览 | 返回预览数据 | AiAutoPanel结果预览区域 |

**关键验证点**:
- ✅ 自然语言指令正确解析为action
- ✅ 对话类请求路由到Chat服务
- ✅ 操作类请求路由到Agent服务
- ✅ 使用当前选中节点上下文
- ✅ 支持ModelConfigPanel配置的6种Provider

**路由规则**:
```
包含操作关键词（生成/展开/总结/标签/创建） -> Build模式(Agent)
其他 -> Chat模式(对话)
```

---

## 📊 性能测试

| 测试用例 | 测试目标 | 验证指标 | 用户关注 |
|---------|---------|---------|---------|
| Perf-TC01 | Chat响应时间 | TTFT < 30秒 | ⭐ 首token延迟 |
| Perf-TC02 | Token使用统计 | inputTokens > 0, outputTokens > 0 | ⭐ TPS计算基础 |

**性能指标说明**:
- **TTFT (Time To First Token)**: 首token延迟，影响用户体验
- **TPS (Tokens Per Second)**: 每秒token数，衡量生成速度
- **Token Usage**: 输入/输出token统计，用于成本核算

---

## 🔍 边界测试

| 测试用例 | 测试目标 | 验证要点 |
|---------|---------|---------|
| Edge-TC01 | 超长消息处理 | 不崩溃、适当处理 |
| Edge-TC02 | 特殊字符处理 | HTML标签、Markdown符号、&符号 |
| Edge-TC03 | 并发请求 | 多线程安全、状态隔离 |

---

## 🛠️ 运行测试

### 运行所有测试
```bash
gradle :freeplane_plugin_ai:test
```

### 运行特定测试类
```bash
gradle :freeplane_plugin_ai:test --tests AIServiceIntegrationTest
```

### 运行特定测试方法
```bash
gradle :freeplane_plugin_ai:test --tests AIServiceIntegrationTest.chat_basicConversation_shouldReturnReply
```

### 查看详细日志
```bash
gradle :freeplane_plugin_ai:test -PTestLoggingFull
```

---

## 📈 测试覆盖度

### 功能覆盖
- [x] Chat对话模式（6个测试用例）
- [x] Build构建模式（9个测试用例）
- [x] Auto自动模式（8个测试用例）
- [x] 性能测试（2个测试用例）
- [x] 边界测试（3个测试用例）

**总计**: 28个测试用例

### 代码改动覆盖
- [x] MessageBuilder.java - 系统提示词优化
- [x] prompts.properties - Prompt模板增强
- [x] AIChatService.java - 工具注册修复
- [x] DefaultChatService.java - 自动模型选择
- [x] DefaultAgentService.java - 四大操作支持
- [x] 前端组件 - AiAutoPanel、AiBuildPanel、ModelConfigPanel

---

## ⚠️ 已知问题

### 测试失败（待修复）
- **MessageBuilderTest** - 3个测试失败
  - 原因：测试期望字符串与实际实现不匹配
  - 测试期望: `"Respond in Markdown."`
  - 实际实现: `"Respond in Markdown format for regular conversations"`
  - 影响：不影响功能运行，仅影响测试套件
  - 修复建议：更新测试断言匹配新实现

---

## 🎯 测试优先级

### P0 - 必须通过
1. Chat-TC01: 基础对话功能
2. Build-TC01: 思维导图生成
3. Build-TC03: 节点展开
4. Build-TC05: 分支摘要
5. Auto-TC01: 路由到Chat
6. Auto-TC02: 路由到Build

### P1 - 重要
1. Chat-TC03: 工具调用能力
2. Chat-TC05: 响应格式验证
3. Build-TC09: 不同深度生成
4. Auto-TC03~05: 自然语言解析
5. Perf-TC01: 响应时间

### P2 - 建议
1. Chat-TC04: 多轮对话上下文
2. Edge-TC01~03: 边界测试
3. Auto-TC06~08: 高级功能

---

## 📝 测试数据准备

### AI Provider配置
测试前需配置至少一个AI Provider：
```properties
# 选择其一配置
ai.openrouter.key=your_key
ai.gemini.key=your_key
ai.dashscope.key=your_key
ai.ernie.key=your_key
ai.ollama.address=http://localhost:11434
```

### 测试思维导图
准备测试用思维导图文件，包含：
- 多个节点（用于展开测试）
- 较长内容分支（用于摘要测试）
- 不同层级结构（用于深度测试）

---

## 🔧 测试环境要求

- Java 21.0.6
- Gradle 8.14+
- 至少一个AI Provider配置
- 网络连接（云端Provider）或本地Ollama服务

---

## 📌 注意事项

1. **Token消耗**: 运行完整测试会消耗API token，注意成本控制
2. **网络延迟**: 性能测试受网络影响，结果可能波动
3. **并发限制**: 部分API有并发限制，Edge-TC03可能失败
4. **模型差异**: 不同Provider/模型的输出格式可能略有差异
5. **测试隔离**: 每个测试用例应独立运行，避免状态污染

---

## 🚀 后续优化建议

1. **Mock AI服务**: 创建Mock服务避免真实API调用，提升测试速度
2. **Snapshot测试**: 对Prompt模板输出进行snapshot测试
3. **集成测试**: 增加端到端测试（前端 -> 后端 -> AI服务）
4. **性能基准**: 建立性能基准，监控TTFT/TPS变化
5. **契约测试**: 验证JSON输出格式符合前端期望
