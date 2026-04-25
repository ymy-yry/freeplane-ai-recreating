<template>
	<div class="toolbar">
	  <button @click="handleFitView" title="适应画布">🔍 适应</button>
	  <button @click="handleCenterView" title="居中导图">📍 居中</button>
	  <button @click="handleRefresh" title="刷新导图">🔄 刷新</button>
	  <button @click="handleToggleAI" title="AI 面板">🤖 AI</button>
	  <button @click="handleCreateNewMap" title="新建导图" class="new-map-btn">➕ 新建</button>
	  <select v-model="selectedMapId" @change="handleSwitchMap" class="map-selector">
		<option value="">切换导图...</option>
		<option v-for="map in store.allMaps" :key="map.mapId" :value="map.mapId">
		  {{ map.title }} {{ map.isCurrent ? '(当前)' : '' }}
		</option>
	  </select>
	  <button @click="handleLoadAllMaps" title="刷新导图列表" class="maps-list-btn">🗂️ 列表</button>
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
  import { nextTick, onMounted, ref } from 'vue'
  import { useMapStore } from '@/stores/mapStore'
  import { exportToMM, exportToJSON, importMindMapFile, showExportDialog } from '@/utils/exportUtils'
  import { useAIStore } from '@/stores/aiStore'
  
  const props = defineProps<{
	vueFlow: any
  }>()
  
  const store = useMapStore()
  const aiStore = useAIStore()
  const searchQuery = ref('')
  const selectedMapId = ref('')
  const importInput = ref<HTMLInputElement | null>(null)

  onMounted(() => {
	void store.loadAllMaps()
  })
  
  const handleFitView = () => {
	props.vueFlow.fitView({ padding: 0.2, duration: 300 })
  }
  
  const handleCenterView = () => {
	props.vueFlow.setCenter(0, 0, { zoom: 1, duration: 300 })
  }
  
  const handleRefresh = async () => {
	store.resumePolling()
	await store.loadMap()
	await store.loadAllMaps()
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
	  if (file.name.toLowerCase().endsWith('.json')) {
		const importedMap = await importMindMapFile(file)
		store.setCurrentMap(importedMap)
	  } else {
		await store.importMap(file)
	  }
	  store.resumePolling()
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

  const handleCreateNewMap = async () => {
	try {
	  const title = prompt('请输入新导图标题:', '新导图')
	  if (title === null) return
	  await store.createNewMap(title || undefined)
	  await nextTick()
	  props.vueFlow.fitView({ padding: 0.2, duration: 250 })
	} catch (error) {
	  const message = error instanceof Error ? error.message : '创建导图失败'
	  alert(message)
	}
  }

  const handleSwitchMap = async () => {
	if (!selectedMapId.value) return
	try {
	  await store.switchCurrentMap(selectedMapId.value)
	  selectedMapId.value = ''
	  store.resumePolling()
	  await nextTick()
	  props.vueFlow.fitView({ padding: 0.2, duration: 250 })
	} catch (error) {
	  const message = error instanceof Error ? error.message : '切换导图失败'
	  alert(message)
	}
  }

  const handleLoadAllMaps = async () => {
	await store.loadAllMaps()
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
  
  button, .search-input, .map-selector {
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

  .new-map-btn {
	background: #edf6ff;
	border-color: #4a90e2;
  }

  .new-map-btn:hover {
	background: #4a90e2;
	color: #fff;
	border-color: #4a90e2;
  }

  .maps-list-btn {
	background: #fff7ed;
	border-color: #f59e0b;
  }

  .maps-list-btn:hover {
	background: #f59e0b;
	color: #fff;
	border-color: #f59e0b;
  }

  .map-selector {
	min-width: 180px;
	padding: 6px 10px;
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