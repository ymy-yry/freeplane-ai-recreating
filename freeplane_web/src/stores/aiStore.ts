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
  
  /** 当前选中的模型 */
  const currentModel = ref<string>('')
  
  /** 对话历史记录 */
  const chatHistory = ref<ChatMessage[]>([])
  
  /** 是否正在流式输出 */
  const streaming = ref(false)
  
  /** 是否正在加载 */
  const loading = ref(false)
  
  /** AI 面板是否可见 */
  const panelVisible = ref(false)

  // ==================== 计算属性 ====================
  
  /** 是否有已配置的模型 */
  const hasConfiguredModels = computed(() => modelList.value.length > 0)
  
  /** 当前选中的模型对象 */
  const currentModelObj = computed(() => 
    modelList.value.find(m => m.modelName === currentModel.value)
  )

  // ==================== 方法 ====================
  
  /**
   * 获取模型列表
   */
  const fetchModelList = async () => {
    try {
      loading.value = true
      const response = await aiApi.getAiModels()
      modelList.value = response.data.models
      
      // 如果没有选中模型，自动选择第一个
      if (modelList.value.length > 0 && !currentModel.value) {
        currentModel.value = modelList.value[0].modelName
      }
    } catch (error: any) {
      console.error('获取模型列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
  
  /**
   * 切换模型
   */
  const switchModel = (modelName: string) => {
    currentModel.value = modelName
    // 可选：清空对话历史
    // chatHistory.value = []
  }
  
  /**
   * 发送 AI 对话
   */
  const sendChat = async (message: string, nodeId?: string) => {
    if (!message.trim()) return
    
    // 添加用户消息
    chatHistory.value.push({
      role: 'user',
      content: message,
      timestamp: Date.now(),
      nodeId
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
        selectedNodeId: nodeId
      })
      
      // 更新助手回复
      chatHistory.value[assistantIndex].content = response.data.reply
    } catch (error: any) {
      chatHistory.value[assistantIndex].content = `❌ 对话失败：${error.message}`
      console.error('AI 对话失败:', error)
    } finally {
      streaming.value = false
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
    
    // 计算属性
    hasConfiguredModels,
    currentModelObj,
    
    // 方法
    fetchModelList,
    switchModel,
    sendChat,
    clearChat,
    togglePanel,
    showPanel,
    hidePanel,
    init
  }
})
