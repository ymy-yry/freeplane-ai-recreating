# MindMapGenerationValidator 算法优化路程

## 总览

| 阶段 | 数据结构 | 环检测算法 | 遍历次数 | 内存峰值 |
|------|----------|-----------|---------|---------|
| V1 原始实现 | MindMapNode 对象树 | 多次独立 DFS | **5次** | O(节点数 × 对象大小) |
| V2 邻接表 + 蛇吞蛋 | GraphData 邻接表 | visited + recursionStack 双 Set DFS | **1次** | O(节点ID总长度) |
| V3 三色标记 | GraphData 邻接表 | 三色标记 + parent Map DFS | **1次** | O(节点ID总长度)，查找减半 |

---

## 阶段一：V1 原始实现

### 文件来源
游离在项目外的 `validation/validation/` 目录，未集成到 `src` 构建路径。

### 数据结构
```
JSON 字符串
    ↓ Gson 解析（仅运行时依赖，编译期不可见）
MindMapNode 对象树（完整节点对象，含 id/text/children 字段）
    ↓ 各验证方法独立遍历
5 次全树 DFS
```

### 核心问题清单
| 问题 | 描述 |
|------|------|
| **5次独立全树遍历** | 环检测、深度检查、子数检查、唯一性检查、统计信息各自独立遍历一次 |
| **Gson 依赖不可见** | Gson 仅通过 langchain4j 运行时传递，编译期 classpath 不含 Gson，导致编译失败 |
| **静态线程池泄漏** | `static ExecutorService` 在 OSGi 插件热重载时不会被关闭，线程持续泄漏 |
| **UNLINKED_CHILD 误报** | 被多个父节点引用的节点被错误标记为"未连接子节点" |
| **HashSet 无序** | 环路径用 HashSet 存储，输出顺序不确定 |
| **算法命名不准确** | 注释称"拓扑排序"，实际是 DFS 环检测（两者逻辑完全不同） |
| **模块位置错误** | 文件在项目外部目录，无法参与编译和测试 |

### 伪代码（5次遍历）
```java
// 旧实现：5个独立方法，每个都从根节点做全树遍历
validateRoot(root);           // 遍历 1
detectCycles(root);           // 遍历 2：HashSet visited + Stack recursion
checkDepth(root, 0);          // 遍历 3
checkChildrenCount(root);     // 遍历 4
collectStatistics(root);      // 遍历 5
```

---

## 阶段二：V2 邻接表 + "蛇吞蛋"内存优化

### 核心改进：数据结构革新

**"蛇吞蛋"原理**：
- **吞入（口部）**：Jackson 解析 JSON 得到 JsonNode 对象树
- **挤出蛋液（颈部）**：DFS 递归时只提取 `id`/`text`/`childIds` 三类字符串写入 `GraphData`
- **吐出蛋壳（颈部挤压）**：JsonNode 节点完成提取后局部引用失效，GC 可立即回收
- **消化蛋液（胃部）**：所有验证在轻量字符串邻接表 `GraphData` 上完成

```
JSON 字符串
    ↓ Jackson 解析（Jackson 在 compileClasspath，通过 langchain4j 传递）
JsonNode 树（"蛋"，完整对象树，内存峰值）
    ↓ traverseToGraph()：DFS 逐节点提取 id/text，JsonNode 节点逐步可 GC（"吐壳"）
GraphData 邻接表（"蛋液"，只含 Map<String,List<String>> + Map<String,String>）
    ↓ 单次 statsDFS()
环检测 + 深度 + 子数 + 统计（全部完成）
```

### GraphData 数据结构
```java
private static final class GraphData {
    String rootId;
    Map<String, List<String>> adjacency; // id → childIds（邻接表）
    Map<String, String> labels;          // id → text（仅用于报告）
}
```

### 环检测算法：双 Set DFS
```java
Set<String> visited = new HashSet<>();
Set<String> recursionStack = new LinkedHashSet<>(); // LinkedHashSet 保证路径有序
List<String> cyclePath = new ArrayList<>();         // 实时维护路径

// 判断逻辑：每节点需要 2 次 contains 查找
if (recursionStack.contains(nodeId)) → 发现环
if (visited.contains(nodeId)) return  → 已完成，跳过

// 回溯：LinkedHashSet.remove(nodeId)（链表维护开销）
//       cyclePath.remove(last)（ArrayList 出栈）
```

### 解决的问题
| 问题 | 解决方案 |
|------|---------|
| 5次独立遍历 | 合并为 `statsDFS` 单次 DFS，同时完成全部验证 |
| Gson 依赖 | 改用 Jackson DataBind（已在 compileClasspath） |
| 静态线程池泄漏 | 线程池改由调用方传入 `ExecutorService`，不持有静态实例 |
| UNLINKED_CHILD 误报 | 重写 `checkDuplicateIds`，通过扫描所有父节点的 childIds 集合检测重复 |
| HashSet 无序 | recursionStack 改用 `LinkedHashSet`，保证路径输出有序 |
| 模块位置 | 迁移至 `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/validation/` |

### 性能对比（实测）
| 规模 | 旧方案（估算） | 新方案（实测） |
|------|--------------|--------------|
| ~85节点 | ~400–800 µs | **271 µs** |
| ~400节点 | ~1500–3000 µs | **390 µs** |
| ~1111节点 | ~5000–10000 µs | **1057 µs** |
| 1111节点宽树完整验证 | ~10–50 ms | **1 ms** |
| 深度101链式树 | ~5–20 ms | **<1 ms** |

