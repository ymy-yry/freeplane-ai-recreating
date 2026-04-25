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

export const toggleFold = async (mapId: string, nodeId: string, folded: boolean) => {
  await api.post('/nodes/toggle-fold', { mapId, nodeId, folded })
}

export interface MapInfo {
  mapId: string
  title: string
  isCurrent: boolean
}

export interface GetAllMapsResponse {
  maps: MapInfo[]
  count: number
}

export const getAllMaps = async (): Promise<GetAllMapsResponse> => {
  const res = await api.get('/maps')
  return res.data
}

export interface CreateMapRequest {
  title?: string
}

export interface CreateMapResponse {
  message: string
  mapId: string
  title: string
  success: boolean
}

export const createNewMap = async (request?: CreateMapRequest): Promise<CreateMapResponse> => {
  const res = await api.post('/maps/create', request || {})
  return res.data
}

export interface SwitchMapRequest {
  mapId: string
}

export interface SwitchMapResponse {
  success: boolean
  message?: string
}

export const switchMap = async (request: SwitchMapRequest): Promise<SwitchMapResponse> => {
  const res = await api.post('/maps/switch', request)
  return res.data
}

export interface ImportMapRequest {
  content: string
  filename?: string
}

export interface ImportMapResponse {
  success: boolean
  mapId: string
  title: string
  filename: string
}

export const importMap = async (request: ImportMapRequest): Promise<ImportMapResponse> => {
  const res = await api.post('/maps/import', request)
  return res.data
}