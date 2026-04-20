import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

export const getNodeDetail = async (nodeId: string) => {
  const res = await api.get(`/nodes/${nodeId}`)
  return res.data
}

export const searchNodes = async (query: string, mapId: string) => {
  const res = await api.post('/nodes/search', { query, mapId, caseSensitive: false })
  return res.data
}