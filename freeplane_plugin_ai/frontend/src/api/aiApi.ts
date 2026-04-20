// frontend/src/api/aiApi.ts
import axios from 'axios'

// 创建 axios 实例（适配后端接口）
const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 1. 获取模型列表（动态加载通义千问、文心一言等）
export function getAiModels() {
  return api.get('/ai/chat/models')
}

// 2. AI 流式对话
export function aiChatStream(data: {
  model: string
  message: string
  nodeId?: string
}) {
  return api.post('/ai/chat/stream', data, {
    responseType: 'stream'
  })
}

// 3. 节点展开
export function expandNode(data: {
  nodeId: string
  model: string
}) {
  return api.post('/ai/build/expand-node', data)
}

// 4. 分支摘要
export function summarizeBranch(data: {
  nodeId: string
  model: string
}) {
  return api.post('/ai/build/summarize', data)
}

// 5. 语义搜索
export function semanticSearch(data: {
  keyword: string
  model: string
}) {
  return api.post('/ai/build/search', data)
}

export default api