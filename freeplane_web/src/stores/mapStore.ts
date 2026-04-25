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

  // 新增：加载所有导图列表
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

  // 新增：创建新导图
  const createNewMap = async (title?: string) => {
    try {
      const response = await mapApi.createNewMap(title ? { title } : undefined)
      if (response.success) {
        // 创建成功后加载新的导图列表
        await loadAllMaps()
        // 并加载新创建的导图
        await loadMap()
        return response
      } else {
        throw new Error(response.message || '创建导图失败')
      }
    } catch (e) {
      console.error('创建导图失败', e)
      throw e
    }
  }

  // 新增：切换当前导图
  const switchCurrentMap = async (mapId: string) => {
    try {
      const response = await mapApi.switchMap({ mapId })
      if (response.success) {
        // 切换成功后重新加载当前导图
        await loadMap()
        // 同时更新导图列表
        await loadAllMaps()
        return response
      } else {
        throw new Error(response.message || '切换导图失败')
      }
    } catch (e) {
      console.error('切换导图失败', e)
      throw e
    }
  }

  // 新增：删除导图
  const deleteMap = async (mapId: string) => {
    try {
      const response = await mapApi.deleteMap({ mapId })
      if (response.success) {
        // 删除成功后更新导图列表
        await loadAllMaps()
        return response
      } else {
        throw new Error(response.message || '删除导图失败')
      }
    } catch (e) {
      console.error('删除导图失败', e)
      throw e
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

    // 修复：正确的递归查找函数，能够找到任意深度的节点
    const findNode = (node: any): any => {
      if (node.id === nodeId) return node
      if (node.children && node.children.length > 0) {
        for (const child of node.children) {
          const result = findNode(child)
          if (result) return result
        }
      }
      return null
    }

    const target = findNode(currentMap.value.root)
    if (!target) return

    const newFolded = !target.folded
    try {
      await mapApi.toggleFold(currentMap.value.mapId, nodeId, newFolded)
      await loadMap() // 立即从后端重新加载，确保状态一致
    } catch (e) {
      console.error('折叠操作失败', e)
    }
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
    deleteMap,
    startPolling,
    stopPolling,
    createNode,
    editNode,
    deleteNode,
    toggleNodeFold
  }
})