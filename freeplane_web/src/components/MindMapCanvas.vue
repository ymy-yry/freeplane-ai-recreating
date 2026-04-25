<template>
	<div class="mindmap-container">
	  <VueFlow
		v-model:nodes="nodes"
		v-model:edges="edges"
		fit-view
		:min-zoom="0.2"
		:max-zoom="2"
		@node-double-click="handleDoubleClick"
		@node-click="handleNodeClick"
		:nodes-draggable="true"
		:edges-updatable="false"
		:connection-line-style="{ stroke: '#94a3b8', strokeWidth: 1.5 }"
		:default-edge-options="{ type: 'smoothstep' }"
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
		@fold="handleFoldFromContext"
		@unfold="handleUnfoldFromContext"
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
  
  const store = useMapStore()
  const aiStore = useAIStore()
  const vueFlow = useVueFlow()
  
  const nodes = ref<Node[]>([])
  const edges = ref<Edge[]>([])
  const selectedNodeId = ref<string>('')
  
  const editPanel = ref({ visible: false, nodeId: '', text: '' })
  
  const actionModal = ref({
	visible: false,
	title: '',
	mode: 'choose' as 'choose' | 'input' | 'delete',
	targetNodeId: ''
  })

  const contextMenu = ref({
	visible: false,
	x: 0,
	y: 0,
	nodeId: ''
  })
  
  // **加强版更新**：每次都生成全新 nodes/edges 数组 + 控制 fitView 行为
  const updateFlow = async () => {
	if (!store.currentMap?.root) return

	const { nodes: newNodes, edges: newEdges } = treeToFlow(store.currentMap.root)

	// 使用自动布局后的坐标，保证结构稳定、可读性更高
	nodes.value = newNodes.map((node) => {
	  // 检查节点是否被选中
	  const isSelected = node.id === selectedNodeId.value;
	  
	  // 创建基本节点对象
	  const updatedNode = { ...node, selected: isSelected };
	  
	  // 如果节点被选中，增强边框样式
	  if (isSelected) {
		// 修改节点的样式以加深边框
		if (updatedNode.style) {
		  updatedNode.style = {
			...updatedNode.style,
			border: '3px solid #1e3a8a', // 深蓝色边框表示选中状态
			boxShadow: '0 0 0 2px rgba(30, 58, 138, 0.3), ' + (updatedNode.style.boxShadow || '0 1px 3px rgba(15, 23, 42, 0.06)')
		  };
		}
	  }
	  
	  return updatedNode;
	});
	if (selectedNodeId.value && !nodes.value.some((node) => node.id === selectedNodeId.value)) {
	  selectedNodeId.value = ''
	}
	edges.value = [...newEdges]

	// 刷新布局但不强制居中，仅在必要时居中
	await nextTick()
	if (!vueFlow.getNodes().length || nodes.value.length !== vueFlow.getNodes().length) { // 如果是首次加载或节点数量变化很大，则居中
		vueFlow.fitView({ padding: 0.15, duration: 200 })
	}
	// 否则只更新节点而不改变视图位置，这样就不会影响当前视图位置
  }
  
  const updateSelectedNodeId = () => {
	const selected = vueFlow.nodes.value.find((n: any) => n.selected)
	selectedNodeId.value = selected?.id || ''
  }

  const handleNodeClick = (event: any) => {
	if (!event?.node?.id) return
	selectedNodeId.value = event.node.id
  }

  const isEditableTarget = (target: EventTarget | null) => {
	if (!(target instanceof HTMLElement)) return false
	const tagName = target.tagName
	return (
	  tagName === 'INPUT' ||
	  tagName === 'TEXTAREA' ||
	  target.isContentEditable ||
	  !!target.closest('[contenteditable="true"]')
	)
  }

  const findNodeInMap = (nodeId: string) => {
	if (!store.currentMap?.root) return null
	const stack = [store.currentMap.root]
	while (stack.length) {
	  const current = stack.pop()!
	  if (current.id === nodeId) return current
	  if (current.children?.length) stack.push(...current.children)
	}
	return null
  }

  const getActiveNodeId = () => {
	const selected = vueFlow.nodes.value.find((n: any) => n.selected)
	if (selected?.id) return selected.id
	if (selectedNodeId.value) return selectedNodeId.value
	return ''
  }
  
  const handleKeyDown = (e: KeyboardEvent) => {
	if (e.key !== 'Tab') return
	if (e.repeat) return
	if (editPanel.value.visible || actionModal.value.visible) return
	if (isEditableTarget(e.target)) return
	e.preventDefault()
  
	const activeNodeId = getActiveNodeId()
	if (!activeNodeId) {
	  if (nodes.value.length > 0) {
		actionModal.value = { visible: true, title: '新建子节点', mode: 'input', targetNodeId: nodes.value[0].id }
	  }
	  return
	}
  
	const mapNode = findNodeInMap(activeNodeId)
	if (!mapNode) return
	const isFolded = !!mapNode.folded
  
	if (isFolded) {
	  void store.toggleNodeFold(activeNodeId)
	} else {
	  actionModal.value = { visible: true, title: '请选择操作', mode: 'choose', targetNodeId: activeNodeId }
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
	if (actionModal.mode === 'choose') {
	  closeActionModal()
	  actionModal.value = { visible: true, title: '删除节点', mode: 'delete', targetNodeId: actionModal.value.targetNodeId }
	} else if (actionModal.mode === 'delete') {
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
	selectedNodeId.value = node.id
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
	selectedNodeId.value = event.node.id
  }

  const hideContextMenu = () => {
	contextMenu.value.visible = false
  }

  const handleAIExpand = async (nodeId: string) => {
	await aiStore.expandNode({ mapId: store.currentMap?.mapId, nodeId })
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

  const handleFoldFromContext = (nodeId: string) => {
    void store.toggleNodeFold(nodeId)
    hideContextMenu()
  }

  const handleUnfoldFromContext = (nodeId: string) => {
    void store.toggleNodeFold(nodeId)
    hideContextMenu()
  }

  const deleteFromContext = (nodeId: string) => {
    store.deleteNode(nodeId)
    hideContextMenu()
  }
  </script>
  
  <style scoped>
  .mindmap-container {
	width: 100%;
	height: 100vh;
	background: #f8fafc;
  }
  </style>