# 测试开发阶段策略实现方案

> **文档目标**: 明确在测试开发阶段可以实现的策略类型、实现方法和验证标准  
> **适用阶段**: 策略者模式 MVP 验证（1-2 周）  
> **更新日期**: 2026-04-26

---

## 1. 测试阶段策略设计原则

### 1.1 核心原则

| 原则 | 说明 | 示例 |
|------|------|------|
| **最小可行** | 优先实现最能体现策略价值的场景 | 参数路由、模式选择 |
| **独立测试** | 每个策略可独立单元测试 | Mock 依赖，验证逻辑 |
| **向后兼容** | 不破坏现有 API | 保留 `ToolExecutionService` 接口 |
| **渐进式** | 从简单到复杂，逐步验证 | 先路由，后优化 |

### 1.2 测试范围界定

```
✅ 可以测试：
  - 策略选择逻辑（基于参数特征）
  - 策略执行路径（不同分支）
  - 策略优先级（冲突解决）
  - 策略注册/注销（动态性）
  - 性能对比（路由时间）

❌ 暂不测试：
  - 生产级 AI 调用（需要 API Key）
  - 大规模性能压测（需要真实数据）
  - 策略热加载（需要 OSGi 动态部署）
  - 机器学习策略选择（需要训练数据）
```

---

## 2. 可实现的策略清单

### 2.1 P0 优先级：核心路由策略（必须实现）

#### 策略 1: 思维导图生成策略（MindMapGenerationStrategy）

**测试场景**：
```java
@Test
public void strategy_selectGenerationMode_byNodeCount() {
    // 给定：不同节点数量的请求
    Map<String, Object> smallRequest = Map.of(
        "expectedNodeCount", 10,
        "userSummary", "生成思维导图"
    );
    Map<String, Object> largeRequest = Map.of(
        "expectedNodeCount", 100,
        "userSummary", "生成思维导图"
    );
    
    // 当：策略选择
    ToolExecutionStrategy smallStrategy = dispatcher.selectStrategy("createNodes", smallRequest);
    ToolExecutionStrategy largeStrategy = dispatcher.selectStrategy("createNodes", largeRequest);
    
    // 则：应选择不同的生成模式
    assertThat(smallStrategy).isInstanceOf(FullGenerationStrategy.class);
    assertThat(largeStrategy).isInstanceOf(IncrementalGenerationStrategy.class);
}
```

**可实现的模式分支**：

| 参数条件 | 选择策略 | 测试方法 |
|---------|---------|---------|
| `expectedNodeCount <= 20` | `FullGenerationStrategy` | 验证一次性生成 |
| `expectedNodeCount > 50` | `IncrementalGenerationStrategy` | 验证分批生成 |
| `maxDepth <= 2` | `TemplateGenerationStrategy` | 验证模板匹配 |
| 其他 | `DefaultGenerationStrategy` | 验证兜底逻辑 |

**测试覆盖目标**：
- ✅ 策略选择逻辑（4 个分支）
- ✅ 参数特征提取（节点数、深度）
- ✅ 优先级冲突解决（多个策略匹配时）

---

#### 策略 2: 节点展开策略（NodeExpansionStrategy）

**测试场景**：
```java
@Test
public void strategy_selectExpansionMode_byDepth() {
    // 给定：不同展开深度的请求
    Map<String, Object> shallowRequest = Map.of(
        "anchorPlacement", Map.of("placementMode", "CHILDREN"),
        "depth", 2
    );
    Map<String, Object> deepRequest = Map.of(
        "anchorPlacement", Map.of("placementMode", "CHILDREN"),
        "depth", 5
    );
    
    // 当：策略选择
    ToolExecutionStrategy shallowStrategy = dispatcher.selectStrategy("createNodes", shallowRequest);
    ToolExecutionStrategy deepStrategy = dispatcher.selectStrategy("createNodes", deepRequest);
    
    // 则：应选择不同的展开模式
    assertThat(shallowStrategy).isInstanceOf(ShallowExpansionStrategy.class);
    assertThat(deepStrategy).isInstanceOf(DeepExpansionStrategy.class);
}
```

