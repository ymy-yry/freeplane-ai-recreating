/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref } from 'vue';
import { useMapStore } from '@/stores/mapStore';
import { exportToMM, exportToJSON, importMindMapFile, showExportDialog } from '@/utils/exportUtils';
import { useAIStore } from '@/stores/aiStore';
const props = defineProps();
const store = useMapStore();
const aiStore = useAIStore();
const searchQuery = ref('');
const importInput = ref(null);
const handleFitView = () => {
    props.vueFlow.fitView({ padding: 0.2, duration: 300 });
};
const handleCenterView = () => {
    props.vueFlow.setCenter(0, 0, { zoom: 1, duration: 300 });
};
const handleRefresh = async () => {
    store.resumePolling();
    await store.loadMap();
};
const handleExport = () => {
    if (!store.currentMap) {
        alert('当前没有打开的导图');
        return;
    }
    showExportDialog(store.currentMap);
};
const handleExportMM = () => {
    if (!store.currentMap) {
        alert('当前没有打开的导图');
        return;
    }
    exportToMM(store.currentMap);
};
const handleExportJSON = () => {
    if (!store.currentMap) {
        alert('当前没有打开的导图');
        return;
    }
    exportToJSON(store.currentMap);
};
const triggerImport = () => {
    importInput.value?.click();
};
const handleImportFile = async (event) => {
    const input = event.target;
    const file = input.files?.[0];
    if (!file)
        return;
    try {
        const importedMap = await importMindMapFile(file);
        store.pausePolling();
        store.setCurrentMap(importedMap);
        props.vueFlow.fitView({ padding: 0.22, duration: 250 });
    }
    catch (error) {
        const message = error instanceof Error ? error.message : '导入失败';
        alert(message);
    }
    finally {
        input.value = '';
    }
};
const handleSearch = async () => {
    const query = searchQuery.value.trim();
    if (!query || !store.currentMap?.mapId)
        return;
    try {
        const res = await aiStore.keywordSearch({ query, mapId: store.currentMap.mapId });
        const first = res.results?.[0];
        if (!first)
            return;
        const node = props.vueFlow.findNode(first.nodeId);
        if (!node)
            return;
        props.vueFlow.setCenter(node.position.x, node.position.y, { zoom: 1.2, duration: 250 });
    }
    finally {
        searchQuery.value = '';
    }
};
const handleToggleAI = () => {
    aiStore.togglePanel();
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['search-input']} */ ;
/** @type {__VLS_StyleScopedClasses['export-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['import-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['search-input']} */ ;
/** @type {__VLS_StyleScopedClasses['mode-btn']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleFitView) },
    title: "适应画布",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleCenterView) },
    title: "居中导图",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleRefresh) },
    title: "刷新导图",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleToggleAI) },
    title: "AI 面板",
});
if (__VLS_ctx.aiStore.panelVisible) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "mode-tabs" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.aiStore.panelVisible))
                    return;
                __VLS_ctx.aiStore.setMode('auto');
            } },
        ...{ class: "mode-btn" },
        ...{ class: ({ active: __VLS_ctx.aiStore.aiMode === 'auto' }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.aiStore.panelVisible))
                    return;
                __VLS_ctx.aiStore.setMode('chat');
            } },
        ...{ class: "mode-btn" },
        ...{ class: ({ active: __VLS_ctx.aiStore.aiMode === 'chat' }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                if (!(__VLS_ctx.aiStore.panelVisible))
                    return;
                __VLS_ctx.aiStore.setMode('build');
            } },
        ...{ class: "mode-btn" },
        ...{ class: ({ active: __VLS_ctx.aiStore.aiMode === 'build' }) },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar-divider" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleExport) },
    title: "导出导图",
    ...{ class: "export-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleExportMM) },
    title: "导出为 Freeplane 格式",
    ...{ class: "export-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleExportJSON) },
    title: "导出为 JSON 备份",
    ...{ class: "export-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.triggerImport) },
    title: "导入 JSON 或 MM",
    ...{ class: "import-btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.handleImportFile) },
    ref: "importInput",
    type: "file",
    accept: ".json,.mm,.xml,application/json,application/xml,text/xml",
    ...{ class: "hidden-input" },
});
/** @type {typeof __VLS_ctx.importInput} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "toolbar-divider" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onKeyup: (__VLS_ctx.handleSearch) },
    value: (__VLS_ctx.searchQuery),
    type: "text",
    placeholder: "搜索节点...",
    ...{ class: "search-input" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.handleSearch) },
});
/** @type {__VLS_StyleScopedClasses['toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['mode-tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['mode-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['mode-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['mode-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['export-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['export-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['export-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['import-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['hidden-input']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar-divider']} */ ;
/** @type {__VLS_StyleScopedClasses['search-input']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            aiStore: aiStore,
            searchQuery: searchQuery,
            importInput: importInput,
            handleFitView: handleFitView,
            handleCenterView: handleCenterView,
            handleRefresh: handleRefresh,
            handleExport: handleExport,
            handleExportMM: handleExportMM,
            handleExportJSON: handleExportJSON,
            triggerImport: triggerImport,
            handleImportFile: handleImportFile,
            handleSearch: handleSearch,
            handleToggleAI: handleToggleAI,
        };
    },
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
