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
  onChunk: (chunk: string) => void
}) {
  // 兼容预留：若后端实现 SSE，可在这里启用真正的流式输出。
  // 当前项目的前端“逐字显示”由 store 的模拟打字效果实现，不依赖后端流式。
  void data
  throw new Error('后端未提供流式接口：请使用非流式 + 前端逐字显示')
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
}) {
  return api.post<{
    success: boolean
    topic: string
    nodeCount: number
    mapId: string
  }>('/ai/build/generate-mindmap', data)
}

export default api
