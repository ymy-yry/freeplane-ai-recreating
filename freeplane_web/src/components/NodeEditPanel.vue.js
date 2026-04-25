/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref, onMounted, nextTick } from 'vue';
const props = defineProps();
const emit = defineEmits();
const editText = ref(props.initialText);
const textareaRef = ref(null);
const handleSave = () => {
    const trimmed = editText.value.trim();
    if (trimmed) {
        emit('save', props.nodeId, trimmed);
    }
    else {
        emit('cancel');
    }
};
const handleCancel = () => {
    emit('cancel');
};
onMounted(() => {
    if (props.visible) {
        nextTick(() => {
            textareaRef.value?.focus();
            textareaRef.value?.select();
        });
    }
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
if (__VLS_ctx.visible) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "edit-panel" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "panel-content" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea, __VLS_intrinsicElements.textarea)({
        ...{ onKeydown: (__VLS_ctx.handleSave) },
        ...{ onBlur: (__VLS_ctx.handleSave) },
        ref: "textareaRef",
        value: (__VLS_ctx.editText),
        rows: "3",
        placeholder: "输入节点文本...",
    });
    /** @type {typeof __VLS_ctx.textareaRef} */ ;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.handleCancel) },
        ...{ class: "cancel-btn" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.handleSave) },
        ...{ class: "save-btn" },
    });
}
/** @type {__VLS_StyleScopedClasses['edit-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['panel-content']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['cancel-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['save-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            editText: editText,
            textareaRef: textareaRef,
            handleSave: handleSave,
            handleCancel: handleCancel,
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
