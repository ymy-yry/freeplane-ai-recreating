// frontend/src/stores/aiStore.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 定义消息类型（TypeScript 类型安全）
export interface AiMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
}

// 定义模型类型（支持通义千问、文心一言等）
export interface AiModel {
  name: string
  label: string
  apiKey?: string
}

// 定义思维导图节点类型（和画布对齐）
export interface MindNode {
  id: string
  text: string
  children?: MindNode[]
}

export const useAiStore = defineStore('ai', () => {
  // 模型相关状态
  const modelList = ref<AiModel[]>([])
  const currentModel = ref<string>('')

  // AI 对话状态
  const messages = ref<AiMessage[]>([])
  const isGenerating = ref<boolean>(false)
  const streamText = ref<string>('')

  // 节点操作状态
  const selectedNode = ref<MindNode | null>(null)
  const expandResult = ref<any>(null)
  const summarizeResult = ref<string>('')

  // 计算属性
  const hasModel = computed(() => currentModel.value !== '')

  // 方法
  const setModelList = (list: AiModel[]) => {
    modelList.value = list
  }

  const setCurrentModel = (model: string) => {
    currentModel.value = model
  }

  const addMessage = (msg: AiMessage) => {
    messages.value.push(msg)
  }

  const reset = () => {
    messages.value = []
    streamText.value = ''
    expandResult.value = null
    summarizeResult.value = ''
    isGenerating.value = false
  }

  // 设置选中节点（画布联动核心）
  const setSelectedNode = (node: MindNode | null) => {
    selectedNode.value = node
  }

  return {
    modelList,
    currentModel,
    messages,
    isGenerating,
    streamText,
    selectedNode,
    expandResult,
    summarizeResult,
    hasModel,
    setModelList,
    setCurrentModel,
    addMessage,
    reset,
    setSelectedNode
  }
})