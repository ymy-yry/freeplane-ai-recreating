# 思维导图环检测容器工具类演进全记录

> 文件路径：`freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/validation/`  
> 核心类：`MindMapGenerationValidator.java` → `SnakeDigestGraph.java`

---

## 一、背景与问题起点

### 1.1 为什么需要环检测

Freeplane AI 插件调用大语言模型（LLM）生成思维导图 JSON。LLM 偶发性地会生成包含**环形引用**的 JSON，例如：

```json
{
  "id": "A", "text": "根节点",
  "children": [
    {
      "id": "B", "text": "子节点B",
      "children": [
        { "id": "A", "text": "回指根节点" }
      ]
    }
  ]
}
```

若将此数据直接写入 `MindMapBufferLayer` 的步骤 6（`createMindMapNodes` 递归创建节点），会触发**无限递归死循环**，导致 JVM 栈溢出。

### 1.2 原始架构的断层

`MindMapBufferLayer.process()` 流程为：

```
步骤1 需求分析
步骤2 提示词优化
步骤3 模型选择
步骤4 调用 AI ←── 返回 JSON
       ↓
步骤5 结果优化       ← 没有结构检查！
       ↓
步骤6 递归创建节点   ← 有环时死循环崩溃
```

**降级链断层**：步骤4 拿到 AI 返回 JSON 后，直接进入步骤5，完全没有结构校验。

---

## 二、第一轮方案：BFS —— 尝试与放弃

### 2.1 BFS 的直觉吸引力

BFS（宽度优先搜索）的直觉来自其天然的"层次遍历"特性：思维导图本身就是按层展开的，BFS 似乎完美匹配。初始设计如下：

```
用 visited HashSet 记录已访问节点
Queue 中依次弹出节点，将其子节点加入 Queue
若 Queue 中弹出的节点已在 visited 中 → 发现环
```

### 2.2 BFS 无法检测有向环的根本原因

| 场景 | BFS 行为 | 实际结论 |
|------|----------|----------|
| 无向图回路 | visited 可以检测 | ✅ 有效 |
| 有向图环（回边） | visited 无法区分"已访问但在别的路径上"与"当前路径的祖先" | ❌ 误报 |

**具体反例：**

```
A → B → D
A → C → D   (D 被两条路径到达，但无环)
```

BFS 到达 D 的第二次时，`visited.contains("D") == true`，会**误报为有环**。

本质原因：**BFS 的 `visited` 集合只记录"到达过"，无法区分当前 DFS 递归栈中的祖先节点（GRAY）与已完全处理的节点（BLACK）**。思维导图 JSON 中，钻石形共享子节点（diamond shape）是合法的，BFS 无法正确处理。

### 2.3 BFS 方案放弃

BFS 方案被放弃，原因总结：
1. **误报率高**：共享子节点（多父节点引用同一子节点）会被错误标记为环
2. **无法区分回边**：BFS 的队列无法感知"当前递归路径"
3. **环路径难以重建**：BFS 无 parent 链结构，发现环后无法输出完整环路径

---

## 三、第二轮方案：双 HashSet DFS —— 初步可用

### 3.1 经典三色 DFS 思路

DFS 本质上通过**递归栈**天然维护了"当前路径"，这正是环检测的关键。经典实现使用**两个 HashSet**：

```java
Set<String> visited;       // 已访问（WHITE 已变 BLACK）
Set<String> recursionStack; // 当前 DFS 递归栈中（GRAY）

void dfs(String nodeId) {
    if (recursionStack.contains(nodeId)) {
        // 发现回边 → 有环
    }
    if (visited.contains(nodeId)) {
        return; // 已处理，剪枝
    }
    visited.add(nodeId);
    recursionStack.add(nodeId);
    for (String child : graph.getChildren(nodeId)) {
        dfs(child);
    }
    recursionStack.remove(nodeId); // 回溯
}
```

### 3.2 双 HashSet 的问题

| 问题 | 描述 |
|------|------|
| 每次状态判断需要 **2次** HashSet 查找 | `visited.contains()` + `recursionStack.contains()` |
| 回溯时 `recursionStack.remove()` 涉及 LinkedHashSet 链表节点维护 | 额外内存写操作 |
| 环路径收集需要维护一个 `List<String> cyclePath`，每步入栈出栈 | 每个节点都有入栈/出栈开销 |
| `GraphData` 私有内部类承载了数据容器+统计+算法 | 职责过重，411 行大类无法复用 |

### 3.3 内部类 GraphData 的问题

