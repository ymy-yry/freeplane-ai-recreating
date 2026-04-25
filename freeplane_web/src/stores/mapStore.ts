import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MindMapData } from '@/types/mindmap'
import * as mapApi from '@/api/mapApi'

export const useMapStore = defineStore('map', () => {
  const currentMap = ref<MindMapData | null>(null)
  const loading = ref(false)
  const allMaps = ref<mapApi.MapInfo[]>([])
  const mapsLoading = ref(false)
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
    pollingInterval = setInterval(loadMap, 3000)
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

  const toggleNodeFold = async (nodeId: string) => {
    if (!currentMap.value) return

    const findNode = (node: any): any => {
      if (node.id === nodeId) return node
      if (!node.children?.length) return null
      for (const child of node.children) {
        const found = findNode(child)
        if (found) return found
      }
      return null
    }
    const target = findNode(currentMap.value.root)
    if (!target) return

    const newFolded = !target.folded

    try {
      await mapApi.toggleFold(currentMap.value.mapId, nodeId, newFolded)
      await loadMap()        // 关键：立即从后端重新加载，确保状态一致
    } catch (e) {
      console.error('折叠操作失败', e)
    }
  }

  const setCurrentMap = (mapData: MindMapData) => {
    currentMap.value = mapData
  }

  const loadAllMaps = async () => {
    mapsLoading.value = true
    try {
      const response = await mapApi.getAllMaps()
      allMaps.value = response.maps
    } catch (e) {
      console.error('加载导图列表失败', e)
    } finally {
      mapsLoading.value = false
    }
  }

  const createNewMap = async (title?: string) => {
    const response = await mapApi.createNewMap(title ? { title } : undefined)
    if (!response.success) throw new Error(response.message || '创建导图失败')
    if (response.mapId) {
      try {
        await mapApi.switchMap({ mapId: response.mapId })
      } catch (e) {
        console.warn('新建后切换导图失败，将继续加载当前导图', e)
      }
    }
    await loadMap()
    await loadAllMaps()
    return response
  }

  const switchCurrentMap = async (mapId: string) => {
    const response = await mapApi.switchMap({ mapId })
    if (!response.success) throw new Error(response.message || '切换导图失败')
    await loadMap()
    await loadAllMaps()
    return response
  }

  const importMap = async (file: File) => {
    const content = await file.text()
    const response = await mapApi.importMap({ content, filename: file.name })
    if (!response.success) throw new Error('导入导图失败')
    await loadMap()
    await loadAllMaps()
    return response
  }

  const pausePolling = () => {
    stopPolling()
  }

  const resumePolling = () => {
    startPolling()
  }

  return {
    currentMap,
    loading,
    allMaps,
    mapsLoading,
    loadMap,
    loadAllMaps,
    createNewMap,
    switchCurrentMap,
    importMap,
    startPolling,
    stopPolling,
    createNode,
    editNode,
    deleteNode,
    toggleNodeFold,
    setCurrentMap,
    pausePolling,
    resumePolling
  }
})