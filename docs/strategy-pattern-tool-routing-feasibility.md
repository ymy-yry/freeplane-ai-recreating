# 策略者模式在工具路由中的可行性分析

> **分析日期**: 2026-04-26  
> **分析范围**: AI 插件工具调用架构升级  
> **当前状态**: 现有架构评估 + 策略者模式方案设计

---

## 1. 当前架构分析

### 1.1 现有工具调用链路

```
用户输入 → MindMapRequirementAnalyzer（关键词匹配）
         → MindMapPromptOptimizer（选择模板）
         → MindMapModelRouter（选择 AI 模型）
         → AI 调用 → MindMapResultOptimizer（结果处理）
```

**核心问题**：
- ✅ 需求识别：基于关键词的简单路由（`MindMapRequirementAnalyzer`）
- ⚠️ 工具调用：硬编码在 `DefaultToolExecutionService` 中的 Map 映射
- ⚠️ 扩展性差：新增工具需要修改 `initializeToolExecutors()` 方法
- ⚠️ 无法动态路由：不支持根据参数特征自动选择工具变体

### 1.2 当前工具执行器注册方式

```java
// DefaultToolExecutionService.java (第 35-43 行)
private void initializeToolExecutors() {
    toolExecutors.put("readNodesWithDescendants", this::executeReadNodesWithDescendants);
    toolExecutors.put("fetchNodesForEditing", this::executeFetchNodesForEditing);
    toolExecutors.put("getSelectedMapAndNodeIdentifiers", this::executeGetSelectedMapAndNodeIdentifiers);
    toolExecutors.put("createNodes", this::executeCreateNodes);
    toolExecutors.put("edit", this::executeEdit);
}
```

**问题分析**：
1. **静态注册**：工具在构造时硬编码，无法热插拔
2. **单一路由**：一个工具名对应一个执行器，无参数感知能力
3. **违反开闭原则**：新增工具需要修改现有类

---

## 2. 策略者模式方案设计

### 2.1 核心概念

**策略者模式（Strategy Pattern）**：定义一系列算法（策略），使它们可以互相替换，且算法的变化不会影响使用算法的客户端。

### 2.2 应用于工具路由的设计

```
ToolDispatcher（上下文）
    ↓ 根据参数特征选择策略
ToolStrategy（策略接口）
    ├── GenerationStrategy（生成策略）
    ├── ExpansionStrategy（扩展策略）
    ├── SummaryStrategy（摘要策略）
    └── CustomStrategy（自定义策略）
         ↓ 委托给具体执行器
ToolExecutor（执行器）
```

### 2.3 关键接口定义

#### 策略接口

```java
/**
 * 工具执行策略接口
 */
public interface ToolExecutionStrategy {
    
    /**
     * 判断是否支持该请求
     * @param toolName 工具名称
     * @param parameters 请求参数
     * @return true 如果此策略可以处理该请求
     */
    boolean supports(String toolName, Map<String, Object> parameters);
    
    /**
     * 执行工具调用
     * @param toolName 工具名称
     * @param parameters 请求参数
     * @return 执行结果
     */
    Object execute(String toolName, Map<String, Object> parameters);
    
    /**
     * 策略优先级（数字越小优先级越高）
     */
    int getPriority();
    
    /**
     * 策略名称（用于日志和调试）
     */
    String getStrategyName();
}
```

#### 策略调度器

```java
/**
 * 工具策略调度器
 */
public class ToolStrategyDispatcher {
    
    private final List<ToolExecutionStrategy> strategies;
    
    public ToolStrategyDispatcher(List<ToolExecutionStrategy> strategies) {
        // 按优先级排序
        this.strategies = strategies.stream()
            .sorted(Comparator.comparingInt(ToolExecutionStrategy::getPriority))
            .collect(Collectors.toList());
    }
    
    /**
     * 分派工具调用到合适的策略
     */
    public Object dispatch(String toolName, Map<String, Object> parameters) {
        for (ToolExecutionStrategy strategy : strategies) {
            if (strategy.supports(toolName, parameters)) {
                LogUtils.info("Using strategy: " + strategy.getStrategyName());
                return strategy.execute(toolName, parameters);
            }
        }
        throw new IllegalArgumentException("No strategy found for tool: " + toolName);
    }
    
    /**
     * 注册新策略（支持运行时动态注册）
     */
    public void registerStrategy(ToolExecutionStrategy strategy) {
        strategies.add(strategy);
        strategies.sort(Comparator.comparingInt(ToolExecutionStrategy::getPriority));
    }
}
```