**可实现的模式分支**：

| 参数条件 | 选择策略 | 测试验证点 |
|---------|---------|-----------|
| `depth <= 3` | `ShallowExpansionStrategy` | 单层展开，快速返回 |
| `depth > 3` | `DeepExpansionStrategy` | 多层展开，递归生成 |
| `focus != null` | `FocusedExpansionStrategy` | 聚焦特定主题 |
| `placementMode != CHILDREN` | 不支持 | `supports()` 返回 false |

**测试覆盖目标**：
- ✅ 展开深度路由（浅层 vs 深层）
- ✅ 聚焦主题识别
- ✅ 锚点模式验证

---

#### 策略 3: 批量编辑策略（BatchEditStrategy）

**测试场景**：
```java
@Test
public void strategy_selectEditMode_byItemCount() {
    // 给定：不同编辑项数量的请求
    Map<String, Object> smallEdit = Map.of(
        "items", Arrays.asList(Map.of("nodeId", "1"), Map.of("nodeId", "2"))
    );
    Map<String, Object> largeEdit = Map.of(
        "items", IntStream.range(0, 20)
            .mapToObj(i -> Map.of("nodeId", String.valueOf(i)))
            .collect(Collectors.toList())
    );
    
    // 当：策略选择
    ToolExecutionStrategy smallStrategy = dispatcher.selectStrategy("edit", smallEdit);
    ToolExecutionStrategy largeStrategy = dispatcher.selectStrategy("edit", largeEdit);
    
    // 则：应选择不同的编辑模式
    assertThat(smallStrategy).isInstanceOf(AtomicEditStrategy.class);
    assertThat(largeStrategy).isInstanceOf(BatchEditStrategy.class);
}
```

**可实现的模式分支**：

| 参数条件 | 选择策略 | 测试验证点 |
|---------|---------|-----------|
| `items.size() <= 10` | `AtomicEditStrategy` | 逐条执行，支持回滚 |
| `items.size() > 10` | `BatchEditStrategy` | 分组执行，事务处理 |
| `items.size() > 50` | `ParallelBatchEditStrategy` | 并行处理（可选） |

**测试覆盖目标**：
- ✅ 批量阈值判断
- ✅ 分组逻辑验证
- ✅ 性能对比（批量 vs 原子）

---

### 2.2 P1 优先级：辅助策略（推荐实现）

#### 策略 4: 读取优化策略（ReadOptimizationStrategy）

**测试场景**：
```java
@Test
public void strategy_selectReadMode_byDepth() {
    // 给定：不同读取深度的请求
    Map<String, Object> shallowRead = Map.of(
        "fullContentDepth", 1,
        "summaryDepth", 2
    );
    Map<String, Object> deepRead = Map.of(
        "fullContentDepth", 5,
        "summaryDepth", 3
    );
    
    // 当：策略选择
    ToolExecutionStrategy shallowStrategy = dispatcher.selectStrategy("readNodesWithDescendants", shallowRead);
    ToolExecutionStrategy deepStrategy = dispatcher.selectStrategy("readNodesWithDescendants", deepRead);
    
    // 则：应选择不同的读取模式
    assertThat(shallowStrategy).isInstanceOf(LazyReadStrategy.class);
    assertThat(deepStrategy).isInstanceOf(EagerReadStrategy.class);
}
```

**可实现的模式分支**：

| 参数条件 | 选择策略 | 说明 |
|---------|---------|------|
| `fullContentDepth <= 2` | `LazyReadStrategy` | 延迟加载，按需读取 |
| `fullContentDepth > 2` | `EagerReadStrategy` | 预加载，一次性读取 |
| `maximumTotalTextCharacters < 1000` | `CompactReadStrategy` | 紧凑格式，节省带宽 |

---

#### 策略 5: 容错降级策略（FallbackStrategy）

**测试场景**：
```java
@Test
public void strategy_fallback_onError() {
    // 给定：会失败的主策略
    ToolExecutionStrategy failingStrategy = new FailingTestStrategy();
    dispatcher.registerStrategy(failingStrategy);
    
    // 当：主策略执行失败
    try {
        dispatcher.dispatch("testTool", Map.of("shouldFail", true));
    } catch (Exception e) {
        // 则：应触发降级策略
        ToolExecutionStrategy fallback = dispatcher.getFallbackStrategy();
        assertThat(fallback).isInstanceOf(DefaultFallbackStrategy.class);
    }
}
```

