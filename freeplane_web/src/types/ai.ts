// AI 类型定义

/**
 * AI 模型信息
 */
export interface AIModel {
  providerName: string
  providerDisplayName: string
  modelName: string
  displayName: string
  isFree: boolean
}

/**
 * 对话消息
 */
export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: number
  nodeId?: string
}

/**
 * Token 使用统计
 */
export interface TokenUsage {
  inputTokens: number
  outputTokens: number
}

/**
 * 节点扩展结果
 */
export interface ExpandNodeResult {
  success: boolean
  nodeId: string
  createdNodes: Array<{
    nodeId: string
    text: string
  }>
  summary: string
}

/**
 * 分支摘要结果
 */
export interface SummarizeResult {
  success: boolean
  nodeId: string
  summary: string
  wordCount: number
  writtenToNote: boolean
}

/**
 * 搜索结果
 */
export interface SearchResult {
  nodeId: string
  text: string
  path: string
}

/**
 * 标签结果
 */
export interface TagResult {
  nodeId: string
  tags: string[]
}

/**
 * 智能缓冲层响应
 */
export interface SmartResponse {
  success: boolean
  usedModel: string
  qualityScore: number
  bufferLayer: string
  processingTime: number
  logs: string[]
  data?: any
  errorMessage?: string
}
