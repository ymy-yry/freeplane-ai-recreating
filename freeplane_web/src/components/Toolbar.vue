<template>
	<div class="toolbar">
	  <button @click="handleFitView" title="适应画布">🔍 适应</button>
	  <button @click="handleCenterView" title="居中导图">📍 居中</button>
	  <button @click="handleRefresh" title="刷新导图">🔄 刷新</button>
	  <button @click="handleToggleAI" title="AI 面板">🤖 AI</button>
	  <div v-if="aiStore.panelVisible" class="mode-tabs">
		<button class="mode-btn" :class="{ active: aiStore.aiMode === 'auto' }" @click="aiStore.setMode('auto')">Auto</button>
		<button class="mode-btn" :class="{ active: aiStore.aiMode === 'chat' }" @click="aiStore.setMode('chat')">Chat</button>
		<button class="mode-btn" :class="{ active: aiStore.aiMode === 'build' }" @click="aiStore.setMode('build')">Build</button>
	  </div>
	  
	  <div class="toolbar-divider"></div>
	  
	  <button @click="handleExport" title="导出导图" class="export-btn">💾 导出</button>
	  <button @click="handleExportMM" title="导出为 Freeplane 格式" class="export-btn">📄 .mm</button>
	  <button @click="handleExportJSON" title="导出为 JSON 备份" class="export-btn">📦 .json</button>
	  <button @click="triggerImport" title="导入 JSON 或 MM" class="import-btn">📥 导入</button>
	  <input
		ref="importInput"
		type="file"
		accept=".json,.mm,.xml,application/json,application/xml,text/xml"
		class="hidden-input"
		@change="handleImportFile"
	  />
	  
	  <div class="toolbar-divider"></div>
	  
	  <input
		v-model="searchQuery"
		type="text"
		placeholder="搜索节点..."
		@keyup.enter="handleSearch"
		class="search-input"
	  />
	  <button @click="handleSearch">搜索</button>
	</div>
  </template>
  
  <script setup lang="ts">
  import { ref } from 'vue'
  import { useMapStore } from '@/stores/mapStore'
  import { exportToMM, exportToJSON, importMindMapFile, showExportDialog } from '@/utils/exportUtils'
  import { useAIStore } from '@/stores/aiStore'
  
  const props = defineProps<{
	vueFlow: any
  }>()
  
  const store = useMapStore()
  const aiStore = useAIStore()
  const searchQuery = ref('')
  const importInput = ref<HTMLInputElement | null>(null)
  
  const handleFitView = () => {
	props.vueFlow.fitView({ padding: 0.2, duration: 300 })
  }
  
  const handleCenterView = () => {
	props.vueFlow.setCenter(0, 0, { zoom: 1, duration: 300 })
  }
  
  const handleRefresh = async () => {
	store.resumePolling()
	await store.loadMap()
  }
  
  const handleExport = () => {
	if (!store.currentMap) {
	  alert('当前没有打开的导图')
	  return
	}
	showExportDialog(store.currentMap)
  }
  
  const handleExportMM = () => {
	if (!store.currentMap) {
	  alert('当前没有打开的导图')
	  return
	}
	exportToMM(store.currentMap)
  }
  
  const handleExportJSON = () => {
	if (!store.currentMap) {
	  alert('当前没有打开的导图')
	  return
	}
	exportToJSON(store.currentMap)
  }

  const triggerImport = () => {
	importInput.value?.click()
  }

  const handleImportFile = async (event: Event) => {
	const input = event.target as HTMLInputElement
	const file = input.files?.[0]
	if (!file) return
	try {
	  const importedMap = await importMindMapFile(file)
	  store.pausePolling()
	  store.setCurrentMap(importedMap)
	  props.vueFlow.fitView({ padding: 0.22, duration: 250 })
	} catch (error) {
	  const message = error instanceof Error ? error.message : '导入失败'
	  alert(message)
	} finally {
	  input.value = ''
	}
  }
  
  const handleSearch = async () => {
	const query = searchQuery.value.trim()
	if (!query || !store.currentMap?.mapId) return
	try {
	  const res = await aiStore.keywordSearch({ query, mapId: store.currentMap.mapId })
	  const first = res.results?.[0]
	  if (!first) return
	  const node = props.vueFlow.findNode(first.nodeId)
	  if (!node) return
	  props.vueFlow.setCenter(node.position.x, node.position.y, { zoom: 1.2, duration: 250 })
	} finally {
	  searchQuery.value = ''
	}
  }
  
  const handleToggleAI = () => {
	aiStore.togglePanel()
  }
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
  
  button:hover {
	background: #f0f0f0;
	border-color: #1890ff;
  }
  
  .search-input {
	width: 180px;
  }

  .hidden-input {
	display: none;
  }
  
  .toolbar-divider {
	width: 1px;
	height: 24px;
	background: #ddd;
	margin: 0 4px;
  }
  
  .export-btn {
	background: #e8f5e9;
	border-color: #4caf50;
  }
  
  .export-btn:hover {
	background: #4caf50;
	color: white;
	border-color: #4caf50;
  }

  .import-btn {
	background: #eef6ff;
	border-color: #4a90e2;
  }

  .import-btn:hover {
	background: #4a90e2;
	color: #fff;
	border-color: #4a90e2;
  }
  
  .search-input:focus {
	border-color: #1890ff;
  }

  .mode-tabs {
	display: flex;
	gap: 4px;
	padding: 2px;
	background: #f1f5f9;
	border-radius: 8px;
  }

  .mode-btn {
	padding: 4px 10px;
	font-size: 12px;
	border: none;
	background: transparent;
	border-radius: 6px;
	cursor: pointer;
  }

  .mode-btn.active {
	background: #fff;
	box-shadow: 0 1px 3px rgba(15, 23, 42, 0.2);
	color: #2563eb;
  }
  </style>