<template>
  <div class="toolbar">
    <MapManager />
    
    <button @click="handleFitView" title="适应画布">🔍 适应</button>
    <button @click="handleCenterView" title="居中导图">📍 居中</button>
    <button @click="handleRefresh" title="刷新导图">🔄 刷新</button>
    <button @click="handleExport" title="导出为图片" :disabled="!store.currentMap || isExporting">📷 {{ isExporting ? '导出中...' : '导出' }}</button>
    
    <div class="search-container">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索节点..."
        @keyup.enter="handleSearch"
        @input="handleSearchInput"
        class="search-input"
        ref="searchInput"
      />
      <button @click="handleSearch">搜索</button>
      
      <div v-if="searchResults.length > 0" class="search-results">
        <div 
          v-for="result in searchResults" 
          :key="result.nodeId"
          class="search-result-item"
          @click="handleSearchResultClick(result.nodeId)"
        >
          <div class="result-text">{{ result.text }}</div>
          <div class="result-path">{{ result.path }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useMapStore } from '@/stores/mapStore'
import type { UseVueFlow } from '@vue-flow/core'
import MapManager from './MapManager.vue'
import { toPng } from 'html-to-image'

const props = defineProps<{
  vueFlow: UseVueFlow
}>()

const store = useMapStore()
const searchQuery = ref('')
const searchInput = ref<HTMLInputElement>()
const searchResults = ref<{ nodeId: string; text: string; path: string }[]>([])
const isExporting = ref(false)

const handleFitView = () => {
  props.vueFlow.fitView({ padding: 0.2, duration: 300 })
}

const handleCenterView = () => {
  props.vueFlow.setCenter(0, 0, { zoom: 1, duration: 300 })
}

const handleRefresh = async () => {
  await store.loadMap()
}

const handleExport = async () => {
  if (isExporting.value) return
  
  isExporting.value = true
  try {
    // 获取Vue Flow容器元素
    const vueFlowContainer = document.querySelector('.vue-flow') as HTMLElement
    if (!vueFlowContainer) {
      throw new Error('找不到Vue Flow容器')
    }

    // 临时隐藏工具栏和其他UI元素
    const toolbar = document.querySelector('.toolbar') as HTMLElement
    const originalToolbarStyle = toolbar?.style.display
    if (toolbar) {
      toolbar.style.display = 'none'
    }

    // 等待UI更新
    await nextTick()

    // 使用html-to-image截取Vue Flow区域
    const dataUrl = await toPng(vueFlowContainer, {
      backgroundColor: '#ffffff',
      quality: 1.0,
      pixelRatio: 2, // 提高分辨率
      filter: (node) => {
        // 过滤掉不需要的元素
        if (node.classList?.contains('toolbar')) {
          return false
        }
        if (node.classList?.contains('vue-flow__controls')) {
          return false
        }
        if (node.classList?.contains('vue-flow__background')) {
          return false
        }
        return true
      }
    })

    // 恢复工具栏显示
    if (toolbar) {
      toolbar.style.display = originalToolbarStyle
    }

    // 创建下载链接
    const link = document.createElement('a')
    link.href = dataUrl
    link.download = `${store.currentMap?.title || 'mindmap'}_${new Date().getTime()}.png`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

  } catch (error) {
    console.error('导出失败:', error)
    alert('导出失败，请稍后重试')
  } finally {
    isExporting.value = false
  }
}

const handleSearchInput = () => {
  if (searchQuery.value.trim()) {
    store.searchNodes(searchQuery.value)
    searchResults.value = store.searchResults
  } else {
    searchResults.value = []
  }
}

const handleSearch = () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }
  
  store.searchNodes(searchQuery.value)
  searchResults.value = store.searchResults
}

const handleSearchResultClick = (nodeId: string) => {
  // 找到对应的节点并聚焦
  const node = props.vueFlow.nodes.value.find((n: { id: string; }) => n.id === nodeId)
  if (node) {
    props.vueFlow.fitView({ 
      nodes: [nodeId],
      padding: 0.3,
      duration: 500 
    })
    
    // 高亮显示选中的节点
    props.vueFlow.updateNode(nodeId, {
      selected: true,
      style: { 
        border: '2px solid #1890ff',
        backgroundColor: '#e6f7ff'
      }
    })
    
    // 3秒后取消高亮
    setTimeout(() => {
      props.vueFlow.updateNode(nodeId, {
        selected: false,
        style: { 
          border: '1px solid #ddd',
          backgroundColor: 'white'
        }
      })
    }, 3000)
  }
  
  searchResults.value = []
  searchQuery.value = ''
}

// 监听搜索输入框，自动清理上一次输入的内容
watch(searchQuery, (newVal, oldVal) => {
  if (newVal === '' && oldVal !== '') {
    searchResults.value = []
  }
})
</script>

<style scoped>
.toolbar {
  position: fixed;
  top: 20px;
  left: 20px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  gap: 8px;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

button, .search-input {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

button:hover:not(:disabled) {
  background: #f0f0f0;
  border-color: #1890ff;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search-container {
  position: relative;
}

.search-input {
  width: 180px;
}

.search-input:focus {
  border-color: #1890ff;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow-y: auto;
  z-index: 1001;
  margin-top: 4px;
}

.search-result-item {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.search-result-item:hover {
  background: #f5f5f5;
}

.search-result-item:last-child {
  border-bottom: none;
}

.result-text {
  font-weight: 500;
  margin-bottom: 2px;
}

.result-path {
  font-size: 12px;
  color: #666;
}
</style>