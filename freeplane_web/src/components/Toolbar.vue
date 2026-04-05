<template>
	<div class="toolbar">
	  <button @click="handleFitView" title="适应画布">
		🔍 适应
	  </button>
	  <button @click="handleCenterView" title="居中导图">
		📍 居中
	  </button>
	  <button @click="handleRefresh" title="刷新导图（同步 Freeplane）">
		🔄 刷新
	  </button>
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
  import { useVueFlow } from '@vue-flow/core'
  
  const store = useMapStore()
  const { fitView: vueFlowFitView, setCenter } = useVueFlow()
  
  const searchQuery = ref('')
  
  // 适应画布
  const handleFitView = () => {
	vueFlowFitView({ padding: 0.2, duration: 300 })
  }
  
  // 居中导图
  const handleCenterView = () => {
	setCenter(0, 0, { zoom: 1, duration: 300 })
  }
  
  // 刷新导图（触发 store 轮询）
  const handleRefresh = async () => {
	await store.loadMap()
  }
  
  // 搜索节点（后续可扩展高亮功能）
  const handleSearch = () => {
	if (!searchQuery.value.trim() || !store.currentMap) return
	console.log('[搜索] 关键词：', searchQuery.value)
	// TODO: 成员B 可在此处调用 nodeApi.searchNodes 并高亮节点
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
  
  button {
	padding: 6px 14px;
	background: #fff;
	border: 1px solid #ddd;
	border-radius: 6px;
	cursor: pointer;
	font-size: 14px;
	transition: all 0.2s;
  }
  
  button:hover {
	background: #f0f0f0;
	border-color: #1890ff;
  }
  
  .search-input {
	padding: 6px 14px;
	border: 1px solid #ddd;
	border-radius: 6px;
	width: 180px;
	font-size: 14px;
	outline: none;
  }
  
  .search-input:focus {
	border-color: #1890ff;
  }
  </style>