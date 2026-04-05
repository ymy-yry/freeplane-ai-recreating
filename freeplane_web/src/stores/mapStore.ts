import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MindMapData } from '@/types/mindmap'
import * as mapApi from '@/api/mapApi'

export const useMapStore = defineStore('map', () => {
  const currentMap = ref<MindMapData | null>(null)
  const loading = ref(false)
  let pollingInterval: NodeJS.Timeout | null = null

  const loadMap = async () => {
    loading.value = true
    try {
      currentMap.value = await mapApi.getCurrentMap()
    } catch (e) {
      console.error('加载导图失败', e)
    } finally {
      loading.value = false
    }
  }

  const startPolling = () => {
    if (pollingInterval) clearInterval(pollingInterval)
    pollingInterval = setInterval(loadMap, 2000)
  }

  const stopPolling = () => {
    if (pollingInterval) clearInterval(pollingInterval)
  }

  const createNode = async (parentId: string, text: string) => {
    if (!currentMap.value) return
    await mapApi.createNode(currentMap.value.mapId, parentId, text)
    await loadMap()
  }

  const editNode = async (nodeId: string, text: string) => {
    if (!currentMap.value) return
    await mapApi.editNode(currentMap.value.mapId, nodeId, text)
    await loadMap()
  }

  const deleteNode = async (nodeId: string) => {
    if (!currentMap.value) return
    await mapApi.deleteNode(currentMap.value.mapId, nodeId)
    await loadMap()
  }

  return { currentMap, loading, loadMap, startPolling, stopPolling, createNode, editNode, deleteNode }
})