原始代码的 `GraphData` 是 `MindMapGenerationValidator` 的**私有内部类**：

```java
// 原始 MindMapGenerationValidator 内部
private static final class GraphData {
    String rootId;
    Map<String, List<String>> adjacency = new LinkedHashMap<>();
    Map<String, String>       labels    = new HashMap<>();
}
```

这个设计的问题：
- **无法独立测试**：私有内部类外部无法访问
- **职责不清**：数据容器 + 邻接表读写 + DFS 辅助 Map 工厂全部混在一起
- **无法复用**：其他模块无法直接使用图结构容器
- **内存浪费**：每个节点的子列表用 `new ArrayList<>()` 初始分配 10 个 Object[] 槽，叶子节点（占比 > 50%）白白消耗内存

---

## 四、第三轮方案：单 HashMap 三色标记优化

### 4.1 核心优化思路

将双 HashSet 合并为**一个 HashMap**，用整数值表示三种颜色状态：

```java
static final int WHITE = 0; // 未访问
static final int GRAY  = 1; // 在当前递归栈中
static final int BLACK = 2; // 已完成，确认无环
```

一次 `color.getOrDefault(nodeId, WHITE)` 查找同时得到状态，取代原来的两次 `contains()`。

### 4.2 回溯方式改进

| 旧方式 | 新方式 |
|--------|--------|
| `recursionStack.remove(nodeId)` — LinkedHashSet 链表维护 | `color.put(nodeId, BLACK)` — 简单 HashMap put |
| `cyclePath.add/remove` — 每步入栈出栈 | `parent.put(childId, nodeId)` — 只记录父节点 |
| 发现环时直接有路径（因为维护了 cyclePath） | 发现环时从 parent Map 按需**回溯重建**路径 |

### 4.3 环路径重建算法

```java
private String buildCyclePath(String cycleEntry, Map<String, String> parent) {
    List<String> path = new ArrayList<>();
    path.add(cycleEntry);
    String cur = parent.get(cycleEntry);
    int limit = parent.size() + 1; // 防止意外死循环
    while (cur != null && !cur.equals(cycleEntry) && limit-- > 0) {
        path.add(cur);
        cur = parent.get(cur);
    }
    path.add(cycleEntry); // 闭合环
    Collections.reverse(path);
    return String.join(" → ", path); // 输出：A → B → C → A
}
```

### 4.4 性能对比

| 指标 | 双 HashSet | 单 HashMap 三色 |
|------|-----------|----------------|
| 状态判断查找次数 | 2次/节点 | 1次/节点 |
| 回溯写操作 | LinkedHashSet.remove（链表） | HashMap.put（数组寻址） |
| 环路径内存 | cyclePath 每步 add/remove | parent Map 按需回溯 |
| 内存结构数量 | 3个（visited + stack + cyclePath） | 2个（color + parent） |

---

## 五、第四轮方案：蛇吞蛋内存模型 + 邻接表懒扩容

### 5.1 蛇吞蛋原理

传统解析模式将 JSON 全量实例化为 `MindMapNode` 对象树，内存高峰 = JSON 大小 × 对象开销。

"蛇吞蛋"模式：

```
蛇（Parser）吞入蛋（JsonNode树）
    ↓ 边消化边挤出蛋液（id/text/edges）
    ↓ 写入轻量邻接表（SnakeDigestGraph）
    ↓ 蛋壳（JsonNode对象）离开作用域 → GC 可回收
最终胃里只剩 id/text/edges 三类字符串
```

代码对应：

```java
private SnakeDigestGraph buildGraph(String json) {
    JsonNode rootNode = OBJECT_MAPPER.readTree(trimmed);  // "吞入蛋"
    SnakeDigestGraph graph = new SnakeDigestGraph();
    traverseToGraph(rootNode, null, graph);  // "挤出蛋液，吐出蛋壳"
    // rootNode 局部引用在此离开作用域 → GC 可回收整棵 JsonNode 树
    return graph;
}
```

### 5.2 邻接表懒扩容（缓张嘴）

类比 ArrayList 自动扩容，但进一步优化**初始容量**：

| 对象 | 旧实现 | 新实现（缓张嘴） |
|------|--------|-----------------|
| 每个节点的子节点列表 | `new ArrayList<>()` → 初始10槽 | `new ArrayList<>(0)` → 初始0槽 |
| 首次 `addEdge` 后 | 已有10槽，无需扩容 | 扩至1槽 |
| 后续按1.5倍增长 | 10→15→22... | 1→2→3→4→6→9... |
| 叶子节点（占比>50%） | 10个 Object[] 槽被白白分配 | **0字节**数组空间 |

