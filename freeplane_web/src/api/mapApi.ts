import axios from 'axios'
import type { MindMapData } from '@/types/mindmap'

const api = axios.create({ baseURL: '/api' })

export const getCurrentMap = async (): Promise<MindMapData> => {
  const res = await api.get('/map/current')
  return res.data
}

export const createNode = async (mapId: string, parentId: string, text: string) => {
  // 根据后端支持，mapId是可选的，不传则使用当前激活的导图
  const payload = { parentId, text, position: 'child' }
  if (mapId) {
    (payload as any).mapId = mapId
  }
  await api.post('/nodes/create', payload)
}

export const editNode = async (mapId: string, nodeId: string, text: string) => {
  const payload = { nodeId, text }
  if (mapId) {
    (payload as any).mapId = mapId
  }
  await api.post('/nodes/edit', payload)
}

export const deleteNode = async (mapId: string, nodeId: string) => {
  const payload = { nodeId }
  if (mapId) {
    (payload as any).mapId = mapId
  }
  await api.post('/nodes/delete', payload)
}

export const toggleFold = async (mapId: string, nodeId: string, folded: boolean) => {
  const payload = { nodeId, folded }
  if (mapId) {
    (payload as any).mapId = mapId
  }
  await api.post('/nodes/toggle-fold', payload)
}

// 多导图API接口
export const createNewMap = async (title: string): Promise<MindMapData> => {
  // 后端可能通过创建新文件或新MapModel来实现
  const res = await api.post('/map/create', { title })
  return res.data
}

export const switchMap = async (mapId: string): Promise<MindMapData> => {
  // 切换当前激活的导图
  const res = await api.post('/map/switch', { mapId })
  return res.data
}

export const getAllMaps = async (): Promise<{ mapId: string; title: string }[]> => {
  // 获取所有已打开的导图列表
  const res = await api.get('/map/all')
  return res.data
}

// 新增：获取导图详细信息
export const getMapDetails = async (mapId: string): Promise<MindMapData> => {
  const res = await api.get(`/map/${mapId}`)
  return res.data
}