---

## 3. 具体策略实现示例

### 3.1 思维导图生成策略

```java
/**
 * 思维导图生成策略
 * 根据参数特征选择不同的生成方式
 */
public class MindMapGenerationStrategy implements ToolExecutionStrategy {
    
    private final AIToolSet toolSet;
    
    @Override
    public boolean supports(String toolName, Map<String, Object> parameters) {
        if (!"createNodes".equals(toolName)) return false;
        
        // 检查是否是思维导图生成场景
        String userSummary = (String) parameters.get("userSummary");
        return userSummary != null && 
               (userSummary.contains("生成") || 
                userSummary.contains("创建") ||
                userSummary.contains("generate"));
    }
    
    @Override
    public Object execute(String toolName, Map<String, Object> parameters) {
        // 根据参数选择生成模式
        String mode = determineGenerationMode(parameters);
        
        switch (mode) {
            case "full":
                return executeFullGeneration(parameters);
            case "incremental":
                return executeIncrementalGeneration(parameters);
            case "template-based":
                return executeTemplateBasedGeneration(parameters);
            default:
                return executeFullGeneration(parameters);
        }
    }
    
    private String determineGenerationMode(Map<String, Object> params) {
        // 根据节点数量、深度等参数决定生成模式
        Integer nodeCount = (Integer) params.get("expectedNodeCount");
        Integer maxDepth = (Integer) params.get("maxDepth");
        
        if (nodeCount != null && nodeCount > 50) {
            return "incremental"; // 大量节点使用增量生成
        }
        if (maxDepth != null && maxDepth <= 2) {
            return "template-based"; // 浅层使用模板
        }
        return "full"; // 默认完整生成
    }
    
    @Override
    public int getPriority() {
        return 10; // 较高优先级
    }
    
    @Override
    public String getStrategyName() {
        return "MindMapGenerationStrategy";
    }
}
```

### 3.2 节点展开策略

```java
/**
 * 节点展开策略
 * 根据上下文选择展开方式
 */
public class NodeExpansionStrategy implements ToolExecutionStrategy {
    
    @Override
    public boolean supports(String toolName, Map<String, Object> parameters) {
        if (!"createNodes".equals(toolName)) return false;
        
        // 检查是否是展开场景（有父节点引用）
        Map<String, Object> anchorPlacement = (Map<String, Object>) parameters.get("anchorPlacement");
        return anchorPlacement != null && 
               "CHILDREN".equals(anchorPlacement.get("placementMode"));
    }
    
    @Override
    public Object execute(String toolName, Map<String, Object> parameters) {
        // 展开逻辑：读取父节点内容 → 生成子节点 → 插入
        String parentNodeId = extractParentNodeId(parameters);
        String focus = (String) parameters.get("focus");
        
        // 策略分支：根据展开深度选择算法
        Integer depth = (Integer) parameters.get("depth");
        if (depth != null && depth > 3) {
            return executeDeepExpansion(parentNodeId, focus, depth);
        } else {
            return executeShallowExpansion(parentNodeId, focus, depth);
        }
    }
    
    @Override
    public int getPriority() {
        return 20;
    }
    
    @Override
    public String getStrategyName() {
        return "NodeExpansionStrategy";
    }
}
```

### 3.3 批量编辑策略

```java
/**
 * 批量编辑策略
 * 根据编辑项数量选择不同的处理方式
 */
public class BatchEditStrategy implements ToolExecutionStrategy {
    
    private static final int BATCH_THRESHOLD = 10; // 批量阈值
    
    @Override
    public boolean supports(String toolName, Map<String, Object> parameters) {
        if (!"edit".equals(toolName)) return false;
        
        List<Map<String, Object>> items = (List<Map<String, Object>>) parameters.get("items");
        return items != null && items.size() > BATCH_THRESHOLD;
    }
    
    @Override
    public Object execute(String toolName, Map<String, Object> parameters) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) parameters.get("items");
        
        // 批量优化：分组执行、事务处理
        List<List<Map<String, Object>>> batches = partitionItems(items);
        
        for (List<Map<String, Object>> batch : batches) {
            executeBatch(batch);
        }
        
        return createBatchResult(items.size());
    }
    
    @Override
    public int getPriority() {
        return 30;
    }
    
    @Override
    public String getStrategyName() {
        return "BatchEditStrategy";
    }
}
```

