<template>
	<div id="app">
	  <MindMapCanvas />
	  
	  <!-- AI 面板（可选显示） -->
	  <div v-if="aiStore.panelVisible" class="ai-panel-overlay">
		<AiChatPanel v-if="aiStore.aiMode === 'chat'" />
		<AiBuildPanel v-else-if="aiStore.aiMode === 'build'" />
		<AiAutoPanel v-else />
	  </div>
	</div>
  </template>
  
  <script setup lang="ts">
  import { onMounted } from 'vue'
  import MindMapCanvas from './components/MindMapCanvas.vue'
  import AiChatPanel from './components/ai/AiChatPanel.vue'
  import AiBuildPanel from './components/ai/AiBuildPanel.vue'
  import AiAutoPanel from './components/ai/AiAutoPanel.vue'
  import { useAIStore } from './stores/aiStore'
  import { useMapStore } from './stores/mapStore'
  
  const aiStore = useAIStore()
  const mapStore = useMapStore()
  
  onMounted(async () => {
	// 初始化地图列表
	await mapStore.loadAllMaps()
  })
  </script>
  
  <style>
  * { 
	margin: 0; 
	padding: 0; 
	box-sizing: border-box; 
  }
  
  #app { 
	height: 100vh; 
	width: 100vw; 
	overflow: hidden; 
	font-family: system-ui, sans-serif; 
  }
  
  .ai-panel-overlay {
	position: absolute;
	right: 0;
	top: 0;
	bottom: 0;
	width: 450px;
	box-shadow: -2px 0 12px rgba(0,0,0,0.15);
	z-index: 1000;
	background: white;
  }
  </style>