<template>
	<div class="toolbar">
	  <button @click="handleFitView" title="适应画布">🔍 适应</button>
	  <button @click="handleCenterView" title="居中导图">📍 居中</button>
	  <button @click="handleRefresh" title="刷新导图">🔄 刷新</button>
	  
	  <div class="toolbar-divider"></div>
	  
	  <button @click="handleExport" title="导出导图" class="export-btn">💾 导出</button>
	  <button @click="handleExportMM" title="导出为 Freeplane 格式" class="export-btn">📄 .mm</button>
	  <button @click="handleExportJSON" title="导出为 JSON 备份" class="export-btn">📦 .json</button>
	  
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
  import { exportToMM, exportToJSON, showExportDialog } from '@/utils/exportUtils'
  
  const props = defineProps<{
	vueFlow: any
  }>()
  
  const store = useMapStore()
  const searchQuery = ref('')
  
  const handleFitView = () => {
	props.vueFlow.fitView({ padding: 0.2, duration: 300 })
  }
  
  const handleCenterView = () => {
	props.vueFlow.setCenter(0, 0, { zoom: 1, duration: 300 })
  }
  
  const handleRefresh = async () => {
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
  
  const handleSearch = () => {
	if (!searchQuery.value.trim()) return
	console.log('[搜索] 关键词：', searchQuery.value)
	searchQuery.value = ''
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
  
  .search-input:focus {
	border-color: #1890ff;
  }
  </style>