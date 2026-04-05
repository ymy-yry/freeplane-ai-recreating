<template>
	<div class="mindmap-container">
	  <VueFlow
		v-model:nodes="nodes"
		v-model:edges="edges"
		fit-view
		:min-zoom="0.2"
		:max-zoom="2"
		@node-double-click="handleDoubleClick"
		@node-context-menu="handleContextMenu"
		@nodes-change="onNodesChange"
	  >
		<Background variant="dots" />
		<Controls />
	  </VueFlow>
  
	  <!-- 右键菜单 -->
	  <NodeContextMenu
		:visible="contextMenu.visible"
		:x="contextMenu.x"
		:y="contextMenu.y"
		:node-id="contextMenu.nodeId"
		@edit="handleEdit"
		@create-child="handleCreateChild"
		@delete="handleDelete"
		@close="contextMenu.visible = false"
	  />
  
	  <!-- 编辑面板 -->
	  <NodeEditPanel
		:visible="editPanel.visible"
		:node-id="editPanel.nodeId"
		:initial-text="editPanel.text"
		@save="handleSaveEdit"
		@cancel="editPanel.visible = false"
	  />
  
	  <!-- 工具栏 -->
	  <Toolbar />
	</div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, onUnmounted, watch } from 'vue'
  import { VueFlow } from '@vue-flow/core'
  import { Background } from '@vue-flow/background'
  import { Controls } from '@vue-flow/controls'
  import type { Node, Edge } from '@vue-flow/core'
  
  import { useMapStore } from '@/stores/mapStore'
  import NodeContextMenu from './NodeContextMenu.vue'
  import NodeEditPanel from './NodeEditPanel.vue'
  import Toolbar from './Toolbar.vue'
  import { treeToFlow } from '@/utils/treeToFlow'
  
  const store = useMapStore()
  
  const nodes = ref<Node[]>([])
  const edges = ref<Edge[]>([])
  
  const contextMenu = ref({
	visible: false,
	x: 0,
	y: 0,
	nodeId: ''
  })
  
  const editPanel = ref({
	visible: false,
	nodeId: '',
	text: ''
  })
  
  const updateFlow = () => {
	if (!store.currentMap?.root) return
	const { nodes: n, edges: e } = treeToFlow(store.currentMap.root)
	nodes.value = n
	edges.value = e
  }
  
  const handleDoubleClick = (event: any) => {
	const node = event.node
	editPanel.value = {
	  visible: true,
	  nodeId: node.id,
	  text: node.data?.text || ''
	}
  }
  
  const handleContextMenu = (event: any) => {
	event.preventDefault()
	contextMenu.value = {
	  visible: true,
	  x: event.event.clientX,
	  y: event.event.clientY,
	  nodeId: event.node.id
	}
  }
  
  const handleEdit = (nodeId: string) => {
	const node = nodes.value.find(n => n.id === nodeId)
	if (node) {
	  editPanel.value = {
		visible: true,
		nodeId,
		text: node.data?.text || ''
	  }
	}
	contextMenu.value.visible = false
  }
  
  const handleCreateChild = (nodeId: string) => {
	const text = prompt('请输入新子节点文本：')
	if (text && text.trim()) {
	  store.createNode(nodeId, text.trim())
	}
	contextMenu.value.visible = false
  }
  
  const handleDelete = (nodeId: string) => {
	if (confirm('确认删除该节点及其所有子节点吗？')) {
	  store.deleteNode(nodeId)
	}
	contextMenu.value.visible = false
  }
  
  const handleSaveEdit = (nodeId: string, text: string) => {
	store.editNode(nodeId, text)
	editPanel.value.visible = false
  }
  
  const onNodesChange = () => {
	// 拖拽后可在此扩展本地位置保存逻辑
  }
  
  // 生命周期
  onMounted(() => {
	store.loadMap()
	store.startPolling()
  })
  
  onUnmounted(() => {
	store.stopPolling()
  })
  
  // 数据更新时刷新画布
  watch(() => store.currentMap, updateFlow, { immediate: true, deep: true })
  </script>
  
  <style scoped>
  .mindmap-container {
	height: 100vh;
	width: 100vw;
	position: relative;
  }
  </style>