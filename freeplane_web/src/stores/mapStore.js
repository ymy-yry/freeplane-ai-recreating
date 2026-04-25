import { defineStore } from 'pinia';
import { ref } from 'vue';
import * as mapApi from '@/api/mapApi';
export const useMapStore = defineStore('map', () => {
    const currentMap = ref(null);
    const loading = ref(false);
    let pollingInterval = null;
    const loadMap = async () => {
        loading.value = true;
        try {
            currentMap.value = await mapApi.getCurrentMap();
        }
        catch (e) {
            console.error('加载导图失败', e);
        }
        finally {
            loading.value = false;
        }
    };
    const startPolling = () => {
        if (pollingInterval)
            clearInterval(pollingInterval);
        pollingInterval = setInterval(loadMap, 3000);
    };
    const stopPolling = () => {
        if (pollingInterval)
            clearInterval(pollingInterval);
    };
    const createNode = async (parentId, text) => {
        if (!currentMap.value)
            return;
        await mapApi.createNode(currentMap.value.mapId, parentId, text);
        await loadMap();
    };
    const editNode = async (nodeId, text) => {
        if (!currentMap.value)
            return;
        await mapApi.editNode(currentMap.value.mapId, nodeId, text);
        await loadMap();
    };
    const deleteNode = async (nodeId) => {
        if (!currentMap.value)
            return;
        await mapApi.deleteNode(currentMap.value.mapId, nodeId);
        await loadMap();
    };
    const toggleNodeFold = async (nodeId) => {
        if (!currentMap.value)
            return;
        const findNode = (node) => {
            if (node.id === nodeId)
                return node;
            return node.children?.find(findNode);
        };
        const target = findNode(currentMap.value.root);
        if (!target)
            return;
        const newFolded = !target.folded;
        try {
            await mapApi.toggleFold(currentMap.value.mapId, nodeId, newFolded);
            await loadMap(); // 关键：立即从后端重新加载，确保状态一致
        }
        catch (e) {
            console.error('折叠操作失败', e);
        }
    };
    const setCurrentMap = (mapData) => {
        currentMap.value = mapData;
    };
    const pausePolling = () => {
        stopPolling();
    };
    const resumePolling = () => {
        startPolling();
    };
    return {
        currentMap,
        loading,
        loadMap,
        startPolling,
        stopPolling,
        createNode,
        editNode,
        deleteNode,
        toggleNodeFold,
        setCurrentMap,
        pausePolling,
        resumePolling
    };
});