**可实现的降级路径**：

| 主策略失败 | 降级策略 | 测试验证点 |
|-----------|---------|-----------|
| AI 调用超时 | `CachedResponseStrategy` | 返回缓存结果 |
| 参数验证失败 | `ParameterCorrectionStrategy` | 自动修正参数 |
| 工具不存在 | `DefaultToolStrategy` | 返回友好错误 |

---

### 2.3 P2 优先级：实验性策略（可选实现）

#### 策略 6: 性能监控策略（PerformanceMonitoringStrategy）

**测试场景**：
```java
@Test
public void strategy_collectExecutionMetrics() {
    // 给定：带监控的策略
    ToolExecutionStrategy monitored = new MonitoredStrategy(new FastStrategy());
    
    // 当：执行 10 次
    for (int i = 0; i < 10; i++) {
        monitored.execute("testTool", Map.of());
    }
    
    // 则：应收集到指标
    StrategyMetrics metrics = monitored.getMetrics();
    assertThat(metrics.getInvocationCount()).isEqualTo(10);
    assertThat(metrics.getAverageExecutionTime()).isGreaterThan(0);
    assertThat(metrics.getErrorCount()).isEqualTo(0);
}
```

---

## 3. 测试用例完整清单

### 3.1 单元测试（必须覆盖）

| 测试类 | 测试方法 | 验证目标 | 优先级 |
|-------|---------|---------|--------|
| `ToolStrategyDispatcherTest` | `dispatch_shouldSelectCorrectStrategy` | 策略选择逻辑 | P0 |
| `ToolStrategyDispatcherTest` | `dispatch_noMatchingStrategy_shouldThrow` | 无匹配策略异常 | P0 |
| `ToolStrategyDispatcherTest` | `registerStrategy_shouldMaintainPriorityOrder` | 优先级排序 | P0 |
| `MindMapGenerationStrategyTest` | `supports_mindmapKeywords_shouldReturnTrue` | 关键词识别 | P0 |
| `MindMapGenerationStrategyTest` | `execute_smallNodeCount_shouldUseFullGeneration` | 小规模生成 | P0 |
| `MindMapGenerationStrategyTest` | `execute_largeNodeCount_shouldUseIncremental` | 大规模生成 | P0 |
| `NodeExpansionStrategyTest` | `supports_childrenMode_shouldReturnTrue` | 展开模式识别 | P0 |
| `NodeExpansionStrategyTest` | `execute_shallowDepth_shouldUseShallowExpansion` | 浅层展开 | P0 |
| `NodeExpansionStrategyTest` | `execute_deepDepth_shouldUseDeepExpansion` | 深层展开 | P0 |
| `BatchEditStrategyTest` | `supports_largeItemCount_shouldReturnTrue` | 批量识别 | P1 |
| `BatchEditStrategyTest` | `execute_shouldPartitionItems` | 分组逻辑 | P1 |
| `FallbackStrategyTest` | `fallback_onError_shouldInvoke` | 降级触发 | P1 |

**预期测试数量**：12-15 个单元测试

---

### 3.2 集成测试（推荐覆盖）

| 测试类 | 测试方法 | 验证目标 | 优先级 |
|-------|---------|---------|--------|
| `StrategyIntegrationTest` | `fullFlow_mindmapGeneration_shouldSelectStrategy` | 完整生成流程 | P0 |
| `StrategyIntegrationTest` | `fullFlow_nodeExpansion_shouldSelectStrategy` | 完整展开流程 | P0 |
| `StrategyIntegrationTest` | `fullFlow_batchEdit_shouldSelectStrategy` | 完整编辑流程 | P1 |
| `StrategyCompatibilityTest` | `existingTools_shouldStillWork` | 向后兼容性 | P0 |

**预期测试数量**：4-6 个集成测试

---

### 3.3 性能测试（可选覆盖）

