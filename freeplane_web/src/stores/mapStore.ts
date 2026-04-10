import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MindMapData } from '@/types/mindmap'
import * as mapApi from '@/api/mapApi'

export const useMapStore = defineStore('map', () => {
  const currentMap = ref<MindMapData | null>(null)
  const allMaps = ref<{ mapId: string; title: string }[]>([])
  const loading = ref(false)
  const searchResults = ref<{ nodeId: string; text: string; path: string }[]>([])
  let pollingInterval: NodeJS.Timeout | null = null

  const loadMap = async (mapId?: string) => {
    loading.value = true
    try {
      if (mapId) {
        // 加载指定导图
        currentMap.value = await mapApi.getMapDetails(mapId)
      } else {
        // 加载当前激活的导图
        currentMap.value = await mapApi.getCurrentMap()
      }
    } catch (e) {
      console.error('加载导图失败', e)
    } finally {
      loading.value = false
    }
  }

  const loadAllMaps = async () => {
    try {
      allMaps.value = await mapApi.getAllMaps()
    } catch (e) {
      console.error('加载导图列表失败', e)
      // 如果后端API不存在，返回空数组
      allMaps.value = []
    }
  }

  const createNewMap = async (title: string) => {
    try {
      currentMap.value = await mapApi.createNewMap(title)
      await loadAllMaps() // 刷新导图列表
    } catch (e) {
      console.error('创建导图失败', e)
      throw e
    }
  }

  const switchMap = async (mapId: string) => {
    try {
      // 先切换激活状态
      currentMap.value = await mapApi.switchMap(mapId)
      // 然后加载导图数据
      await loadMap(mapId)
    } catch (e) {
      console.error('切换导图失败', e)
      throw e
    }
  }

  const searchNodes = (query: string) => {
    if (!query.trim() || !currentMap.value) {
      searchResults.value = []
      return
    }

    const results: { nodeId: string; text: string; path: string }[] = []
    const searchTerm = query.toLowerCase()

    const searchInNode = (node: any, path: string = '') => {
      const currentPath = path ? `${path} > ${node.text}` : node.text
      
      if (node.text.toLowerCase().includes(searchTerm)) {
        results.push({
          nodeId: node.id,
          text: node.text,
          path: currentPath
        })
      }

      if (!node.folded && node.children) {
        node.children.forEach((child: any) => searchInNode(child, currentPath))
      }
    }

    searchInNode(currentMap.value.root)
    searchResults.value = results
  }

  const startPolling = () => {
    if (pollingInterval) clearInterval(pollingInterval)
    pollingInterval = setInterval(() => loadMap(), 3000)
  }

  const stopPolling = () => {
    if (pollingInterval) clearInterval(pollingInterval)
  }

  const createNode = async (parentId: string, text: string, mapId?: string) => {
    if (!currentMap.value) return
    await mapApi.createNode(mapId || currentMap.value.mapId, parentId, text)
    await loadMap(mapId || currentMap.value.mapId)
  }

  const editNode = async (nodeId: string, text: string, mapId?: string) => {
    if (!currentMap.value) return
    await mapApi.editNode(mapId || currentMap.value.mapId, nodeId, text)
    await loadMap(mapId || currentMap.value.mapId)
  }

  const deleteNode = async (nodeId: string, mapId?: string) => {
    if (!currentMap.value) return
    await mapApi.deleteNode(mapId || currentMap.value.mapId, nodeId)
    await loadMap(mapId || currentMap.value.mapId)
  }

  const toggleNodeFold = async (nodeId: string, mapId?: string) => {
    if (!currentMap.value) return

    const findNode = (node: any): any => {
      if (node.id === nodeId) return node
      return node.children?.find(findNode)
    }
    const target = findNode(currentMap.value.root)
    if (!target) return

    const newFolded = !target.folded

    try {
      await mapApi.toggleFold(mapId || currentMap.value.mapId, nodeId, newFolded)
      await loadMap(mapId || currentMap.value.mapId)
    } catch (e) {
      console.error('折叠操作失败', e)
    }
  }

  return {
    currentMap,
    allMaps,
    loading,
    searchResults,
    loadMap,
    loadAllMaps,
    createNewMap,
    switchMap,
    searchNodes,
    startPolling,
    stopPolling,
    createNode,
    editNode,
    deleteNode,
    toggleNodeFold
  }
})