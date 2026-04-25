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
		@node-drag-stop="handleNodeDragStop"
		@node-context-menu="handleNodeContextMenu"
		@pane-click="hideContextMenu"
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

	  <AiAutoPanel :map-id="store.currentMap?.mapId" :selected-node-id="selectedNodeId" />
	  <AiChatPanel :map-id="store.currentMap?.mapId" :selected-node-id="selectedNodeId" />
	  <AiBuildPanel :map-id="store.currentMap?.mapId" :selected-node-id="selectedNodeId" />
	  <NodeContextMenu
		:visible="contextMenu.visible"
		:x="contextMenu.x"
		:y="contextMenu.y"
		:node-id="contextMenu.nodeId"
		@close="hideContextMenu"
		@ai-expand="handleAIExpand"
		@ai-summarize="handleAISummarize"
		@edit="openEditFromContext"
		@create-child="openCreateChildFromContext"
		@delete="deleteFromContext"
	  />
	</div>
  </template>
  
  <script setup lang="ts">
  import { ref, onMounted, onUnmounted, watch, nextTick, provide } from 'vue'
  import { VueFlow, useVueFlow, type Node, type Edge } from '@vue-flow/core'
  import { Background } from '@vue-flow/background'
  import { Controls } from '@vue-flow/controls'
  
  import { useMapStore } from '@/stores/mapStore'
  import { useAIStore } from '@/stores/aiStore'
  import NodeEditPanel from './NodeEditPanel.vue'
  import Toolbar from './Toolbar.vue'
  import ActionModal from './ActionModal.vue'
  import AiAutoPanel from './ai/AiAutoPanel.vue'
  import AiChatPanel from './ai/AiChatPanel.vue'
  import AiBuildPanel from './ai/AiBuildPanel.vue'
  import NodeContextMenu from './NodeContextMenu.vue'
  import { treeToFlow } from '@/utils/treeToFlow'
  import { applyNodePositions, hasNodePositions, saveNodePosition } from '@/utils/nodePositionCache'
  
  const store = useMapStore()
  const aiStore = useAIStore()
  const vueFlow = useVueFlow()
  
  const nodes = ref<Node[]>([])
  const edges = ref<Edge[]>([])
  const selectedNodeId = ref<string>('')
  const lastRenderedMapId = ref('')
  
  const editPanel = ref({ visible: false, nodeId: '', text: '' })
  
  const actionModal = ref({
	visible: false,
	title: '',
	mode: 'choose' as 'choose' | 'input' | 'delete',
	targetNodeId: '' as string
  })

  const contextMenu = ref({
	visible: false,
	x: 0,
	y: 0,
	nodeId: ''
  })
  
  // **加强版更新**：每次都生成全新 nodes/edges 数组 + 强制 fitView
  const updateFlow = async () => {
	if (!store.currentMap?.root) return
  
	const mapId = store.currentMap.mapId
	const previousNodeCount = nodes.value.length
	const hadCachedPositions = hasNodePositions(mapId)
	const { nodes: newNodes, edges: newEdges } = treeToFlow(store.currentMap.root)

	nodes.value = applyNodePositions(mapId, [...newNodes])
	edges.value = [...newEdges]
  
	await nextTick()
	const mapSwitched = lastRenderedMapId.value !== mapId
	const structureChanged = previousNodeCount !== newNodes.length
	if (mapSwitched || (structureChanged && !hadCachedPositions)) {
	  vueFlow.fitView({ padding: 0.15, duration: 200 })
	}
	lastRenderedMapId.value = mapId
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
	aiStore.init()
	window.addEventListener('keydown', handleKeyDown)
	window.addEventListener('click', hideContextMenu)
  })
  
  onUnmounted(() => {
	store.stopPolling()
	window.removeEventListener('keydown', handleKeyDown)
	window.removeEventListener('click', hideContextMenu)
  })
  
  watch(() => store.currentMap, updateFlow, { deep: true })
  watch(() => vueFlow.nodes.value.map((n: any) => ({ id: n.id, selected: n.selected })), updateSelectedNodeId, { deep: true })
  
  const focusNode = (nodeId: string) => {
	const target = vueFlow.findNode(nodeId)
	if (!target) return
	vueFlow.setCenter(target.position.x, target.position.y, { zoom: 1.2, duration: 300 })
  }

  provide('selectedNodeId', selectedNodeId)

  const handleNodeContextMenu = (event: any) => {
	if (!event?.event || !event?.node) return
	event.event.preventDefault()
	contextMenu.value = {
	  visible: true,
	  x: event.event.clientX,
	  y: event.event.clientY,
	  nodeId: event.node.id
	}
  }

  const hideContextMenu = () => {
	contextMenu.value.visible = false
  }

  const handleAIExpand = async (nodeId: string) => {
	await aiStore.expandNode({ mapId: store.currentMap?.mapId, nodeId })
  }

  const handleNodeDragStop = (event: any) => {
	const mapId = store.currentMap?.mapId
	const nodeId = event?.node?.id
	const position = event?.node?.position
	if (!mapId || !nodeId || !position) return
	saveNodePosition(mapId, nodeId, position)
  }

  const handleAISummarize = async (nodeId: string) => {
	await aiStore.summarize({ mapId: store.currentMap?.mapId, nodeId, writeToNote: true })
  }

  const openEditFromContext = (nodeId: string) => {
	const node = vueFlow.findNode(nodeId)
	editPanel.value = { visible: true, nodeId, text: (node?.data as any)?.label || '' }
	hideContextMenu()
  }

  const openCreateChildFromContext = (nodeId: string) => {
	actionModal.value = { visible: true, title: '新建子节点', mode: 'input', targetNodeId: nodeId }
	hideContextMenu()
  }

  const deleteFromContext = (nodeId: string) => {
	void store.deleteNode(nodeId)
	hideContextMenu()
  }
  </script>
  
  <style scoped>
  .mindmap-container { 
	height: 100vh; 
	width: 100vw; 
	position: relative; 
  }
  </style>