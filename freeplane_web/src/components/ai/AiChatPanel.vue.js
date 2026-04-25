/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, nextTick, ref, watch } from 'vue';
import { useAIStore } from '@/stores/aiStore';
import ModelSelector from '@/components/ai/ModelSelector.vue';
import ModelConfigPanel from '@/components/ai/ModelConfigPanel.vue';
const props = defineProps();
const aiStore = useAIStore();
const input = ref('');
const scrollEl = ref(null);
const configVisible = ref(false);
let startX = 0;
let startWidth = 320;
const canSend = computed(() => input.value.trim().length > 0 && !!aiStore.currentModel);
const roleLabel = (role) => {
    if (role === 'user')
        return '我';
    if (role === 'assistant')
        return 'AI';
    return '系统';
};
const send = async () => {
    const msg = input.value.trim();
    if (!msg)
        return;
    input.value = '';
    await aiStore.sendChatStreaming(msg, {
        mapId: props.mapId,
        selectedNodeId: props.selectedNodeId
    });
    await nextTick();
    scrollToBottom();
};
const scrollToBottom = () => {
    if (!scrollEl.value)
        return;
    scrollEl.value.scrollTop = scrollEl.value.scrollHeight;
};
watch(() => aiStore.chatHistory.length, async () => {
    await nextTick();
    scrollToBottom();
});
const onKeydown = (e) => {
    if (e.key !== 'Enter')
        return;
    if (!e.ctrlKey)
        return;
    e.preventDefault();
    void send();
};
const onResizeStart = (event) => {
    startX = event.clientX;
    startWidth = aiStore.panelWidth;
    window.addEventListener('mousemove', onResizeMove);
    window.addEventListener('mouseup', onResizeEnd);
};
const onResizeMove = (event) => {
    const delta = startX - event.clientX;
    aiStore.setPanelWidth(startWidth + delta);
};
const onResizeEnd = () => {
    window.removeEventListener('mousemove', onResizeMove);
    window.removeEventListener('mouseup', onResizeEnd);
};
const retry = async () => {
    await aiStore.retryLastChat();
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['resize-handle']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['msg']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['typing']} */ ;
/** @type {__VLS_StyleScopedClasses['typing']} */ ;
/** @type {__VLS_StyleScopedClasses['typing']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['resize-handle']} */ ;
// CSS variable injection 
// CSS variable injection end 
if (__VLS_ctx.aiStore.panelVisible && __VLS_ctx.aiStore.aiMode === 'chat') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
        ...{ class: "chat-panel" },
        ...{ style: ({ width: `${__VLS_ctx.aiStore.panelWidth}px` }) },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ onMousedown: (__VLS_ctx.onResizeStart) },
        ...{ class: "resize-handle" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "chat-header" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "title" },
    });
    /** @type {[typeof ModelSelector, ]} */ ;
    // @ts-ignore
    const __VLS_0 = __VLS_asFunctionalComponent(ModelSelector, new ModelSelector({
        ...{ 'onOpenConfig': {} },
        compact: true,
    }));
    const __VLS_1 = __VLS_0({
        ...{ 'onOpenConfig': {} },
        compact: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_0));
    let __VLS_3;
    let __VLS_4;
    let __VLS_5;
    const __VLS_6 = {
        onOpenConfig: (...[$event]) => {
            if (!(__VLS_ctx.aiStore.panelVisible && __VLS_ctx.aiStore.aiMode === 'chat'))
                return;
            __VLS_ctx.configVisible = true;
        }
    };
    var __VLS_2;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.aiStore.clearChat) },
        ...{ class: "ghost" },
        disabled: (__VLS_ctx.aiStore.streaming),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ref: "scrollEl",
        ...{ class: "chat-history" },
    });
    /** @type {typeof __VLS_ctx.scrollEl} */ ;
    if (__VLS_ctx.aiStore.chatHistory.length === 0) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "empty" },
        });
    }
    for (const [m, idx] of __VLS_getVForSourceType((__VLS_ctx.aiStore.chatHistory))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (idx),
            ...{ class: "msg" },
            ...{ class: (m.role) },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "meta" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
            ...{ class: "role" },
        });
        (__VLS_ctx.roleLabel(m.role));
        if (m.nodeId) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "node" },
            });
            (m.nodeId);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({
            ...{ class: "content" },
        });
        (m.content);
        if (__VLS_ctx.aiStore.streaming && m.role === 'assistant' && !m.content) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "typing" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        }
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "current-node" },
    });
    (__VLS_ctx.selectedNodeId || '-');
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "composer" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
        ...{ onKeydown: (__VLS_ctx.onKeydown) },
        value: (__VLS_ctx.input),
        ...{ class: "input" },
        placeholder: "输入消息（Ctrl+Enter 发送）",
        disabled: (__VLS_ctx.aiStore.streaming),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.send) },
        ...{ class: "primary" },
        disabled: (__VLS_ctx.aiStore.streaming || !__VLS_ctx.canSend),
    });
    (__VLS_ctx.aiStore.streaming ? '输出中...' : '发送');
    if (__VLS_ctx.aiStore.lastChatFailed) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "retry-row" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.retry) },
            ...{ class: "retry-btn" },
            disabled: (__VLS_ctx.aiStore.streaming),
        });
    }
    /** @type {[typeof ModelConfigPanel, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(ModelConfigPanel, new ModelConfigPanel({
        ...{ 'onClose': {} },
        visible: (__VLS_ctx.configVisible),
    }));
    const __VLS_8 = __VLS_7({
        ...{ 'onClose': {} },
        visible: (__VLS_ctx.configVisible),
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
    let __VLS_10;
    let __VLS_11;
    let __VLS_12;
    const __VLS_13 = {
        onClose: (...[$event]) => {
            if (!(__VLS_ctx.aiStore.panelVisible && __VLS_ctx.aiStore.aiMode === 'chat'))
                return;
            __VLS_ctx.configVisible = false;
        }
    };
    var __VLS_9;
}
/** @type {__VLS_StyleScopedClasses['chat-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['resize-handle']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-header']} */ ;
/** @type {__VLS_StyleScopedClasses['title']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['chat-history']} */ ;
/** @type {__VLS_StyleScopedClasses['empty']} */ ;
/** @type {__VLS_StyleScopedClasses['msg']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['role']} */ ;
/** @type {__VLS_StyleScopedClasses['node']} */ ;
/** @type {__VLS_StyleScopedClasses['content']} */ ;
/** @type {__VLS_StyleScopedClasses['typing']} */ ;
/** @type {__VLS_StyleScopedClasses['current-node']} */ ;
/** @type {__VLS_StyleScopedClasses['composer']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['primary']} */ ;
/** @type {__VLS_StyleScopedClasses['retry-row']} */ ;
/** @type {__VLS_StyleScopedClasses['retry-btn']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ModelSelector: ModelSelector,
            ModelConfigPanel: ModelConfigPanel,
            aiStore: aiStore,
            input: input,
            scrollEl: scrollEl,
            configVisible: configVisible,
            canSend: canSend,
            roleLabel: roleLabel,
            send: send,
            onKeydown: onKeydown,
            onResizeStart: onResizeStart,
            retry: retry,
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
