/**
 * AI 相关接口封装
 * 调用 freeplane_plugin_ai 插件的 REST API（端口 6299）
 */

import axios from 'axios'
import type {
  AIModel,
  ChatMessage,
  TokenUsage,
  ExpandNodeResult,
  SummarizeResult,
  SearchResult,
  TagResult,
  SmartResponse
} from '@/types/ai'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ==================== AI 模型管理 ====================

/**
 * 获取可用的 AI 模型列表
 */
export function getAiModels() {
  return api.get<{ models: AIModel[] }>('/ai/chat/models')
}

// ==================== AI 对话 ====================

/**
 * AI 对话（同步响应）
 */
export function aiChat(data: {
  message: string
  modelSelection?: string
  mapId?: string
  selectedNodeId?: string
  serviceType?: string // 'chat' 或 'agent'
}) {
  return api.post<{
    reply: string
    tokenUsage: TokenUsage
  }>('/ai/chat/message', data)
}

/**
 * AI 对话（流式输出）- 待后端实现
 */
export function aiChatStream(data: {
  message: string
  modelSelection?: string
  nodeId?: string
  onChunk: (chunk: string) => void
}) {
  // TODO: 实现 Server-Sent Events (SSE)
  throw new Error('流式对话暂未实现')
}

// ==================== 节点扩展 ====================

/**
 * AI 扩展节点：基于目标节点生成子节点
 */
export function expandNode(data: {
  nodeId: string
  mapId?: string
  count?: number
  depth?: number
  focus?: string
  serviceType?: string // 默认为 'agent'
}) {
  return api.post<ExpandNodeResult>('/ai/build/expand-node', data)
}

// ==================== 分支摘要 ====================

/**
 * AI 生成分支摘要
 */
export function summarizeBranch(data: {
  nodeId: string
  mapId?: string
  maxWords?: number
  writeToNote?: boolean
  serviceType?: string // 默认为 'agent'
}) {
  return api.post<SummarizeResult>('/ai/build/summarize', data)
}

// ==================== 节点搜索 ====================

/**
 * 关键词搜索节点
 */
export function searchNodes(data: {
  query: string
  caseSensitive?: boolean
  mapId?: string
}) {
  return api.post<{
    results: SearchResult[]
    totalCount: number
  }>('/nodes/search', data)
}

// ==================== 自动标签 ====================

/**
 * AI 提取节点关键词标签
 */
export function autoTag(data: {
  nodeIds: string[]
  mapId?: string
  serviceType?: string // 默认为 'agent'
}) {
  return api.post<{
    results: TagResult[]
    message: string
  }>('/ai/build/tag', data)
}

// ==================== 智能缓冲层 ====================

/**
 * 智能请求：自然语言理解，自动选择最佳工具
 */
export function smartRequest(data: {
  input: string
  serviceType?: string // 'chat' 或 'agent'
}) {
  return api.post<SmartResponse>('/ai/chat/smart', data)
}

// ==================== 思维导图生成 ====================

/**
 * AI 一键生成思维导图
 */
export function generateMindMap(data: {
  topic: string
  modelSelection?: string
  maxDepth?: number
  serviceType?: string // 默认为 'agent'
}) {
  return api.post<{
    success: boolean
    topic: string
    nodeCount: number
    mapId: string
  }>('/ai/build/generate-mindmap', data)
}

export default api
