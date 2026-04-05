import axios from 'axios'
import type { MindMapData } from '@/types/mindmap'

const api = axios.create({ baseURL: '/api' })

export const getCurrentMap = async (): Promise<MindMapData> => {
  const res = await api.get('/map/current')
  return res.data
}

export const createNode = async (mapId: string, parentId: string, text: string) => {
  await api.post('/nodes/create', { mapId, parentId, text, position: 'child' })
}

export const editNode = async (mapId: string, nodeId: string, text: string) => {
  await api.post('/nodes/edit', { mapId, nodeId, text })
}

export const deleteNode = async (mapId: string, nodeId: string) => {
  await api.post('/nodes/delete', { mapId, nodeId })
}