| 测试类 | 测试方法 | 验证目标 | 优先级 |
|-------|---------|---------|--------|
| `StrategyPerformanceTest` | `dispatch_routingTime_shouldBeLessThan1ms` | 路由性能 | P2 |
| `StrategyPerformanceTest` | `batchVsAtomic_editPerformance_shouldCompare` | 批量性能优势 | P2 |

**预期测试数量**：2-3 个性能测试

---

## 4. 实施步骤详细计划

### 第 1 天：基础设施搭建

**任务清单**：
- [ ] 创建 `ToolExecutionStrategy` 接口
- [ ] 创建 `ToolStrategyDispatcher` 调度器
- [ ] 创建 `AbstractToolStrategy` 抽象基类
- [ ] 编写 `ToolStrategyDispatcherTest`（3 个测试）

**代码结构**：
```
freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/strategy/
├── ToolExecutionStrategy.java          # 策略接口
├── ToolStrategyDispatcher.java         # 调度器
├── AbstractToolStrategy.java           # 抽象基类
└── StrategyPriority.java              # 优先级常量

freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/strategy/
└── ToolStrategyDispatcherTest.java    # 调度器测试
```

**验收标准**：
- ✅ 调度器可注册策略
- ✅ 策略按优先级排序
- ✅ 单元测试全部通过

---

### 第 2-3 天：P0 策略实现

**任务清单**：
- [ ] 实现 `MindMapGenerationStrategy`（含 3 个子策略）
- [ ] 实现 `NodeExpansionStrategy`（含 2 个子策略）
- [ ] 实现 `BatchEditStrategy`
- [ ] 编写对应单元测试（9 个测试）

**子策略实现**：
```
MindMapGenerationStrategy
├── FullGenerationStrategy          # 小规模完整生成
├── IncrementalGenerationStrategy  # 大规模增量生成
└── TemplateGenerationStrategy     # 模板匹配生成

NodeExpansionStrategy
├── ShallowExpansionStrategy       # 浅层展开（depth <= 3）
└── DeepExpansionStrategy          # 深层展开（depth > 3）

BatchEditStrategy
├── AtomicEditStrategy             # 原子编辑（<= 10 项）
└── BatchEditStrategy              # 批量编辑（> 10 项）
```

**验收标准**：
- ✅ 每个策略可独立测试
- ✅ 策略选择逻辑正确
- ✅ 单元测试覆盖率 > 80%

---

### 第 4 天：P1 策略实现

**任务清单**：
- [ ] 实现 `ReadOptimizationStrategy`（含 2 个子策略）
- [ ] 实现 `FallbackStrategy`
- [ ] 编写对应单元测试（4 个测试）

**验收标准**：
- ✅ 读取优化策略可选择
- ✅ 降级策略可触发
- ✅ 单元测试全部通过

---

### 第 5 天：集成测试与兼容性验证

**任务清单**：
- [ ] 编写 `StrategyIntegrationTest`（4 个测试）
- [ ] 编写 `StrategyCompatibilityTest`（验证向后兼容）
- [ ] 修复发现的问题
- [ ] 运行全量测试套件

**验收标准**：
- ✅ 完整流程测试通过
- ✅ 现有测试不受影响
- ✅ 无破坏性变更

---

### 第 6-7 天：性能测试与文档

**任务清单**：
- [ ] 编写 `StrategyPerformanceTest`（2 个测试）
- [ ] 收集性能数据（路由时间、批量优势）
- [ ] 更新架构文档
- [ ] 编写策略使用指南

**验收标准**：
- ✅ 路由时间 < 1ms
- ✅ 批量编辑性能优势 > 30%
- ✅ 文档完整

---

## 5. 测试数据准备

### 5.1 Mock 数据生成器

