<template>
  <div class="chat-panel">
    <div class="chat-header">
      <div class="title">AI 对话</div>
      <div class="header-right">
        <span v-if="modelLabel" class="model-pill" :title="modelLabel">{{ modelLabel }}</span>
        <button class="ghost" @click="aiStore.clearChat" :disabled="aiStore.streaming">清空</button>
      </div>
    </div>

    <div ref="scrollEl" class="chat-history">
      <div v-if="aiStore.chatHistory.length === 0" class="empty">
        请选择模型，然后开始对话。支持 Markdown 文本输出（当前以纯文本方式显示）。
      </div>

      <div v-for="(m, idx) in aiStore.chatHistory" :key="idx" class="msg" :class="m.role">
        <div class="meta">
          <span class="role">{{ roleLabel(m.role) }}</span>
          <span v-if="m.nodeId" class="node">节点：{{ m.nodeId }}</span>
        </div>
        <pre class="content">{{ m.content }}</pre>
      </div>
    </div>

    <div class="composer">
      <textarea
        v-model="input"
        class="input"
        placeholder="输入消息（Shift+Enter 换行，Enter 发送）"
        :disabled="aiStore.streaming"
        @keydown="onKeydown"
      />
      <button class="primary" @click="send" :disabled="aiStore.streaming || !canSend">
        {{ aiStore.streaming ? '输出中...' : '发送' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useAIStore } from '@/stores/aiStore'
import type { ChatMessage } from '@/types/ai'

const props = defineProps<{
  mapId?: string
  selectedNodeId?: string
}>()

const aiStore = useAIStore()
const input = ref('')
const scrollEl = ref<HTMLElement | null>(null)

const modelLabel = computed(() => aiStore.currentModelObj?.displayName || aiStore.currentModel)
const canSend = computed(() => input.value.trim().length > 0 && !!aiStore.currentModel)

const roleLabel = (role: ChatMessage['role']) => {
  if (role === 'user') return '我'
  if (role === 'assistant') return 'AI'
  return '系统'
}

const send = async () => {
  const msg = input.value.trim()
  if (!msg) return
  input.value = ''

  await aiStore.sendChatStreaming(msg, {
    mapId: props.mapId,
    selectedNodeId: props.selectedNodeId
  })

  await nextTick()
  scrollToBottom()
}

const scrollToBottom = () => {
  if (!scrollEl.value) return
  scrollEl.value.scrollTop = scrollEl.value.scrollHeight
}

watch(
  () => aiStore.chatHistory.length,
  async () => {
    await nextTick()
    scrollToBottom()
  }
)

const onKeydown = (e: KeyboardEvent) => {
  if (e.key !== 'Enter') return
  if (e.shiftKey) return
  e.preventDefault()
  void send()
}
</script>

<style scoped>
.chat-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #e7e7e7;
  background: #fff;
}

.title {
  font-weight: 700;
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-pill {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  padding: 4px 8px;
  border: 1px solid #d7ebff;
  background: #f3f9ff;
  border-radius: 999px;
  color: #0b61c2;
}

.ghost {
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}

.ghost:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.chat-history {
  flex: 1;
  overflow: auto;
  padding: 12px;
  background: #fafafa;
}

.empty {
  color: #666;
  font-size: 13px;
  padding: 8px 0;
}

.msg {
  padding: 10px 10px;
  border-radius: 10px;
  border: 1px solid #e7e7e7;
  background: #fff;
  margin-bottom: 10px;
}

.msg.user {
  border-color: #d7ebff;
  background: #f3f9ff;
}

.meta {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 6px;
  color: #666;
  font-size: 12px;
}

.role {
  font-weight: 700;
  color: #333;
}

.node {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 11px;
}

.content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.5;
  color: #111;
}

.composer {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-top: 1px solid #e7e7e7;
  background: #fff;
}

.input {
  flex: 1;
  height: 64px;
  resize: none;
  border-radius: 10px;
  border: 1px solid #ddd;
  padding: 10px;
  font-size: 13px;
  outline: none;
}

.input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.12);
}

.primary {
  width: 90px;
  border-radius: 10px;
  border: 1px solid #1890ff;
  background: #1890ff;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

