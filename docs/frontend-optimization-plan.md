# Freeplane Web 前端完整优化计划

**负责人**：赵佳艺（成员B）  孙楠（成员C）
**开始日期**：2026年4月8日  
**文档版本**：1.0

---

## 一、当前状态评估

### 1.1 已完成功能（✅）

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| 思维导图加载 | ✅ 完成 | 轮询 + 树形转换 |
| 节点创建 | ✅ 完成 | Tab 键 + ActionModal |
| 节点编辑 | ✅ 完成 | 双击编辑 |
| 节点删除 | ✅ 完成 | 二次确认 |
| 节点折叠/展开 | ✅ 完成 | 后端同步 |
| 基础工具栏 | ✅ 完成 | 适应、居中、刷新、搜索 |
| 右键菜单组件 | ✅ 存在 | NodeContextMenu.vue（未集成） |

### 1.2 已知问题（⚠️）

| 问题 | 严重程度 | 位置 | 说明 |
|------|---------|------|------|
| NodeEditPanel 文本不同步 | 中 | NodeEditPanel.vue:34 | initialText 不随 props 更新 |
| 右键菜单未集成 | 低 | MindMapCanvas.vue | NodeContextMenu 组件存在但未使用 |
| 搜索功能未实现 | 中 | Toolbar.vue:42-46 | 只有 console.log |
| 布局算法可能重叠 | 低 | treeToFlow.ts:7-9 | 多分支时 Y 坐标可能重叠 |

### 1.3 缺失功能（❌）

| 功能 | 优先级 | 预计工作量 |
|------|--------|-----------|
| AI 智能生成面板 | 🔴 高 | 2 天 |
| 搜索结果高亮 | 🟡 中 | 1 天 |
| 节点样式编辑 | 🟡 中 | 2 天 |
| 快捷键系统 | 🟢 低 | 1 天 |
| 导出功能 | 🟢 低 | 1 天 |


---

## 二、详细实施计划

### 阶段 1：Bug 修复与体验优化

#### Day 1：修复已知问题

**任务 1.1**：修复 NodeEditPanel 文本同步问题
- 文件：`freeplane_web/src/components/NodeEditPanel.vue`
- 工作量：30 行修改
- 内容：
  ```typescript
  // 添加 watch 监听 props 变化
  import { watch } from 'vue'
  
  watch(() => props.initialText, (newVal) => {
    editText.value = newVal
  })
  
  watch(() => props.visible, (visible) => {
    if (visible) {
      editText.value = props.initialText
      nextTick(() => textareaRef.value?.focus())
    }
  })
  ```

**任务 1.2**：集成右键菜单
- 文件：`freeplane_web/src/components/MindMapCanvas.vue`
- 工作量：+50 行
- 内容：
  ```vue
  <NodeContextMenu
    :visible="contextMenu.visible"
    :x="contextMenu.x"
    :y="contextMenu.y"
    :node-id="contextMenu.nodeId"
    @edit="handleEditFromContext"
    @delete="handleDeleteFromContext"
    @create-child="handleCreateChildFromContext"
  />
  
  <VueFlow @node-context-menu="handleNodeContextMenu" ...>
  ```

**任务 1.3**：实现搜索节点功能
- 文件：`freeplane_web/src/components/Toolbar.vue`
- 工作量：+40 行
- 内容：
  ```typescript
  import * as nodeApi from '@/api/nodeApi'
  
  const handleSearch = async () => {
    if (!searchQuery.value.trim() || !store.currentMap) return
    
    const results = await nodeApi.searchNodes(
      searchQuery.value,
      store.currentMap.mapId
    )
    
    // 高亮搜索结果
    highlightNodes(results.results)
  }
  ```

---

#### Day 2：布局算法优化

**任务 2.1**：优化 treeToFlow 布局
- 文件：`freeplane_web/src/utils/treeToFlow.ts`
- 工作量：+80 行修改
- 内容：
  - 改进子树 Y 坐标计算
  - 添加节点防重叠逻辑
  - 支持左右对称布局

**任务 2.2**：实现节点位置持久化
- 文件：`freeplane_web/src/stores/mapStore.ts`
- 工作量：+60 行
- 内容：
  ```typescript
  // 保存节点位置
  const nodePositions = ref<Map<string, {x: number, y: number}>>(new Map())
  
  const saveNodePosition = (nodeId: string, position: {x: number, y: number}) => {
    nodePositions.value.set(nodeId, position)
  }
  
  // 在 updateFlow 时恢复位置
  ```

---

#### Day 3：用户体验优化

**任务 3.1**：添加加载状态提示
- 文件：`freeplane_web/src/components/MindMapCanvas.vue`
- 工作量：+40 行
- 内容：
  ```vue
  <div v-if="store.loading" class="loading-overlay">
    <div class="spinner"></div>
    <p>加载中...</p>
  </div>
  ```

