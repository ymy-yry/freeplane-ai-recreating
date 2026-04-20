<template>
  <div class="ai-chat-panel">
    <ModelSelector />

    <div class="chat-box">
      <div v-for="(msg, idx) in aiStore.messages" :key="idx" class="msg-item">
        <div class="msg-role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
      </div>

      <div v-if="aiStore.isGenerating" class="msg-item streaming">
        <div class="msg-role">AI</div>
        <div class="msg-content" v-html="renderMarkdown(aiStore.streamText)"></div>
      </div>
    </div>

    <div class="input-bar">
      <textarea
        v-model="inputText"
        placeholder="输入问题，按 Enter 发送，Shift+Enter 换行"
        @keydown.enter.exact="sendMessage"
        class="input-area"
      ></textarea>
      <button @click="sendMessage" class="send-btn" :disabled="!aiStore.hasModel || !inputText">
        发送
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { marked } from 'marked'
import ModelSelector from './ModelSelector.vue'
import { useAiStore } from '../../stores/aiStore'
import { aiChatStream } from '../../api/aiApi'

const aiStore = useAiStore()
const inputText = ref('')

// 流式发送消息
const sendMessage = async () => {
  if (!inputText.value || !aiStore.currentModel) return

  // 1. 添加用户消息
  aiStore.addMessage({ role: 'user', content: inputText.value })
  // 2. 重置流式状态
  aiStore.isGenerating = true
  aiStore.streamText = ''

  try {
    // 3. 调用流式接口
    const response = await aiChatStream({
      model: aiStore.currentModel,
      message: inputText.value,
      nodeId: aiStore.selectedNode?.id
    })

    // 4. 逐字读取流式数据
    const reader = response.data.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      aiStore.streamText += decoder.decode(value)
    }

    // 5. 流式结束，添加AI消息
    aiStore.addMessage({ role: 'assistant', content: aiStore.streamText })
  } catch (err) {
    console.error('对话失败:', err)
  } finally {
    // 6. 重置状态
    aiStore.isGenerating = false
    inputText.value = ''
  }
}

// Markdown 渲染
const renderMarkdown = (text: string) => {
  return marked.parse(text)
}
</script>

<style scoped>
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100vh;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}
.chat-box {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  background: #f9f9f9;
}
.msg-item {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
.msg-role {
  font-weight: bold;
  color: #666;
  min-width: 40px;
}
.msg-content {
  flex: 1;
  background: #fff;
  padding: 8px 12px;
  border-radius: 8px;
  line-height: 1.6;
}
.streaming .msg-content {
  border-left: 3px solid #409eff;
}
.input-bar {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #eee;
  background: #fff;
}
.input-area {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  resize: none;
  min-height: 40px;
  font-size: 14px;
}
.send-btn {
  padding: 0 20px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.send-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}
</style>