---

## 4. 实施路径

### 4.1 阶段一：核心基础设施（1-2 天）

**目标**：建立策略者模式基础框架

**任务清单**：
1. ✅ 创建 `ToolExecutionStrategy` 接口
2. ✅ 创建 `ToolStrategyDispatcher` 调度器
3. ✅ 创建 `AbstractToolStrategy` 抽象基类（提供公共逻辑）
4. ✅ 编写单元测试验证策略选择逻辑

**文件结构**：
```
freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/strategy/
├── ToolExecutionStrategy.java
├── ToolStrategyDispatcher.java
├── AbstractToolStrategy.java
└── StrategyPriority.java（优先级常量）
```

### 4.2 阶段二：策略迁移（2-3 天）

**目标**：将现有工具执行器迁移为策略实现

**任务清单**：
1. 将 `DefaultToolExecutionService` 中的 5 个执行器改造为策略：
   - `ReadNodesStrategy`
   - `FetchNodesStrategy`
   - `SelectionStrategy`
   - `CreateNodesStrategy`（包含生成、展开等子策略）
   - `EditStrategy`（包含批量编辑子策略）

2. 保持向后兼容：
   - 保留 `ToolExecutionService` 接口
   - 内部使用策略调度器
   - 对外 API 不变

**迁移示例**：
```java
// 改造前
toolExecutors.put("createNodes", this::executeCreateNodes);

// 改造后
dispatcher.registerStrategy(new CreateNodesStrategy(toolSet));
```

### 4.3 阶段三：智能路由增强（2-3 天）

**目标**：实现基于参数特征的动态路由

**任务清单**：
1. 实现参数特征提取器：
   ```java
   public class ParameterFeatureExtractor {
       public static Set<Feature> extract(Map<String, Object> params) {
           // 提取：节点数量、深度、操作类型、内容大小等
       }
   }
   ```

2. 创建策略选择规则引擎：
   ```java
   public class StrategyRuleEngine {
       public ToolExecutionStrategy select(String toolName, Set<Feature> features) {
           // 基于规则匹配最佳策略
       }
   }
   ```

3. 实现策略变体：
   - `FastCreateStrategy`（快速创建，忽略验证）
   - `SafeCreateStrategy`（安全创建，完整验证）
   - `BatchEditStrategy`（批量编辑）
   - `AtomicEditStrategy`（原子编辑，支持回滚）

### 4.4 阶段四：性能优化与监控（1-2 天）

**目标**：添加策略执行监控和性能分析

**任务清单**：
1. 添加策略执行指标：
   ```java
   public class StrategyMetrics {
       private Map<String, Long> executionTimes;
       private Map<String, Integer> invocationCounts;
       private Map<String, Integer> errorCounts;
   }
   ```

2. 集成到现有观察者系统：
   ```java
   // 在 ObservableToolExecutor 中添加策略信息
   ToolExecutionBeforeEvent.create(
       toolName, arguments, caller, strategyName);
   ```

3. 提供策略选择日志：
   ```
   INFO: Strategy selected: MindMapGenerationStrategy (priority=10)
         for tool=createNodes, features=[large-scale, deep-hierarchy]
   ```

---

## 5. 可行性评估

### 5.1 技术可行性：✅ 高

| 评估维度 | 评分 | 说明 |
|---------|------|------|
| **技术成熟度** | ⭐⭐⭐⭐⭐ | 策略者模式是经典 GoF 设计模式，广泛应用于各类框架 |
| **框架兼容性** | ⭐⭐⭐⭐⭐ | 与现有 LangChain4j 工具系统完全兼容，不冲突 |
| **实现复杂度** | ⭐⭐⭐⭐ | 中等复杂度，主要是接口定义和策略拆分 |
| **测试覆盖** | ⭐⭐⭐⭐ | 可独立测试每个策略，单元测试友好 |
| **性能影响** | ⭐⭐⭐⭐⭐ | 策略选择是 O(n) 线性扫描，n 通常 < 10，性能损耗 < 1ms |

