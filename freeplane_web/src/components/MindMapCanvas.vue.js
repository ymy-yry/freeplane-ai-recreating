/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, onMounted, onUnmounted, watch, nextTick, provide } from 'vue';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { useMapStore } from '@/stores/mapStore';
import { useAIStore } from '@/stores/aiStore';
import NodeEditPanel from './NodeEditPanel.vue';
import Toolbar from './Toolbar.vue';
import ActionModal from './ActionModal.vue';
import AiAutoPanel from './ai/AiAutoPanel.vue';
import AiChatPanel from './ai/AiChatPanel.vue';
import AiBuildPanel from './ai/AiBuildPanel.vue';
import NodeContextMenu from './NodeContextMenu.vue';
import { treeToFlow } from '@/utils/treeToFlow';
import { applyNodePositions, hasNodePositions, saveNodePosition } from '@/utils/nodePositionCache';
const store = useMapStore();
const aiStore = useAIStore();
const vueFlow = useVueFlow();
const nodes = ref([]);
const edges = ref([]);
const selectedNodeId = ref('');
const lastRenderedMapId = ref('');
const editPanel = ref({ visible: false, nodeId: '', text: '' });
const actionModal = ref({
    visible: false,
    title: '',
    mode: 'choose',
    targetNodeId: ''
});
const contextMenu = ref({
    visible: false,
    x: 0,
    y: 0,
    nodeId: ''
});
// **加强版更新**：每次都生成全新 nodes/edges 数组 + 强制 fitView
const updateFlow = async () => {
    if (!store.currentMap?.root)
        return;
    const mapId = store.currentMap.mapId;
    const previousNodeCount = nodes.value.length;
    const hadCachedPositions = hasNodePositions(mapId);
    const { nodes: newNodes, edges: newEdges } = treeToFlow(store.currentMap.root);
    nodes.value = applyNodePositions(mapId, [...newNodes]);
    edges.value = [...newEdges];
    await nextTick();
    const mapSwitched = lastRenderedMapId.value !== mapId;
    const structureChanged = previousNodeCount !== newNodes.length;
    if (mapSwitched || (structureChanged && !hadCachedPositions)) {
        vueFlow.fitView({ padding: 0.15, duration: 200 });
    }
    lastRenderedMapId.value = mapId;
};
const updateSelectedNodeId = () => {
    const selected = vueFlow.nodes.value.find((n) => n.selected);
    selectedNodeId.value = selected?.id || '';
};
const handleKeyDown = (e) => {
    if (e.key !== 'Tab')
        return;
    e.preventDefault();
    const selectedNode = vueFlow.nodes.value.find((n) => n.selected);
    if (!selectedNode) {
        if (nodes.value.length > 0) {
            actionModal.value = { visible: true, title: '新建子节点', mode: 'input', targetNodeId: nodes.value[0].id };
        }
        return;
    }
    const isFolded = !!selectedNode.data?.folded || !!selectedNode.data?.originalNode?.folded;
    if (isFolded) {
        store.toggleNodeFold(selectedNode.id);
    }
    else {
        actionModal.value = { visible: true, title: '请选择操作', mode: 'choose', targetNodeId: selectedNode.id };
    }
};
const closeActionModal = () => { actionModal.value.visible = false; };
const handleNewChildConfirm = () => {
    closeActionModal();
    actionModal.value = { visible: true, title: '新建子节点', mode: 'input', targetNodeId: actionModal.value.targetNodeId };
};
const handleFoldConfirm = () => {
    closeActionModal();
    if (actionModal.value.targetNodeId) {
        store.toggleNodeFold(actionModal.value.targetNodeId);
    }
};
const handleDelete = () => {
    if (actionModal.value.mode === 'choose') {
        closeActionModal();
        actionModal.value = { visible: true, title: '删除节点', mode: 'delete', targetNodeId: actionModal.value.targetNodeId };
    }
    else if (actionModal.value.mode === 'delete') {
        if (actionModal.value.targetNodeId) {
            store.deleteNode(actionModal.value.targetNodeId);
        }
        closeActionModal();
    }
};
const handleInputConfirm = (text) => {
    closeActionModal();
    if (actionModal.value.targetNodeId && text) {
        store.createNode(actionModal.value.targetNodeId, text);
    }
};
const handleDoubleClick = (event) => {
    const node = event.node;
    editPanel.value = { visible: true, nodeId: node.id, text: node.data?.label || '' };
};
const handleSaveEdit = (nodeId, text) => {
    store.editNode(nodeId, text);
    editPanel.value.visible = false;
};
onMounted(() => {
    store.loadMap();
    store.startPolling();
    aiStore.init();
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('click', hideContextMenu);
});
onUnmounted(() => {
    store.stopPolling();
    window.removeEventListener('keydown', handleKeyDown);
    window.removeEventListener('click', hideContextMenu);
});
watch(() => store.currentMap, updateFlow, { deep: true });
watch(() => vueFlow.nodes.value.map((n) => ({ id: n.id, selected: n.selected })), updateSelectedNodeId, { deep: true });
const focusNode = (nodeId) => {
    const target = vueFlow.findNode(nodeId);
    if (!target)
        return;
    vueFlow.setCenter(target.position.x, target.position.y, { zoom: 1.2, duration: 300 });
};
provide('selectedNodeId', selectedNodeId);
const handleNodeContextMenu = (event) => {
    if (!event?.event || !event?.node)
        return;
    event.event.preventDefault();
    contextMenu.value = {
        visible: true,
        x: event.event.clientX,
        y: event.event.clientY,
        nodeId: event.node.id
    };
};
const hideContextMenu = () => {
    contextMenu.value.visible = false;
};
const handleAIExpand = async (nodeId) => {
    await aiStore.expandNode({ mapId: store.currentMap?.mapId, nodeId });
};
const handleNodeDragStop = (event) => {
    const mapId = store.currentMap?.mapId;
    const nodeId = event?.node?.id;
    const position = event?.node?.position;
    if (!mapId || !nodeId || !position)
        return;
    saveNodePosition(mapId, nodeId, position);
};
const handleAISummarize = async (nodeId) => {
    await aiStore.summarize({ mapId: store.currentMap?.mapId, nodeId, writeToNote: true });
};
const openEditFromContext = (nodeId) => {
    const node = vueFlow.findNode(nodeId);
    editPanel.value = { visible: true, nodeId, text: node?.data?.label || '' };
    hideContextMenu();
};
const openCreateChildFromContext = (nodeId) => {
    actionModal.value = { visible: true, title: '新建子节点', mode: 'input', targetNodeId: nodeId };
    hideContextMenu();
};
const deleteFromContext = (nodeId) => {
    void store.deleteNode(nodeId);
    hideContextMenu();
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "mindmap-container" },
});
const __VLS_0 = {}.VueFlow;
/** @type {[typeof __VLS_components.VueFlow, typeof __VLS_components.VueFlow, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ 'onNodeDoubleClick': {} },
    ...{ 'onNodeDragStop': {} },
    ...{ 'onNodeContextMenu': {} },
    ...{ 'onPaneClick': {} },
    nodes: (__VLS_ctx.nodes),
    edges: (__VLS_ctx.edges),
    fitView: true,
    minZoom: (0.2),
    maxZoom: (2),
    nodesDraggable: (true),
    edgesUpdatable: (false),
    connectionLineStyle: ({ stroke: '#666', strokeWidth: 2 }),
    defaultEdgeOptions: ({ type: 'bezier' }),
}));
const __VLS_2 = __VLS_1({
    ...{ 'onNodeDoubleClick': {} },
    ...{ 'onNodeDragStop': {} },
    ...{ 'onNodeContextMenu': {} },
    ...{ 'onPaneClick': {} },
    nodes: (__VLS_ctx.nodes),
    edges: (__VLS_ctx.edges),
    fitView: true,
    minZoom: (0.2),
    maxZoom: (2),
    nodesDraggable: (true),
    edgesUpdatable: (false),
    connectionLineStyle: ({ stroke: '#666', strokeWidth: 2 }),
    defaultEdgeOptions: ({ type: 'bezier' }),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
let __VLS_4;
let __VLS_5;
let __VLS_6;
const __VLS_7 = {
    onNodeDoubleClick: (__VLS_ctx.handleDoubleClick)
};
const __VLS_8 = {
    onNodeDragStop: (__VLS_ctx.handleNodeDragStop)
};
const __VLS_9 = {
    onNodeContextMenu: (__VLS_ctx.handleNodeContextMenu)
};
const __VLS_10 = {
    onPaneClick: (__VLS_ctx.hideContextMenu)
};
__VLS_3.slots.default;
const __VLS_11 = {}.Background;
/** @type {[typeof __VLS_components.Background, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
    variant: "dots",
}));
const __VLS_13 = __VLS_12({
    variant: "dots",
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
const __VLS_15 = {}.Controls;
/** @type {[typeof __VLS_components.Controls, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({}));
const __VLS_17 = __VLS_16({}, ...__VLS_functionalComponentArgsRest(__VLS_16));
var __VLS_3;
/** @type {[typeof NodeEditPanel, ]} */ ;
// @ts-ignore
const __VLS_19 = __VLS_asFunctionalComponent(NodeEditPanel, new NodeEditPanel({
    ...{ 'onSave': {} },
    ...{ 'onCancel': {} },
    visible: (__VLS_ctx.editPanel.visible),
    nodeId: (__VLS_ctx.editPanel.nodeId),
    initialText: (__VLS_ctx.editPanel.text),
}));
const __VLS_20 = __VLS_19({
    ...{ 'onSave': {} },
    ...{ 'onCancel': {} },
    visible: (__VLS_ctx.editPanel.visible),
    nodeId: (__VLS_ctx.editPanel.nodeId),
    initialText: (__VLS_ctx.editPanel.text),
}, ...__VLS_functionalComponentArgsRest(__VLS_19));
let __VLS_22;
let __VLS_23;
let __VLS_24;
const __VLS_25 = {
    onSave: (__VLS_ctx.handleSaveEdit)
};
const __VLS_26 = {
    onCancel: (...[$event]) => {
        __VLS_ctx.editPanel.visible = false;
    }
};
var __VLS_21;
/** @type {[typeof ActionModal, ]} */ ;
// @ts-ignore
const __VLS_27 = __VLS_asFunctionalComponent(ActionModal, new ActionModal({
    ...{ 'onNewChild': {} },
    ...{ 'onFold': {} },
    ...{ 'onDelete': {} },
    ...{ 'onConfirmInput': {} },
    ...{ 'onCancel': {} },
    visible: (__VLS_ctx.actionModal.visible),
    title: (__VLS_ctx.actionModal.title),
    mode: (__VLS_ctx.actionModal.mode),
}));
const __VLS_28 = __VLS_27({
    ...{ 'onNewChild': {} },
    ...{ 'onFold': {} },
    ...{ 'onDelete': {} },
    ...{ 'onConfirmInput': {} },
    ...{ 'onCancel': {} },
    visible: (__VLS_ctx.actionModal.visible),
    title: (__VLS_ctx.actionModal.title),
    mode: (__VLS_ctx.actionModal.mode),
}, ...__VLS_functionalComponentArgsRest(__VLS_27));
let __VLS_30;
let __VLS_31;
let __VLS_32;
const __VLS_33 = {
    onNewChild: (__VLS_ctx.handleNewChildConfirm)
};
const __VLS_34 = {
    onFold: (__VLS_ctx.handleFoldConfirm)
};
const __VLS_35 = {
    onDelete: (__VLS_ctx.handleDelete)
};
const __VLS_36 = {
    onConfirmInput: (__VLS_ctx.handleInputConfirm)
};
const __VLS_37 = {
    onCancel: (__VLS_ctx.closeActionModal)
};
var __VLS_29;
/** @type {[typeof Toolbar, ]} */ ;
// @ts-ignore
const __VLS_38 = __VLS_asFunctionalComponent(Toolbar, new Toolbar({
    vueFlow: (__VLS_ctx.vueFlow),
}));
const __VLS_39 = __VLS_38({
    vueFlow: (__VLS_ctx.vueFlow),
}, ...__VLS_functionalComponentArgsRest(__VLS_38));
/** @type {[typeof AiAutoPanel, ]} */ ;
// @ts-ignore
const __VLS_41 = __VLS_asFunctionalComponent(AiAutoPanel, new AiAutoPanel({
    mapId: (__VLS_ctx.store.currentMap?.mapId),
    selectedNodeId: (__VLS_ctx.selectedNodeId),
}));
const __VLS_42 = __VLS_41({
    mapId: (__VLS_ctx.store.currentMap?.mapId),
    selectedNodeId: (__VLS_ctx.selectedNodeId),
}, ...__VLS_functionalComponentArgsRest(__VLS_41));
/** @type {[typeof AiChatPanel, ]} */ ;
// @ts-ignore
const __VLS_44 = __VLS_asFunctionalComponent(AiChatPanel, new AiChatPanel({
    mapId: (__VLS_ctx.store.currentMap?.mapId),
    selectedNodeId: (__VLS_ctx.selectedNodeId),
}));
const __VLS_45 = __VLS_44({
    mapId: (__VLS_ctx.store.currentMap?.mapId),
    selectedNodeId: (__VLS_ctx.selectedNodeId),
}, ...__VLS_functionalComponentArgsRest(__VLS_44));
/** @type {[typeof AiBuildPanel, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(AiBuildPanel, new AiBuildPanel({
    mapId: (__VLS_ctx.store.currentMap?.mapId),
    selectedNodeId: (__VLS_ctx.selectedNodeId),
}));
const __VLS_48 = __VLS_47({
    mapId: (__VLS_ctx.store.currentMap?.mapId),
    selectedNodeId: (__VLS_ctx.selectedNodeId),
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
/** @type {[typeof NodeContextMenu, ]} */ ;
// @ts-ignore
const __VLS_50 = __VLS_asFunctionalComponent(NodeContextMenu, new NodeContextMenu({
    ...{ 'onClose': {} },
    ...{ 'onAiExpand': {} },
    ...{ 'onAiSummarize': {} },
    ...{ 'onEdit': {} },
    ...{ 'onCreateChild': {} },
    ...{ 'onDelete': {} },
    visible: (__VLS_ctx.contextMenu.visible),
    x: (__VLS_ctx.contextMenu.x),
    y: (__VLS_ctx.contextMenu.y),
    nodeId: (__VLS_ctx.contextMenu.nodeId),
}));
const __VLS_51 = __VLS_50({
    ...{ 'onClose': {} },
    ...{ 'onAiExpand': {} },
    ...{ 'onAiSummarize': {} },
    ...{ 'onEdit': {} },
    ...{ 'onCreateChild': {} },
    ...{ 'onDelete': {} },
    visible: (__VLS_ctx.contextMenu.visible),
    x: (__VLS_ctx.contextMenu.x),
    y: (__VLS_ctx.contextMenu.y),
    nodeId: (__VLS_ctx.contextMenu.nodeId),
}, ...__VLS_functionalComponentArgsRest(__VLS_50));
let __VLS_53;
let __VLS_54;
let __VLS_55;
const __VLS_56 = {
    onClose: (__VLS_ctx.hideContextMenu)
};
const __VLS_57 = {
    onAiExpand: (__VLS_ctx.handleAIExpand)
};
const __VLS_58 = {
    onAiSummarize: (__VLS_ctx.handleAISummarize)
};
const __VLS_59 = {
    onEdit: (__VLS_ctx.openEditFromContext)
};
const __VLS_60 = {
    onCreateChild: (__VLS_ctx.openCreateChildFromContext)
};
const __VLS_61 = {
    onDelete: (__VLS_ctx.deleteFromContext)
};
var __VLS_52;
/** @type {__VLS_StyleScopedClasses['mindmap-container']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            VueFlow: VueFlow,
            Background: Background,
            Controls: Controls,
            NodeEditPanel: NodeEditPanel,
            Toolbar: Toolbar,
            ActionModal: ActionModal,
            AiAutoPanel: AiAutoPanel,
            AiChatPanel: AiChatPanel,
            AiBuildPanel: AiBuildPanel,
            NodeContextMenu: NodeContextMenu,
            store: store,
            vueFlow: vueFlow,
            nodes: nodes,
            edges: edges,
            selectedNodeId: selectedNodeId,
            editPanel: editPanel,
            actionModal: actionModal,
            contextMenu: contextMenu,
            closeActionModal: closeActionModal,
            handleNewChildConfirm: handleNewChildConfirm,
            handleFoldConfirm: handleFoldConfirm,
            handleDelete: handleDelete,
            handleInputConfirm: handleInputConfirm,
            handleDoubleClick: handleDoubleClick,
            handleSaveEdit: handleSaveEdit,
            handleNodeContextMenu: handleNodeContextMenu,
            hideContextMenu: hideContextMenu,
            handleAIExpand: handleAIExpand,
            handleNodeDragStop: handleNodeDragStop,
            handleAISummarize: handleAISummarize,
            openEditFromContext: openEditFromContext,
            openCreateChildFromContext: openCreateChildFromContext,
            deleteFromContext: deleteFromContext,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
