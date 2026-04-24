/**
 * AI 状态管理
 * 管理 AI 相关的全局状态和操作
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AIModel, ChatMessage } from '@/types/ai'
import * as aiApi from '@/api/aiApi'

export const useAIStore = defineStore('ai', () => {
  // ==================== 状态 ====================
  
  /** 可用的 AI 模型列表 */
  const modelList = ref<AIModel[]>([])
  
  /** 当前选中的模型（格式：providerName:modelName） */
  const currentModel = ref<string>('')
  
  /** 对话历史记录 */
  const chatHistory = ref<ChatMessage[]>([])
  
  /** 是否正在流式输出 */
  const streaming = ref(false)
  
  /** 是否正在加载 */
  const loading = ref(false)
  
  /** AI 面板是否可见 */
  const panelVisible = ref(false)
  
  /** 最近一次错误信息（用于 UI 反馈） */
  const lastError = ref<string>('')

  // ==================== 计算属性 ====================
  
  /** 是否有已配置的模型 */
  const hasConfiguredModels = computed(() => modelList.value.length > 0)
  
  /** 当前选中的模型对象 */
  const currentModelObj = computed(() => {
    const [providerName, modelName] = splitModelSelection(currentModel.value)
    return modelList.value.find(m => m.providerName === providerName && m.modelName === modelName)
  })

  // ==================== 方法 ====================
  
  const splitModelSelection = (selection: string): [string, string] => {
    const idx = selection.indexOf(':')
    if (idx <= 0 || idx === selection.length - 1) return ['', selection]
    return [selection.slice(0, idx), selection.slice(idx + 1)]
  }
  
  const toModelSelection = (model: AIModel) => `${model.providerName}:${model.modelName}`
  
  /**
   * 获取模型列表
   */
  const fetchModelList = async () => {
    try {
      loading.value = true
      lastError.value = ''
      const response = await aiApi.getAiModels()
      modelList.value = response.data.models
      
      // 如果没有选中模型，自动选择第一个
      if (modelList.value.length > 0 && !currentModel.value) {
        currentModel.value = toModelSelection(modelList.value[0])
      }
    } catch (error: any) {
      console.error('获取模型列表失败:', error)
      lastError.value = error?.message || '获取模型列表失败'
      throw error
    } finally {
      loading.value = false
    }
  }
  
  /**
   * 切换模型
   */
  const switchModel = (modelSelection: string) => {
    currentModel.value = modelSelection
    // 可选：清空对话历史
    // chatHistory.value = []
  }
  
  /**
   * 发送 AI 对话
   */
  const sendChat = async (message: string, ctx?: { mapId?: string; selectedNodeId?: string }) => {
    if (!message.trim()) return
    
    // 添加用户消息
    chatHistory.value.push({
      role: 'user',
      content: message,
      timestamp: Date.now(),
      nodeId: ctx?.selectedNodeId
    })
    
    // 添加助手占位消息
    const assistantIndex = chatHistory.value.push({
      role: 'assistant',
      content: '',
      timestamp: Date.now()
    }) - 1
    
    streaming.value = true
    try {
      const response = await aiApi.aiChat({
        message,
        modelSelection: currentModel.value,
        mapId: ctx?.mapId,
        selectedNodeId: ctx?.selectedNodeId
      })
      
      // 更新助手回复
      chatHistory.value[assistantIndex].content = response.data.reply
    } catch (error: any) {
      const msg = error?.message || '对话失败'
      lastError.value = msg
      chatHistory.value[assistantIndex].content = `❌ 对话失败：${msg}`
      console.error('AI 对话失败:', error)
    } finally {
      streaming.value = false
    }
  }
  
  /**
   * 发送 AI 对话（逐字显示）
   * 当前实现：先请求完整回复，再模拟逐字输出（后端 SSE 上线后可替换为真正流式）。
   */
  const sendChatStreaming = async (message: string, ctx?: { mapId?: string; selectedNodeId?: string }) => {
    if (!message.trim()) return
    
    chatHistory.value.push({
      role: 'user',
      content: message,
      timestamp: Date.now(),
      nodeId: ctx?.selectedNodeId
    })
    
    const assistantIndex = chatHistory.value.push({
      role: 'assistant',
      content: '',
      timestamp: Date.now()
    }) - 1
    
    streaming.value = true
    try {
      const response = await aiApi.aiChat({
        message,
        modelSelection: currentModel.value,
        mapId: ctx?.mapId,
        selectedNodeId: ctx?.selectedNodeId
      })
      
      const text = response.data.reply || ''
      await typeOut(chatHistory.value[assistantIndex], text, 10)
    } catch (error: any) {
      const msg = error?.message || '对话失败'
      lastError.value = msg
      chatHistory.value[assistantIndex].content = `❌ 对话失败：${msg}`
      console.error('AI 对话失败:', error)
    } finally {
      streaming.value = false
    }
  }
  
  const typeOut = (message: ChatMessage, fullText: string, delayMs: number) => {
    message.content = ''
    return new Promise<void>((resolve) => {
      let i = 0
      const timer = window.setInterval(() => {
        i += 1
        message.content = fullText.slice(0, i)
        if (i >= fullText.length) {
          window.clearInterval(timer)
          resolve()
        }
      }, Math.max(0, delayMs))
    })
  }
  
  const expandNode = async (data: { mapId?: string; nodeId: string; depth?: number; count?: number; focus?: string }) => {
    try {
      loading.value = true
      lastError.value = ''
      const res = await aiApi.expandNode(data)
      return res.data
    } catch (error: any) {
      lastError.value = error?.message || '展开节点失败'
      throw error
    } finally {
      loading.value = false
    }
  }
  
  const summarize = async (data: { mapId?: string; nodeId: string; maxWords?: number; writeToNote?: boolean }) => {
    try {
      loading.value = true
      lastError.value = ''
      const res = await aiApi.summarizeBranch(data)
      return res.data
    } catch (error: any) {
      lastError.value = error?.message || '分支摘要失败'
      throw error
    } finally {
      loading.value = false
    }
  }
  
  const tagNodes = async (data: { mapId?: string; nodeIds: string[] }) => {
    try {
      loading.value = true
      lastError.value = ''
      const res = await aiApi.autoTag(data)
      return res.data
    } catch (error: any) {
      lastError.value = error?.message || '自动标签失败'
      throw error
    } finally {
      loading.value = false
    }
  }
  
  const keywordSearch = async (data: { mapId?: string; query: string; caseSensitive?: boolean }) => {
    try {
      loading.value = true
      lastError.value = ''
      const res = await aiApi.searchNodes(data)
      return res.data
    } catch (error: any) {
      lastError.value = error?.message || '搜索失败'
      throw error
    } finally {
      loading.value = false
    }
  }
  
  /**
   * 清空对话历史
   */
  const clearChat = () => {
    chatHistory.value = []
  }
  
  /**
   * 切换 AI 面板显示状态
   */
  const togglePanel = () => {
    panelVisible.value = !panelVisible.value
  }
  
  /**
   * 显示 AI 面板
   */
  const showPanel = () => {
    panelVisible.value = true
  }
  
  /**
   * 隐藏 AI 面板
   */
  const hidePanel = () => {
    panelVisible.value = false
  }
  
  /**
   * 初始化 AI Store
   */
  const init = async () => {
    try {
      await fetchModelList()
    } catch (error) {
      console.warn('AI Store 初始化失败:', error)
    }
  }

  // ==================== 返回 ====================
  
  return {
    // 状态
    modelList,
    currentModel,
    chatHistory,
    streaming,
    loading,
    panelVisible,
    lastError,
    
    // 计算属性
    hasConfiguredModels,
    currentModelObj,
    
    // 方法
    fetchModelList,
    switchModel,
    sendChat,
    sendChatStreaming,
    clearChat,
    togglePanel,
    showPanel,
    hidePanel,
    init,
    expandNode,
    summarize,
    tagNodes,
    keywordSearch
  }
})