外层邻接表 Map 初始8桶（`LinkedHashMap(8, 0.75f)`），针对思维导图小图（4~8节点）设计，超过6节点后首次自动扩容。

---

## 六、先列计划：SnakeDigestGraph 容器工具类封装

### 6.1 单一职责原则的违背

优化后的 `MindMapGenerationValidator` 依然有一个问题：`GraphData` 内部类虽已优化，但**数据容器本身还是私有内部类**，导致：

- 无法为数据容器写独立单元测试
- DFS 算法直接操作 `graph.adjacency`（包级访问）
- 其他模块无法复用图容器

### 6.2 命名方案选型

参考 `ConcurrentHashMap` 的命名逻辑（`Concurrent` 描述特性 + `HashMap` 描述结构），候选名称：

| 候选名 | 语义 | 评分 |
|--------|------|------|
| `CycleDetectionGraph` | 功能导向，直白 | 合格 |
| `AdjacencyListGraph` | 结构导向，过于通用 | 较弱 |
| `SnakeDigestGraph` | **蛇（Snake）+ 消化（Digest）+ 图（Graph）**，体现蛇吞蛋原理 | **最优** |

**选定：`SnakeDigestGraph`**

命名语义：
- `Snake`：蛇吞蛋原理中的"蛇"，驱动整个解析流程
- `Digest`：消化行为——只保留 id/text/edges 三类"蛋液"，JsonNode 对象（"蛋壳"）解析后即可被 GC 回收
- `Graph`：底层数据结构为有向图（邻接表）

### 6.3 职责边界划分

```
SnakeDigestGraph（容器）          MindMapGenerationValidator（算法）
─────────────────────────         ──────────────────────────────────
持有邻接表 adjacency               Jackson 解析 JSON → buildGraph()
持有标签表 labels                  traverseToGraph() 递归提取
持有 rootId                        validateGraph() 全部验证逻辑
registerNode() / addEdge()         detectCyclesAndCollectStats() DFS
setRootId() / getRootId()          buildCyclePath() 回溯重建环路径
getChildren() 不可变视图
adjacencyEntries() 不可变视图
newColorMap() DFS辅助工厂
newParentMap() DFS辅助工厂
```

### 6.4 防御性封装设计

```java
// ❌ 旧方式：外部可以直接修改
graph.adjacency.get("A").add("X");  // 破坏图结构

// ✅ 新方式：返回不可变视图
public List<String> getChildren(String nodeId) {
    List<String> children = adjacency.get(nodeId);
    return children == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(children);  // 防外部篡改
}

public Set<Map.Entry<String, List<String>>> adjacencyEntries() {
    return Collections.unmodifiableSet(adjacency.entrySet());
}
```

### 6.5 幂等保护

```java
public void setRootId(String id) {
    if (this.rootId == null) {  // 仅首次生效
        this.rootId = id;
    }
}
```

`traverseToGraph` 是 DFS 递归，每个节点都会调用 `graph.setRootId(id)`。幂等保护确保只有第一个调用（根节点）生效，后续递归调用无副作用。

### 6.6 DFS 辅助工厂方法

```java
// DFS 前按节点数预分配，避免 DFS 中途 rehash
public Map<String, Integer> newColorMap() {
    return new HashMap<>(Math.max(INITIAL_CAPACITY, adjacency.size() * 2));
}

public Map<String, String> newParentMap() {
    return new HashMap<>(Math.max(INITIAL_CAPACITY, adjacency.size() * 2));
}
```

容量 = `max(8, nodeCount × 2)`，装填率保持在 0.5 以下，DFS 全程无 rehash。

---

## 七、降级处理打通（步骤 4.5）

### 7.1 发现降级链断层

原始 `MindMapBufferLayer.process()` 没有调用 `MindMapGenerationValidator`，导致：

- 有环的 JSON 直接进入步骤5（结果优化）
- 步骤6（递归创建节点）死循环崩溃

### 7.2 插入步骤 4.5

在步骤4（调用AI）和步骤5（结果优化）之间插入验证层：

```java
// 步骤 4.5：结构验证（环检测降级处理）
aiResponse = validateAndHandleDegradation(aiResponse, request, response);
```

### 7.3 降级决策树