**任务 3.2**：添加操作成功/失败提示
- 文件：`freeplane_web/src/stores/mapStore.ts`
- 工作量：+50 行
- 内容：
  ```typescript
  import { useToast } from '@/composables/useToast'
  
  const createNode = async (parentId: string, text: string) => {
    try {
      await mapApi.createNode(...)
      toast.success('节点创建成功')
    } catch (e) {
      toast.error('节点创建失败：' + e.message)
    }
  }
  ```

**任务 3.3**：优化轮询策略
- 文件：`freeplane_web/src/stores/mapStore.ts`
- 工作量：+30 行修改
- 内容：
  ```typescript
  // 智能轮询：用户操作时暂停轮询
  const isUserOperating = ref(false)
  
  const startPolling = () => {
    pollingInterval = setInterval(() => {
      if (!isUserOperating.value) {
        loadMap()
      }
    }, 3000)
  }
  ```

---

### 阶段 2：AI 智能缓冲层集成

#### Day 4-5：AI 智能生成面板

**任务 4.1**：创建 AISmartPanel 组件
- 文件：`freeplane_web/src/components/AISmartPanel.vue`（新建）
- 工作量：200 行
- 功能：
  - 自然语言输入框
  - 一键生成按钮
  - 处理进度展示
  - 优化日志展示
  - 质量评分显示

**任务 4.2**：新增智能 API 调用
- 文件：`freeplane_web/src/api/mapApi.ts`
- 工作量：+20 行
- 内容：
  ```typescript
  export const smartGenerateMindMap = async (input: string) => {
    const res = await api.post('/ai/smart', { input })
    return res.data
  }
  ```

**任务 4.3**：集成到工具栏
- 文件：`freeplane_web/src/components/Toolbar.vue`
- 工作量：+40 行
- 内容：
  ```vue
  <button @click="showAISmartPanel = true" class="ai-btn">
    🤖 AI 智能生成
  </button>
  
  <AISmartPanel
    :visible="showAISmartPanel"
    @generate="handleAIGenerate"
    @close="showAISmartPanel = false"
  />
  ```

---

#### Day 6：AI 功能增强

**任务 5.1**：添加 AI 生成历史记录
- 文件：`freeplane_web/src/stores/mapStore.ts`
- 工作量：+60 行
- 内容：
  ```typescript
  const aiHistory = ref<Array<{
    input: string
    model: string
    quality: number
    timestamp: number
  }>>([])
  
  const addToAIHistory = (record) => {
    aiHistory.value.unshift(record)
  }
  ```

**任务 5.2**：实现 AI 生成结果预览
- 文件：`freeplane_web/src/components/AISmartPanel.vue`
- 工作量：+80 行
- 内容：
  - 生成前显示预估信息
  - 生成中显示进度条
  - 生成后显示质量评分

---

#### Day 7：AI 交互优化

**任务 6.1**：添加流式响应支持
- 文件：`freeplane_web/src/api/mapApi.ts`
- 工作量：+50 行
- 内容：
  ```typescript
  export const smartGenerateStream = async (
    input: string,
    onChunk: (chunk: string) => void
  ) => {
    const response = await fetch('/api/ai/smart', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ input, stream: true })
    })
    
    const reader = response.body.getReader()
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      onChunk(new TextDecoder().decode(value))
    }
  }
  ```

**任务 6.2**：优化错误处理
- 文件：`freeplane_web/src/components/AISmartPanel.vue`
- 工作量：+40 行
- 内容：
  - 网络错误重试
  - 模型降级提示
  - 友好错误信息

---

### 阶段 3：高级功能开发

#### Day 8：节点样式编辑

**任务 7.1**：创建 NodeStylePanel 组件
- 文件：`freeplane_web/src/components/NodeStylePanel.vue`（新建）
- 工作量：150 行
- 功能：
  - 背景颜色选择
  - 边框样式设置
  - 字体大小调整
  - 图标选择

**任务 7.2**：添加样式 API
- 文件：`freeplane_web/src/api/nodeApi.ts`
- 工作量：+30 行
- 内容：
  ```typescript
  export const updateNodeStyle = async (
    mapId: string,
    nodeId: string,
    style: NodeStyle
  ) => {
    await api.post('/nodes/style', { mapId, nodeId, style })
  }
  ```

---

#### Day 9：快捷键系统

**任务 8.1**：实现快捷键管理器
- 文件：`freeplane_web/src/utils/shortcutManager.ts`（新建）
- 工作量：120 行
- 内容：
  ```typescript
  export const shortcuts = {
    'Tab': 'createNode',
    'Delete': 'deleteNode',
    'F2': 'editNode',
    'Ctrl+F': 'search',
    'Ctrl+Z': 'undo',
    'Ctrl+Y': 'redo',
    'Escape': 'closePanel'
  }
  ```

**任务 8.2**：集成到 MindMapCanvas
- 文件：`freeplane_web/src/components/MindMapCanvas.vue`
- 工作量：+30 行修改
- 内容：替换现有的 handleKeyDown

---

#### Day 10：导出功能

