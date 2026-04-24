<template>
	<div class="mindmap-container">
	  <VueFlow
		v-model:nodes="nodes"
		v-model:edges="edges"
		fit-view
		:min-zoom="0.2"
		:max-zoom="2"
		@node-double-click="handleDoubleClick"
		:nodes-draggable="true"
		:edges-updatable="false"
		:connection-line-style="{ stroke: '#666', strokeWidth: 2 }"
		:default-edge-options="{ type: 'bezier' }"
	  >
		<Background variant="dots" />
		<Controls />
	  </VueFlow>
  
	  <NodeEditPanel
		:visible="editPanel.visible"
		:node-id="editPanel.nodeId"
		:initial-text="editPanel.text"
		@save="handleSaveEdit"
		@cancel="editPanel.visible = false"
	  />
  
	  <ActionModal
		:visible="actionModal.visible"
		:title="actionModal.title"
		:mode="actionModal.mode"
		@new-child="handleNewChildConfirm"
		@fold="handleFoldConfirm"
		@delete="handleDelete"
		@confirm-input="handleInputConfirm"
		@cancel="closeActionModal"
	  />
  
	  <Toolbar :vue-flow="vueFlow" />

	  <AiPanel
		:map-id="store.currentMap?.mapId"
		:selected-node-id="selectedNodeId"
		@refresh-map="store.loadMap"
		@focus-node="focusNode"
	  />
	</div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
  import { VueFlow, useVueFlow, type Node, type Edge } from '@vue-flow/core'
  import { Background } from '@vue-flow/background'
  import { Controls } from '@vue-flow/controls'
  
  import { useMapStore } from '@/stores/mapStore'
  import NodeEditPanel from './NodeEditPanel.vue'
  import Toolbar from './Toolbar.vue'
  import ActionModal from './ActionModal.vue'
  import AiPanel from './ai/AiPanel.vue'
  import { treeToFlow } from '@/utils/treeToFlow'
  
  const store = useMapStore()
  const vueFlow = useVueFlow()
  
  const nodes = ref<Node[]>([])
  const edges = ref<Edge[]>([])
  const selectedNodeId = ref<string>('')
  
  const editPanel = ref({ visible: false, nodeId: '', text: '' })
  
  const actionModal = ref({
	visible: false,
	title: '',
	mode: 'choose' as 'choose' | 'input' | 'delete',
	targetNodeId: '' as string
  })
  
  // **加强版更新**：每次都生成全新 nodes/edges 数组 + 强制 fitView
  const updateFlow = async () => {
	if (!store.currentMap?.root) return
  
	const { nodes: newNodes, edges: newEdges } = treeToFlow(store.currentMap.root)
  
	// 使用全新数组引用，确保 Vue Flow 完全重新渲染
	nodes.value = newNodes.map((newNode: Node) => {
	  const existing = nodes.value.find((n: Node) => n.id === newNode.id)
	  return existing ? { ...newNode, position: { ...existing.position } } : newNode
	})
  
	edges.value = [...newEdges]
  
	// 强制刷新布局
	await nextTick()
	vueFlow.fitView({ padding: 0.15, duration: 200 })
  }
  
  const updateSelectedNodeId = () => {
	const selected = vueFlow.nodes.value.find((n: any) => n.selected)
	selectedNodeId.value = selected?.id || ''
  }
  
  const handleKeyDown = (e: KeyboardEvent) => {
	if (e.key !== 'Tab') return
	e.preventDefault()
  
	const selectedNode = vueFlow.nodes.value.find((n: any) => n.selected)
	if (!selectedNode) {
	  if (nodes.value.length > 0) {
		actionModal.value = { visible: true, title: '新建子节点', mode: 'input', targetNodeId: nodes.value[0].id }
	  }
	  return
	}
  
	const isFolded = !!(selectedNode.data as any)?.folded || !!(selectedNode.data as any)?.originalNode?.folded
  
	if (isFolded) {
	  store.toggleNodeFold(selectedNode.id)
	} else {
	  actionModal.value = { visible: true, title: '请选择操作', mode: 'choose', targetNodeId: selectedNode.id }
	}
  }
  
  const closeActionModal = () => { actionModal.value.visible = false }
  
  const handleNewChildConfirm = () => {
	closeActionModal()
	actionModal.value = { visible: true, title: '新建子节点', mode: 'input', targetNodeId: actionModal.value.targetNodeId }
  }
  
  const handleFoldConfirm = () => {
	closeActionModal()
	if (actionModal.value.targetNodeId) {
	  store.toggleNodeFold(actionModal.value.targetNodeId)
	}
  }
  
  const handleDelete = () => {
	if (actionModal.value.mode === 'choose') {
	  closeActionModal()
	  actionModal.value = { visible: true, title: '删除节点', mode: 'delete', targetNodeId: actionModal.value.targetNodeId }
	} else if (actionModal.value.mode === 'delete') {
	  if (actionModal.value.targetNodeId) {
		store.deleteNode(actionModal.value.targetNodeId)
	  }
	  closeActionModal()
	}
  }
  
  const handleInputConfirm = (text: string) => {
	closeActionModal()
	if (actionModal.value.targetNodeId && text) {
	  store.createNode(actionModal.value.targetNodeId, text)
	}
  }
  
  const handleDoubleClick = (event: any) => {
	const node = event.node
	editPanel.value = { visible: true, nodeId: node.id, text: (node.data as any)?.label || '' }
  }
  
  const handleSaveEdit = (nodeId: string, text: string) => {
	store.editNode(nodeId, text)
	editPanel.value.visible = false
  }
  
  onMounted(() => {
	store.loadMap()
	store.startPolling()
	window.addEventListener('keydown', handleKeyDown)
  })
  
  onUnmounted(() => {
	store.stopPolling()
	window.removeEventListener('keydown', handleKeyDown)
  })
  
  watch(() => store.currentMap, updateFlow, { deep: true })
  watch(() => vueFlow.nodes.value.map((n: any) => ({ id: n.id, selected: n.selected })), updateSelectedNodeId, { deep: true })
  
  const focusNode = (nodeId: string) => {
	const target = vueFlow.findNode(nodeId)
	if (!target) return
	vueFlow.setCenter(target.position.x, target.position.y, { zoom: 1.2, duration: 300 })
  }
  </script>
  
  <style scoped>
  .mindmap-container { 
	height: 100vh; 
	width: 100vw; 
	position: relative; 
  }
  </style>