```java
/**
 * 测试数据工厂
 */
public class StrategyTestDataFactory {
    
    public static Map<String, Object> createSmallMindMapRequest() {
        return Map.of(
            "expectedNodeCount", 10,
            "maxDepth", 2,
            "userSummary", "生成思维导图"
        );
    }
    
    public static Map<String, Object> createLargeMindMapRequest() {
        return Map.of(
            "expectedNodeCount", 100,
            "maxDepth", 5,
            "userSummary", "生成思维导图"
        );
    }
    
    public static Map<String, Object> createShallowExpansionRequest() {
        return Map.of(
            "anchorPlacement", Map.of("placementMode", "CHILDREN"),
            "depth", 2,
            "focus", "核心概念"
        );
    }
    
    public static Map<String, Object> createDeepExpansionRequest() {
        return Map.of(
            "anchorPlacement", Map.of("placementMode", "CHILDREN"),
            "depth", 5,
            "focus", "设计模式"
        );
    }
    
    public static Map<String, Object> createSmallEditRequest(int itemCount) {
        List<Map<String, Object>> items = IntStream.range(0, itemCount)
            .mapToObj(i -> Map.of("nodeIdentifier", "ID_" + i))
            .collect(Collectors.toList());
        return Map.of("items", items);
    }
}
```

---

### 5.2 Mock 策略实现

```java
/**
 * 用于测试的 Mock 策略
 */
public class MockTestStrategy extends AbstractToolStrategy {
    
    private final String name;
    private final boolean shouldSupport;
    private final Object mockResult;
    
    public MockTestStrategy(String name, boolean shouldSupport, Object mockResult) {
        this.name = name;
        this.shouldSupport = shouldSupport;
        this.mockResult = mockResult;
    }
    
    @Override
    public boolean supports(String toolName, Map<String, Object> parameters) {
        return shouldSupport;
    }
    
    @Override
    public Object execute(String toolName, Map<String, Object> parameters) {
        return mockResult;
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getStrategyName() {
        return name;
    }
}
```

---

## 6. 验证标准与验收条件

### 6.1 功能验证

| 验证项 | 标准 | 测试方法 |
|-------|------|---------|
| **策略选择正确性** | 100% 匹配预期策略 | 单元测试覆盖所有分支 |
| **优先级冲突解决** | 高优先级策略优先 | 注册多个匹配策略，验证选择 |
| **参数特征提取** | 准确识别关键参数 | 边界值测试 |
| **降级策略触发** | 失败时自动降级 | 注入异常，验证降级路径 |
| **向后兼容性** | 现有测试不受影响 | 运行全量测试套件 |

---

### 6.2 性能验证

| 验证项 | 标准 | 测试方法 |
|-------|------|---------|
| **策略路由时间** | < 1ms | `StrategyPerformanceTest` |
| **批量编辑优势** | > 30% 性能提升 | 对比批量 vs 原子编辑 |
| **内存占用增长** | < 20% | JVM 内存分析 |
| **测试执行时间** | < 30 秒（全量） | CI 计时 |

---

### 6.3 代码质量验证

| 验证项 | 标准 | 检查方法 |
|-------|------|---------|
| **单元测试覆盖率** | > 80% | JaCoCo 报告 |
| **代码重复率** | < 5% | SonarQube |
| **编译警告** | 0 | `gradle compileJava` |
| **代码规范** | 符合项目规范 | Checkstyle |

---

## 7. 预期产出物

### 7.1 代码产出

| 文件 | 行数预估 | 说明 |
|------|---------|------|
| `ToolExecutionStrategy.java` | 30 | 策略接口 |
| `ToolStrategyDispatcher.java` | 80 | 调度器 |
| `AbstractToolStrategy.java` | 50 | 抽象基类 |
| `MindMapGenerationStrategy.java` | 120 | 生成策略 |
| `NodeExpansionStrategy.java` | 100 | 展开策略 |
| `BatchEditStrategy.java` | 90 | 批量编辑策略 |
| `ReadOptimizationStrategy.java` | 80 | 读取优化策略 |
| `FallbackStrategy.java` | 60 | 降级策略 |
| **总计** | **~610 行** | 核心代码 |

---

### 7.2 测试产出

