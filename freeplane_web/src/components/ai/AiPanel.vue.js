/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { onMounted, ref } from 'vue';
import { useAIStore } from '@/stores/aiStore';
import ModelSelector from '@/components/ai/ModelSelector.vue';
import AiChatPanel from '@/components/ai/AiChatPanel.vue';
import ExpandNodeDialog from '@/components/ai/ExpandNodeDialog.vue';
import SummarizePanel from '@/components/ai/SummarizePanel.vue';
import SemanticSearchBar from '@/components/ai/SemanticSearchBar.vue';
const __VLS_props = defineProps();
const emit = defineEmits();
const aiStore = useAIStore();
const tab = ref('chat');
const expandVisible = ref(false);
onMounted(() => {
    void aiStore.init();
});
const openExpand = () => {
    tab.value = 'expand';
    expandVisible.value = true;
};
const handleExpanded = () => {
    expandVisible.value = false;
    emit('refresh-map');
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['ai-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "ai-panel" },
    ...{ class: ({ open: __VLS_ctx.aiStore.panelVisible }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "topbar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "left" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "brand" },
});
/** @type {[typeof ModelSelector, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(ModelSelector, new ModelSelector({}));
const __VLS_1 = __VLS_0({}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "right" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.aiStore.fetchModelList) },
    ...{ class: "ghost" },
    disabled: (__VLS_ctx.aiStore.loading),
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.aiStore.hidePanel) },
    ...{ class: "ghost" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tabs" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.tab = 'chat';
        } },
    ...{ class: "tab" },
    ...{ class: ({ active: __VLS_ctx.tab === 'chat' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.openExpand();
        } },
    ...{ class: "tab" },
    ...{ class: ({ active: __VLS_ctx.tab === 'expand' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.tab = 'summarize';
        } },
    ...{ class: "tab" },
    ...{ class: ({ active: __VLS_ctx.tab === 'summarize' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.tab = 'search';
        } },
    ...{ class: "tab" },
    ...{ class: ({ active: __VLS_ctx.tab === 'search' }) },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "body" },
});
if (__VLS_ctx.tab === 'chat') {
    /** @type {[typeof AiChatPanel, ]} */ ;
    // @ts-ignore
    const __VLS_3 = __VLS_asFunctionalComponent(AiChatPanel, new AiChatPanel({
        mapId: (__VLS_ctx.mapId),
        selectedNodeId: (__VLS_ctx.selectedNodeId),
    }));
    const __VLS_4 = __VLS_3({
        mapId: (__VLS_ctx.mapId),
        selectedNodeId: (__VLS_ctx.selectedNodeId),
    }, ...__VLS_functionalComponentArgsRest(__VLS_3));
}
else if (__VLS_ctx.tab === 'expand') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "placeholder" },
    });
}
else if (__VLS_ctx.tab === 'summarize') {
    /** @type {[typeof SummarizePanel, ]} */ ;
    // @ts-ignore
    const __VLS_6 = __VLS_asFunctionalComponent(SummarizePanel, new SummarizePanel({
        ...{ 'onUpdatedMap': {} },
        mapId: (__VLS_ctx.mapId),
        selectedNodeId: (__VLS_ctx.selectedNodeId),
    }));
    const __VLS_7 = __VLS_6({
        ...{ 'onUpdatedMap': {} },
        mapId: (__VLS_ctx.mapId),
        selectedNodeId: (__VLS_ctx.selectedNodeId),
    }, ...__VLS_functionalComponentArgsRest(__VLS_6));
    let __VLS_9;
    let __VLS_10;
    let __VLS_11;
    const __VLS_12 = {
        onUpdatedMap: (...[$event]) => {
            if (!!(__VLS_ctx.tab === 'chat'))
                return;
            if (!!(__VLS_ctx.tab === 'expand'))
                return;
            if (!(__VLS_ctx.tab === 'summarize'))
                return;
            __VLS_ctx.emit('refresh-map');
        }
    };
    var __VLS_8;
}
else if (__VLS_ctx.tab === 'search') {
    /** @type {[typeof SemanticSearchBar, ]} */ ;
    // @ts-ignore
    const __VLS_13 = __VLS_asFunctionalComponent(SemanticSearchBar, new SemanticSearchBar({
        ...{ 'onPickNode': {} },
        mapId: (__VLS_ctx.mapId),
    }));
    const __VLS_14 = __VLS_13({
        ...{ 'onPickNode': {} },
        mapId: (__VLS_ctx.mapId),
    }, ...__VLS_functionalComponentArgsRest(__VLS_13));
    let __VLS_16;
    let __VLS_17;
    let __VLS_18;
    const __VLS_19 = {
        onPickNode: (...[$event]) => {
            if (!!(__VLS_ctx.tab === 'chat'))
                return;
            if (!!(__VLS_ctx.tab === 'expand'))
                return;
            if (!!(__VLS_ctx.tab === 'summarize'))
                return;
            if (!(__VLS_ctx.tab === 'search'))
                return;
            __VLS_ctx.emit('focus-node', $event);
        }
    };
    var __VLS_15;
}
/** @type {[typeof ExpandNodeDialog, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(ExpandNodeDialog, new ExpandNodeDialog({
    ...{ 'onCancel': {} },
    ...{ 'onSuccess': {} },
    visible: (__VLS_ctx.expandVisible),
    mapId: (__VLS_ctx.mapId),
    nodeId: (__VLS_ctx.selectedNodeId),
}));
const __VLS_21 = __VLS_20({
    ...{ 'onCancel': {} },
    ...{ 'onSuccess': {} },
    visible: (__VLS_ctx.expandVisible),
    mapId: (__VLS_ctx.mapId),
    nodeId: (__VLS_ctx.selectedNodeId),
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
let __VLS_23;
let __VLS_24;
let __VLS_25;
const __VLS_26 = {
    onCancel: (...[$event]) => {
        __VLS_ctx.expandVisible = false;
    }
};
const __VLS_27 = {
    onSuccess: (__VLS_ctx.handleExpanded)
};
var __VLS_22;
/** @type {__VLS_StyleScopedClasses['ai-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['topbar']} */ ;
/** @type {__VLS_StyleScopedClasses['left']} */ ;
/** @type {__VLS_StyleScopedClasses['brand']} */ ;
/** @type {__VLS_StyleScopedClasses['right']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
/** @type {__VLS_StyleScopedClasses['tab']} */ ;
/** @type {__VLS_StyleScopedClasses['body']} */ ;
/** @type {__VLS_StyleScopedClasses['placeholder']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ModelSelector: ModelSelector,
            AiChatPanel: AiChatPanel,
            ExpandNodeDialog: ExpandNodeDialog,
            SummarizePanel: SummarizePanel,
            SemanticSearchBar: SemanticSearchBar,
            emit: emit,
            aiStore: aiStore,
            tab: tab,
            expandVisible: expandVisible,
            openExpand: openExpand,
            handleExpanded: handleExpanded,
        };
    },
    __typeEmits: {},
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
