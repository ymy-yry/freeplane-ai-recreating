# AI Agent 增强配置说明

## 概述

本次升级为 Freeplane AI 插件引入了增强的 Agent 能力配置，支持更智能的推理和工具使用策略。

## 新增配置项

### 1. 思维链推理 (Chain of Thought)

```properties
# 启用思维链推理模式（默认：true）
ai_agent_enable_chain_of_thought=true
```

**作用**：让 AI Agent 在处理复杂任务时进行逐步推理，提高决策质量。

### 2. 工具调用验证 (Tool Validation)

```properties
# 启用工具调用前参数验证（默认：true）
ai_agent_enable_tool_validation=true
```

**作用**：在调用工具前验证参数有效性，减少错误调用。

### 3. 质量检查 (Quality Check)

```properties
# 启用响应质量检查（默认：true）
ai_agent_enable_quality_check=true
```

**作用**：在返回结果前进行自我验证，确保满足用户需求。

### 4. 最大工具调用次数

```properties
# 每轮对话最大工具调用次数（默认：10，范围：1-50）
ai_agent_max_tool_calls_per_turn=10
```

**作用**：防止无限工具调用循环，控制资源使用。

### 5. 自我修正 (Self Correction)

```properties
# 启用自我修正机制（默认：true）
ai_agent_enable_self_correction=true
```

**作用**：允许 Agent 发现并纠正自己的错误。

## Prompt 工程增强

系统消息中新增了以下指导内容：

### 思维链指导
```
For complex tasks, think step-by-step:
1. Understand the user's intent
2. Plan your approach
3. Execute tools as needed
4. Verify results before responding
5. Provide clear, structured output
```

### 工具使用策略
```
Tool usage best practices:
- Use tools when they can help achieve the goal
- Combine multiple tool calls when appropriate
- Handle tool errors gracefully
- Always validate tool arguments before calling
```

### 质量检查指导
```
Before finalizing your response:
- Ensure all user requirements are met
- Verify map/node operations succeeded
- Check for consistency and completeness
```

## LangChain4j 版本升级

从 1.10.0 升级到 1.12.0，获得以下改进：

- 更好的工具调用支持
- 改进的流式输出
- 增强的错误处理
- 性能优化

## 配置位置

配置文件位于 Freeplane 的用户配置目录中的 `preferences.properties` 文件。

## 推荐配置

### 开发环境
```properties
ai_agent_enable_chain_of_thought=true
ai_agent_enable_tool_validation=true
ai_agent_enable_quality_check=true
ai_agent_max_tool_calls_per_turn=15
ai_agent_enable_self_correction=true
```

### 生产环境（保守）
```properties
ai_agent_enable_chain_of_thought=true
ai_agent_enable_tool_validation=true
ai_agent_enable_quality_check=true
ai_agent_max_tool_calls_per_turn=8
ai_agent_enable_self_correction=true
```

### 生产环境（高性能）
```properties
ai_agent_enable_chain_of_thought=false
ai_agent_enable_tool_validation=true
ai_agent_enable_quality_check=false
ai_agent_max_tool_calls_per_turn=5
ai_agent_enable_self_correction=false
```

## 监控和调试

启用日志查看 Agent 配置：
```
Creating AIChatService with agent configuration: Agent Configuration [ChainOfThought: true, ToolValidation: true, QualityCheck: true, MaxToolCalls: 10, SelfCorrection: true]
```

## 注意事项

1. **性能影响**：启用所有增强功能会增加响应时间 10-30%
2. **Token 消耗**：思维链和质量检查会增加约 20% 的 token 使用量
3. **兼容性**：配置项向后兼容，未配置时使用默认值
4. **调试建议**：遇到问题时可临时关闭质量检查和思维链以定位问题

## 故障排除

### Agent 响应变慢
- 减少 `ai_agent_max_tool_calls_per_turn`
- 临时关闭 `ai_agent_enable_chain_of_thought`

### 工具调用失败
- 确保 `ai_agent_enable_tool_validation=true`
- 检查工具参数格式

### Token 消耗过高
- 关闭 `ai_agent_enable_quality_check`
- 减少 `ai_agent_max_tool_calls_per_turn`