| 文件 | 测试数量 | 说明 |
|------|---------|------|
| `ToolStrategyDispatcherTest.java` | 3 | 调度器测试 |
| `MindMapGenerationStrategyTest.java` | 4 | 生成策略测试 |
| `NodeExpansionStrategyTest.java` | 3 | 展开策略测试 |
| `BatchEditStrategyTest.java` | 2 | 批量编辑测试 |
| `ReadOptimizationStrategyTest.java` | 2 | 读取优化测试 |
| `FallbackStrategyTest.java` | 2 | 降级策略测试 |
| `StrategyIntegrationTest.java` | 4 | 集成测试 |
| `StrategyCompatibilityTest.java` | 2 | 兼容性测试 |
| `StrategyPerformanceTest.java` | 2 | 性能测试 |
| **总计** | **24 个测试** | 完整覆盖 |

---

### 7.3 文档产出

| 文档 | 内容 |
|------|------|
| `strategy-pattern-tool-routing-feasibility.md` | 可行性分析（已完成） |
| `strategy-pattern-implementation-guide.md` | 实施指南（待创建） |
| `strategy-pattern-api-reference.md` | API 参考文档（待创建） |
| 代码注释 | 每个策略类包含 JavaDoc |

---

## 8. 风险与缓解措施

### 8.1 技术风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| **策略选择冲突** | 中 | 中 | 明确 `supports()` 条件，添加单元测试 |
| **优先级设置不当** | 低 | 中 | 定义 `StrategyPriority` 常量，文档说明 |
| **性能下降** | 低 | 低 | 性能测试验证，策略数量 < 20 |
| **测试覆盖不足** | 中 | 高 | 设定覆盖率目标 > 80%，代码审查 |

---

### 8.2 进度风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| **策略实现复杂度高** | 中 | 中 | 先实现核心逻辑，后续优化 |
| **测试数据准备耗时** | 低 | 低 | 使用 Mock 数据工厂，复用测试数据 |
| **兼容性问题** | 低 | 高 | 保留现有 API，逐步迁移 |

---

## 9. 成功指标

### 9.1 短期指标（1-2 周）

- ✅ 完成 3 个 P0 策略实现
- ✅ 24 个测试全部通过
- ✅ 单元测试覆盖率 > 80%
- ✅ 策略路由时间 < 1ms
- ✅ 向后兼容性验证通过

---

### 9.2 中期指标（1 个月）

- ✅ 完成所有策略迁移（5 个工具）
- ✅ 集成到 `DefaultToolExecutionService`
- ✅ 策略执行监控可用
- ✅ 文档完整

---

### 9.3 长期指标（3 个月）

- ✅ 支持策略热加载
- ✅ 策略性能自动优化
- ✅ AI 驱动的策略选择
- ✅ 企业级可扩展性

---

## 10. 附录

### A. 策略选择决策树

```
用户请求
  ↓
工具名称匹配？
  ├─ 是 → 进入策略选择
  │     ↓
  │   参数特征提取
  │     ↓
  │   遍历策略（按优先级）
  │     ├─ supports() 返回 true → 选择该策略
  │     └─ supports() 返回 false → 继续遍历
  │     ↓
  │   找到策略？
  │     ├─ 是 → 执行策略
  │     └─ 否 → 抛出异常
  └─ 否 → 不支持的工具
```

---

### B. 策略优先级建议

| 优先级范围 | 用途 | 示例 |
|-----------|------|------|
| 1-10 | 核心业务策略 | `MindMapGenerationStrategy` (10) |
| 11-20 | 辅助业务策略 | `NodeExpansionStrategy` (20) |
| 21-30 | 优化策略 | `BatchEditStrategy` (30) |
| 31-40 | 容错策略 | `FallbackStrategy` (40) |
| 90-100 | 兜底策略 | `DefaultToolStrategy` (100) |

---

### C. 测试执行命令

```bash
# 运行所有策略测试
gradle :freeplane_plugin_ai:test --tests "org.freeplane.plugin.ai.strategy.*"

# 运行单个测试类
gradle :freeplane_plugin_ai:test --tests "ToolStrategyDispatcherTest"

# 生成测试覆盖率报告
gradle :freeplane_plugin_ai:jacocoTestReport

# 查看测试报告
open freeplane_plugin_ai/build/reports/jacoco/test/html/index.html
```

---

**文档版本**: v1.0  
**最后更新**: 2026-04-26  
**维护者**: AI 插件开发团队
