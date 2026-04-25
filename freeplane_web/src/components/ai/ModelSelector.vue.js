/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, watch } from 'vue';
import { useAIStore } from '@/stores/aiStore';
const aiStore = useAIStore();
const props = withDefaults(defineProps(), {
    compact: false
});
const compact = props.compact;
const emit = defineEmits();
const selectedModel = computed({
    get: () => aiStore.currentModel,
    set: (value) => aiStore.switchModel(value)
});
const handleModelChange = () => {
    console.log('切换模型:', selectedModel.value);
    // 可选：切换模型时清空对话
    // aiStore.clearChat()
};
// 监听模型列表加载
watch(() => aiStore.modelList.length, (newLength) => {
    if (newLength > 0 && !aiStore.currentModel) {
        const m = aiStore.modelList[0];
        aiStore.currentModel = m.providerName + '|' + m.modelName;
    }
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({
    compact: false
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['model-selector']} */ ;
/** @type {__VLS_StyleScopedClasses['model-select']} */ ;
/** @type {__VLS_StyleScopedClasses['model-select']} */ ;
/** @type {__VLS_StyleScopedClasses['model-select']} */ ;
/** @type {__VLS_StyleScopedClasses['model-select']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "model-selector" },
    ...{ class: ({ compact: __VLS_ctx.compact }) },
});
if (!__VLS_ctx.compact) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "selector-label" },
    });
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    ...{ onChange: (__VLS_ctx.handleModelChange) },
    value: (__VLS_ctx.selectedModel),
    disabled: (__VLS_ctx.aiStore.loading),
    ...{ class: "model-select" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "",
    disabled: true,
});
(__VLS_ctx.aiStore.loading ? '加载中...' : '请选择模型');
for (const [model] of __VLS_getVForSourceType((__VLS_ctx.aiStore.modelList))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        key: (model.providerName + '|' + model.modelName),
        value: (model.providerName + '|' + model.modelName),
    });
    (model.displayName);
    (model.isFree ? '(免费)' : '');
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (...[$event]) => {
            __VLS_ctx.emit('open-config');
        } },
    ...{ class: "config-btn" },
    type: "button",
});
if (!__VLS_ctx.aiStore.hasConfiguredModels) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "warning" },
    });
}
/** @type {__VLS_StyleScopedClasses['model-selector']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-label']} */ ;
/** @type {__VLS_StyleScopedClasses['model-select']} */ ;
/** @type {__VLS_StyleScopedClasses['config-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['warning']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            aiStore: aiStore,
            compact: compact,
            emit: emit,
            selectedModel: selectedModel,
            handleModelChange: handleModelChange,
        };
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
; /* PartiallyEnd: #4569/main.vue */