### 5.2 业务价值：✅ 高

| 价值维度 | 当前架构 | 策略者模式 | 提升 |
|---------|---------|-----------|------|
| **扩展性** | 修改现有类 | 新增策略类 | ⬆️ 开闭原则 |
| **可测试性** | 集成测试为主 | 独立单元测试 | ⬆️ 测试覆盖率 |
| **可维护性** | 硬编码路由 | 声明式策略 | ⬆️ 代码清晰度 |
| **动态性** | 启动时固定 | 运行时可注册 | ⬆️ 热插拔 |
| **参数感知** | 无 | 基于参数路由 | ⬆️ 智能路由 |

### 5.3 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| **策略选择冲突** | 中 | 中 | 优先级机制 + 明确的 supports() 条件 |
| **性能下降** | 低 | 低 | 策略数量少（< 20），线性扫描极快 |
| **迁移成本** | 中 | 中 | 分阶段迁移，保持向后兼容 |
| **学习曲线** | 低 | 低 | 策略者模式是基础设计模式，文档丰富 |

---

## 6. 与现有架构的集成

### 6.1 与 MindMapBufferLayer 的集成

```java
// 当前
public class MindMapBufferLayer implements BufferLayer {
    private MindMapPromptOptimizer promptOptimizer;
    private MindMapModelRouter modelRouter;
    // ❌ 工具调用硬编码在 callAI() 中
    
    private String callAI(String prompt, String model) {
        // 硬编码的 AI 调用逻辑
        return "模拟结果"; // TODO: 调用 LangChain4j
    }
}

// 改造后
public class MindMapBufferLayer implements BufferLayer {
    private MindMapPromptOptimizer promptOptimizer;
    private MindMapModelRouter modelRouter;
    private ToolStrategyDispatcher toolDispatcher; // ✅ 新增
    
    private String callAI(String prompt, String model) {
        // ✅ 使用策略调度器
        Map<String, Object> params = Map.of(
            "prompt", prompt,
            "model", model,
            "requestType", currentRequest.getRequestType()
        );
        return (String) toolDispatcher.dispatch("aiCall", params);
    }
}
```

### 6.2 与 ToolExecutorFactory 的关系

**现状**：
- `ToolExecutorFactory` 基于反射创建 `DefaultToolExecutor`
- 通过 `@Tool` 注解识别工具方法
- 适用于 LangChain4j 的工具注册

**策略者模式的定位**：
- **互补而非替代**：策略者模式用于**工具调用前**的路由决策
- `ToolExecutorFactory` 仍然负责创建底层执行器
- 策略者模式在 `ModelContextProtocolToolDispatcher` 之前介入

**集成架构**：
```
用户请求 
  → ToolStrategyDispatcher（策略路由）
  → ToolExecutorFactory（创建执行器）
  → ModelContextProtocolToolDispatcher（MCP 分发）
  → DefaultToolExecutor（实际执行）
```

---

## 7. 代码示例：完整集成

### 7.1 策略注册配置

```java
/**
 * AI 插件激活器中注册策略
 */
public class AIPluginActivator implements BundleActivator {
    
    @Override
    public void start(BundleContext context) {
        // 创建策略列表
        List<ToolExecutionStrategy> strategies = Arrays.asList(
            new MindMapGenerationStrategy(toolSet),
            new NodeExpansionStrategy(toolSet),
            new BranchSummaryStrategy(toolSet),
            new BatchEditStrategy(toolSet),
            new DefaultToolStrategy(toolSet) // 兜底策略
        );
        
        // 创建调度器
        ToolStrategyDispatcher dispatcher = new ToolStrategyDispatcher(strategies);
        
        // 注册到服务
        context.registerService(ToolStrategyDispatcher.class, dispatcher, null);
    }
}
```

### 7.2 在 DefaultToolExecutionService 中使用

```java
public class DefaultToolExecutionService implements ToolExecutionService {
    
    private ToolStrategyDispatcher strategyDispatcher;
    
    @Override
    public Object executeTool(String toolName, Map<String, Object> parameters) {
        // ✅ 使用策略调度器而非硬编码 Map
        return strategyDispatcher.dispatch(toolName, parameters);
    }
    
    public void setStrategyDispatcher(ToolStrategyDispatcher dispatcher) {
        this.strategyDispatcher = dispatcher;
    }
}
```