**任务 9.1**：添加导出菜单
- 文件：`freeplane_web/src/components/Toolbar.vue`
- 工作量：+50 行
- 内容：
  ```vue
  <div class="dropdown">
    <button>📥 导出</button>
    <div class="dropdown-content">
      <button @click="exportAsPNG">PNG 图片</button>
      <button @click="exportAsSVG">SVG 矢量图</button>
      <button @click="exportAsJSON">JSON 数据</button>
      <button @click="exportAsMarkdown">Markdown</button>
    </div>
  </div>
  ```

**任务 9.2**：实现导出功能
- 文件：`freeplane_web/src/utils/exportUtils.ts`（新建）
- 工作量：100 行
- 内容：
  ```typescript
  export const exportAsPNG = (nodes: Node[], edges: Edge[]) => {
    // 使用 html2canvas 或截图 API
  }
  
  export const exportAsJSON = (mapData: MindMapData) => {
    const blob = new Blob([JSON.stringify(mapData, null, 2)], {
      type: 'application/json'
    })
    downloadBlob(blob, 'mindmap.json')
  }
  ```

---

### 阶段 4：性能优化与测试（2 天）

#### Day 11：性能优化

**任务 10.1**：优化大数据渲染
- 文件：`freeplane_web/src/utils/treeToFlow.ts`
- 工作量：+60 行
- 内容：
  - 虚拟滚动（节点 > 100 时）
  - 按需渲染子节点
  - Web Worker 计算布局

**任务 10.2**：优化网络请求
- 文件：`freeplane_web/src/stores/mapStore.ts`
- 工作量：+40 行
- 内容：
  - 请求去重
  - 失败重试
  - 增量更新（只请求变化的节点）

---

#### Day 12：测试与文档

**任务 11.1**：编写组件测试
- 文件：`freeplane_web/src/components/__tests__/`
- 工作量：150 行
- 内容：
  - MindMapCanvas 测试
  - ActionModal 测试
  - AISmartPanel 测试

**任务 11.2**：编写 E2E 测试
- 文件：`freeplane_web/e2e/`
- 工作量：100 行
- 内容：
  - 节点创建流程
  - 折叠/展开流程
  - AI 生成流程

**任务 11.3**：更新文档
- 文件：`freeplane_web/README.md`
- 工作量：50 行
- 内容：
  - 功能列表
  - 使用指南
  - 开发指南

---

## 四、文件清单

### 4.1 新增文件（8个）

```
freeplane_web/src/
├── components/
│   ├── AISmartPanel.vue           # AI 智能生成面板（200 行）
│   └── NodeStylePanel.vue         # 节点样式编辑（150 行）
├── utils/
│   ├── shortcutManager.ts         # 快捷键管理（120 行）
│   └── exportUtils.ts             # 导出工具（100 行）
├── composables/
│   └── useToast.ts                # Toast 提示（50 行）
└── components/__tests__/
    ├── MindMapCanvas.spec.ts      # 组件测试（80 行）
    ├── ActionModal.spec.ts        # 组件测试（40 行）
    └── AISmartPanel.spec.ts       # 组件测试（30 行）
```

### 4.2 修改文件（7个）

```
freeplane_web/src/
├── api/
│   ├── mapApi.ts                  # +20 行（智能生成接口）
│   └── nodeApi.ts                 # +30 行（样式、搜索）
├── components/
│   ├── MindMapCanvas.vue          # +100 行（右键菜单、加载状态）
│   ├── NodeEditPanel.vue          # +20 行（文本同步）
│   ├── Toolbar.vue                # +90 行（AI 按钮、导出菜单）
│   └── ActionModal.vue            # +10 行（样式优化）
└── stores/
    └── mapStore.ts                # +150 行（AI 历史、Toast、优化）
```

---

## 五、验收标准

### 5.1 功能验收

- [ ] 所有已知 Bug 已修复
- [ ] AI 智能生成面板可用
- [ ] 搜索功能正常
- [ ] 右键菜单集成完成
- [ ] 节点样式可编辑
- [ ] 快捷键系统完善
- [ ] 导出功能可用

### 5.2 性能验收

- [ ] 首屏加载 < 2 秒
- [ ] 节点操作响应 < 200ms
- [ ] 100 节点渲染流畅
- [ ] 内存占用 < 200MB

### 5.3 质量验收

- [ ] TypeScript 零错误
- [ ] ESLint 零警告
- [ ] 测试覆盖率 > 70%
- [ ] 无控制台错误

---

## 六、风险与应对

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| AI 接口未就绪 | 高 | 中 | 使用 Mock 数据开发 |
| 性能优化困难 | 中 | 中 | 分阶段优化，先解决瓶颈 |
| 浏览器兼容性 | 中 | 低 | 使用 Polyfill |
| 时间不足 | 高 | 低 | 优先核心功能 |

---



**计划编制**：赵佳艺（成员B）  孙楠（成员C）
**编制日期**：2026年4月8日  
**下次更新**：阶段 1 完成后