```
validateAndHandleDegradation(aiResponse)
        │
        ▼
  validator.validate(aiResponse)
        │
   isValid? ──YES──▶ 返回原始 aiResponse（直通）
        │
       NO
        │
   hasCycle?   hasParseError?
   ──YES────────────YES──▶ 强制降级 createSampleMindMapJSON()
        │                   输出日志：[VALIDATION DEGRADED]
       NO
        │
   其他错误（超深度/超子节点数）
        ▼
   仅记录警告，返回原始 aiResponse（继续流程）
```

**致命 vs 非致命**：
- `CIRCULAR_DEPENDENCY`：有环数据**不可渲染**，步骤6必定死循环 → 强制降级
- `PARSE_ERROR`：无法解析 → 强制降级  
- `EXCEEDS_MAX_DEPTH` / `EXCEEDS_MAX_CHILDREN`：超限但结构合法 → 仅警告，继续

---

## 八、最终架构全貌

### 8.1 文件结构

```
freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/validation/
├── SnakeDigestGraph.java              ← 新建（蛇吞蛋图容器，151行）
├── MindMapGenerationValidator.java    ← 改造（删除GraphData内部类，383行）
├── MindMapValidationResult.java       ← 不变
└── MindMapNode.java                   ← 不变

freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/buffer/mindmap/
└── MindMapBufferLayer.java            ← 新增步骤4.5 + validator字段
```

### 8.2 完整调用链

```
MindMapBufferLayer.process()
    │
    ├── 步骤4：callAI() → aiResponse（JSON字符串）
    │
    ├── 步骤4.5：validateAndHandleDegradation()
    │       └── MindMapGenerationValidator.validate(aiResponse)
    │               └── buildGraph() ← "蛇吞蛋"
    │                       └── OBJECT_MAPPER.readTree() 解析 JsonNode 树
    │                       └── traverseToGraph() DFS 提取邻接表
    │                               └── graph.registerNode() / addEdge() / setRootId()
    │                       └── JsonNode 树离开作用域 → GC 回收（"吐蛋壳"）
    │               └── validateGraph(SnakeDigestGraph)
    │                       ├── validateRoot()
    │                       ├── checkDuplicateIds()
    │                       └── detectCyclesAndCollectStats()
    │                               └── statsDFS() ← 三色标记单HashMap DFS
    │                                       └── buildCyclePath() ← parent Map回溯
    │
    └── 步骤5/6：结果优化 + 节点创建（确保此时数据无环）
```

### 8.3 综合对比

| 维度 | 初始（双HashSet+内部类） | 最终（SnakeDigestGraph+三色DFS） |
|------|------------------------|----------------------------------|
| 状态查找次数 | 2次/节点 | **1次/节点** |
| 回溯操作 | LinkedHashSet.remove 链表维护 | **HashMap.put 数组寻址** |
| 叶子节点内存 | 10槽 Object[] | **0字节** |
| 职责 | 数据+算法混合 | **单一职责分离** |
| 可测试性 | 私有内部类，无法独立测试 | **public 类，可独立测试** |
| 防御性封装 | 直接暴露 Map 字段 | **不可变视图** |
| 降级保护 | 断层，有环时死循环崩溃 | **步骤4.5 拦截，强制降级** |
| 线程安全 | 无可变状态，安全 | 无可变状态，安全 |

---

## 九、编译验证

```
$ gradle :freeplane_plugin_ai:compileJava

BUILD SUCCESSFUL in 4s
```

所有变更编译通过，无警告（除 Gradle 版本兼容性提示外）。

---

## 十、关键设计决策摘要

1. **为何放弃 BFS**：BFS 的 visited 集合无法区分回边与共享节点，会在钻石形拓扑结构中误报，且无法重建环路径。

2. **为何选 DFS 三色而非双 HashSet**：状态查找从2次降至1次，回溯从链表维护降至 HashMap put，环路径从逐步维护降至按需重建。

3. **为何初始容量设为0而非10**：思维导图叶子节点占比 > 50%，叶子节点子列表永远不会有元素，`new ArrayList<>(0)` 比 `new ArrayList<>()` 节省 40字节/节点（10个 Object 引用 × 4字节）。

4. **为何命名 SnakeDigestGraph 而非 CycleDetectionGraph**：`CycleDetectionGraph` 描述用途（功能导向），`SnakeDigestGraph` 描述核心机制（蛇吞蛋原理）+ 数据结构（Graph），与 `ConcurrentHashMap` 命名逻辑一致，体现本类特有的内存优化机制。

5. **为何降级策略区分致命/非致命**：有环结构会导致步骤6无限递归崩溃（致命），必须替换；超深度/超子节点数结构仍可渲染（非致命），强制降级反而会丢失 AI 有效输出。