---

## 8. 性能对比预估

| 场景 | 当前架构 | 策略者模式 | 差异 |
|------|---------|-----------|------|
| **工具路由时间** | < 0.1ms（HashMap 查找） | < 1ms（策略遍历） | +0.9ms |
| **内存占用** | 固定（5 个执行器） | 动态（策略数量可变） | +10-20% |
| **扩展新工具** | 修改代码 + 重新编译 | 新增策略类 + 注册 | 无需修改现有代码 |
| **运行时灵活性** | 无 | 支持热插拔 | 显著提升 |

**结论**：性能损耗可忽略不计（< 1ms），换取显著的架构优势。

---

## 9. 推荐实施方案

### 9.1 短期方案（1-2 周）

**最小可行改进（MVP）**：
1. 创建策略接口和调度器
2. 将 `createNodes` 工具拆分为 3 个策略：
   - `MindMapGenerationStrategy`
   - `NodeExpansionStrategy`
   - `DefaultCreateStrategy`
3. 编写单元测试验证策略选择

**预期收益**：
- 验证策略者模式可行性
- 为后续扩展奠定基础
- 不影响现有功能

### 9.2 中期方案（1 个月）

**全面迁移**：
1. 将所有 5 个工具迁移为策略
2. 实现参数特征提取器
3. 添加策略执行监控
4. 更新文档和示例

**预期收益**：
- 完整的策略者模式实现
- 提升代码可维护性
- 支持动态策略注册

### 9.3 长期方案（3 个月）

**智能路由引擎**：
1. 基于机器学习的策略选择
2. 策略自动优化（根据历史性能）
3. 策略组合（Pipeline 模式）
4. 策略热加载（无需重启）

**预期收益**：
- AI 驱动的策略选择
- 自适应优化
- 企业级可扩展性

---

## 10. 结论与建议

### ✅ 结论：**强烈推荐使用策略者模式**

**理由**：
1. **技术成熟**：经典设计模式，风险低
2. **价值显著**：显著提升扩展性和可维护性
3. **成本可控**：可分阶段实施，不影响现有功能
4. **性能无损**：< 1ms 的路由开销可忽略
5. **面向未来**：为 AI 驱动的智能路由奠定基础

### 📋 建议行动

1. **立即开始**：实施短期方案（1-2 周）
2. **保持兼容**：确保迁移过程不破坏现有 API
3. **充分测试**：每个策略独立测试，集成测试覆盖
4. **文档同步**：更新架构文档和开发指南
5. **性能监控**：添加策略执行指标，持续优化

### ⚠️ 注意事项

1. **避免过度设计**：初期保持简单，不要创建过多策略
2. **明确策略边界**：每个策略职责单一，避免交叉
3. **优先级管理**：合理设置优先级，避免策略冲突
4. **错误处理**：策略执行失败时提供清晰的错误信息

---

## 附录

### A. 参考实现

- **JDK 内置**：`java.util.Comparator`（策略者模式经典实现）
- **Spring Framework**：`HandlerInterceptor`（拦截器策略）
- **LangChain4j**：`ToolExecutor` 接口（现有工具执行器）

### B. 相关文档

- [GoF 设计模式 - 策略者模式](https://refactoring.guru/design-patterns/strategy)
- [LangChain4j 工具系统文档](file:///c:/Users/zengming/Desktop/free/freeplane-1.13.x/docs/langchain4j-guide.md)
- [AI 插件核心改进](file:///c:/Users/zengming/Desktop/free/freeplane-1.13.x/docs/ai-plugin-core-improvements.md)

### C. 术语表

| 术语 | 说明 |
|------|------|
| **策略者模式** | Strategy Pattern，定义算法族并使其可互相替换 |
| **策略路由** | 根据参数特征选择最合适的执行策略 |
| **热插拔** | 运行时动态注册/注销策略，无需重启 |
| **开闭原则** | 对扩展开放，对修改关闭（SOLID 原则之一） |

---

**文档版本**: v1.0  
**最后更新**: 2026-04-26  
**维护者**: AI 插件开发团队