---

## 阶段三：V3 三色标记 + parent Map

### 核心改进：DFS 内部数据结构优化

**三色语义**（源自图着色理论）：

| 颜色 | 值 | 含义 |
|------|----|------|
| WHITE | 0 | 未访问（节点尚未进入 DFS） |
| GRAY | 1 | 正在递归栈中（当前 DFS 路径上） |
| BLACK | 2 | 已完成，子树确认无环 |

### 算法对比

#### 旧（双 Set）
```
每个节点需要：
  1. recursionStack.contains(nodeId)   → 查找1（LinkedHashSet）
  2. visited.contains(nodeId)          → 查找2（HashSet）
  3. recursionStack.add(nodeId)
  4. visited.add(nodeId)
  5. cyclePath.add(nodeId)             → 每步路径维护
  ...
  6. recursionStack.remove(nodeId)     → LinkedHashSet 链表维护
  7. cyclePath.remove(last)            → ArrayList 出栈
```

#### 新（三色标记）
```
每个节点需要：
  1. color.getOrDefault(nodeId, WHITE)  → 一次查找同时判断 GRAY/BLACK
  2. color.put(nodeId, GRAY)            → 进入
  3. parent.put(childId, nodeId)        → 仅记录父关系，O(1)
  ...
  4. color.put(nodeId, BLACK)           → 简单 put，替代 LinkedHashSet.remove()
  （环路径：仅在发现环时按需调用 buildCyclePath()，正常路径无额外开销）
```

### 数据结构替换
| 旧结构 | 新结构 | 收益 |
|--------|--------|------|
| `HashSet<String> visited` | `Map<String,Integer> color`（BLACK） | 合并两个 Set |
| `LinkedHashSet<String> recursionStack` | `Map<String,Integer> color`（GRAY） | 消除链表维护 |
| `ArrayList<String> cyclePath` | `Map<String,String> parent` | 路径按需重建，正常路径零开销 |

### 环路径重建（buildCyclePath）
```java
// 发现回边：color[nodeId] == GRAY
// 从 parent Map 沿父链回溯，直到再次遇到 cycleEntry
// 输出：A → B → C → A

private String buildCyclePath(String cycleEntry, Map<String, String> parent) {
    List<String> path = new ArrayList<>();
    path.add(cycleEntry);
    String cur = parent.get(cycleEntry);
    while (cur != null && !cur.equals(cycleEntry)) {
        path.add(cur);
        cur = parent.get(cur);
    }
    path.add(cycleEntry); // 闭合环
    Collections.reverse(path);
    return String.join(" → ", path);
}
```

### 优化量化

| 维度 | 双 Set 方案 | 三色标记方案 |
|------|------------|-------------|
| 每节点 Map/Set 查找次数 | **2次** | **1次** |
| 内存结构数量 | 3个集合（visited + recursionStack + cyclePath） | 2个 Map（color + parent） |
| 回溯操作 | `LinkedHashSet.remove()` 链表维护 | `color.put(BLACK)` 简单覆写 |
| 路径维护时机 | **每步**入栈/出栈 | **仅发现环时**按需重建 |
| 正常路径（无环）路径开销 | O(n) ArrayList 操作 | **O(0)**（parent Map 已存在，无额外操作） |

---

## 完整演进图

```
V1 原始实现
├── 数据结构：MindMapNode 对象树
├── 解析：Gson（编译期不可见 ❌）
├── 遍历：5次独立全树 DFS
├── 环检测：HashSet visited（无序）
├── 问题：泄漏线程、误报、5倍遍历开销
└── 位置：游离在项目外部目录

        ↓ [迁移 + 重构]

V2 邻接表 + 蛇吞蛋
├── 数据结构：GraphData 邻接表（Map<String,List<String>>）
├── 解析：Jackson（编译期可见 ✅）+ 蛇吞蛋（JsonNode GC）
├── 遍历：单次 statsDFS（1次完成全部验证）
├── 环检测：visited(HashSet) + recursionStack(LinkedHashSet) 双 Set
├── 修复：静态线程池、UNLINKED_CHILD误报、路径有序输出
└── 位置：freeplane_plugin_ai/src/.../validation/

        ↓ [算法优化]

V3 三色标记 + parent Map（当前版本）
├── 数据结构：GraphData 邻接表（不变）
├── 解析：蛇吞蛋（不变）
├── 遍历：单次 statsDFS（不变）
├── 环检测：color HashMap（WHITE/GRAY/BLACK）+ parent HashMap
├── 优化：查找 2次→1次，消除链表维护，路径按需重建
└── 测试：20/20 全通过，完整环路径输出验证
```

---

## 测试覆盖（最终版）

| 测试组 | 用例 | 状态 |
|--------|------|------|
| 基础正确性 | 简单树、空输入、非法JSON、数组根、重复ID、自引用 | ✅ |
| 环路径验证 | CIRCULAR_DEPENDENCY 路径输出、SELF_REFERENCE 输出 | ✅ |
| 深度/子数限制 | maxDepth / maxChildren / maxTotalNodes | ✅ |
| 大规模数据 | 1111节点宽树（1ms）、深度101链（<1ms） | ✅ |
| 内存/耗时对比 | 三规模吞吐量、内存测量 | ✅ |
| 线程安全/异步 | 20线程并发、callback、CompletableFuture | ✅ |
| 统计信息 | 单节点、完全二叉树深度3 | ✅ |

**总计：20/20 通过，0 失败**
