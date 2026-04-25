/**
 * AI 状态管理
 * 管理 AI 相关的全局状态和操作
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { useMapStore } from '@/stores/mapStore';
import * as aiApi from '@/api/aiApi';
export const useAIStore = defineStore('ai', () => {
    // ==================== 状态 ====================
    /** 可用的 AI 模型列表 */
    const modelList = ref([]);
    /** 当前选中的模型（格式：providerName:modelName） */
    const currentModel = ref('');
    /** 对话历史记录 */
    const chatHistory = ref([]);
    /** 是否正在流式输出 */
    const streaming = ref(false);
    /** 是否正在加载 */
    const loading = ref(false);
    /** AI 面板是否可见 */
    const panelVisible = ref(false);
    /** 当前 AI 模式：auto / chat / build */
    const aiMode = ref('chat');
    /** 服务路由类型 */
    const serviceType = ref('chat');
    /** Build 操作独立加载状态 */
    const buildLoading = ref(false);
    /** Build 操作结果预览 */
    const buildResult = ref('');
    const buildResultStatus = ref('idle');
    const panelWidth = ref(320);
    /** 最近一次错误信息（用于 UI 反馈） */
    const lastError = ref('');
    const lastChatFailed = ref(false);
    const lastChatRequest = ref(null);
    // ==================== 计算属性 ====================
    /** 是否有已配置的模型 */
    const hasConfiguredModels = computed(() => modelList.value.length > 0);
    /** 当前选中的模型对象 */
    const currentModelObj = computed(() => {
        const [providerName, modelName] = splitModelSelection(currentModel.value);
        return modelList.value.find(m => m.providerName === providerName && m.modelName === modelName);
    });
    // ==================== 方法 ====================
    const splitModelSelection = (selection) => {
        const idx = selection.indexOf('|');
        if (idx <= 0 || idx === selection.length - 1)
            return ['', selection];
        return [selection.slice(0, idx), selection.slice(idx + 1)];
    };
    const toModelSelection = (model) => `${model.providerName}|${model.modelName}`;
    const setMode = (mode) => {
        aiMode.value = mode;
        if (mode === 'auto')
            serviceType.value = 'auto';
        if (mode === 'chat')
            serviceType.value = 'chat';
        if (mode === 'build')
            serviceType.value = 'agent';
    };
    const setPanelWidth = (width) => {
        const normalized = Math.max(280, Math.min(680, Math.round(width)));
        panelWidth.value = normalized;
    };
    /**
     * 获取模型列表
     */
    const fetchModelList = async () => {
        try {
            loading.value = true;
            lastError.value = '';
            const response = await aiApi.getAiModels();
            modelList.value = response.data.models;
            // 如果没有选中模型，自动选择第一个
            if (modelList.value.length > 0 && !currentModel.value) {
                currentModel.value = toModelSelection(modelList.value[0]);
            }
        }
        catch (error) {
            console.error('获取模型列表失败:', error);
            lastError.value = error?.message || '获取模型列表失败';
            throw error;
        }
        finally {
            loading.value = false;
        }
    };
    /**
     * 切换模型
     */
    const switchModel = (modelSelection) => {
        currentModel.value = modelSelection;
        // 可选：清空对话历史
        // chatHistory.value = []
    };
    /**
     * 发送 AI 对话
     */
    const sendChat = async (message, ctx) => {
        if (!message.trim())
            return;
        lastChatRequest.value = { message, ctx };
        lastChatFailed.value = false;
        // 添加用户消息
        chatHistory.value.push({
            role: 'user',
            content: message,
            timestamp: Date.now(),
            nodeId: ctx?.selectedNodeId
        });
        // 添加助手占位消息
        const assistantIndex = chatHistory.value.push({
            role: 'assistant',
            content: '',
            timestamp: Date.now()
        }) - 1;
        streaming.value = true;
        try {
            const response = await aiApi.aiChat({
                message,
                modelSelection: currentModel.value,
                mapId: ctx?.mapId,
                selectedNodeId: ctx?.selectedNodeId,
                serviceType: serviceType.value
            });
            // 更新助手回复
            chatHistory.value[assistantIndex].content = response.data.reply;
        }
        catch (error) {
            const msg = error?.message || '对话失败';
            lastError.value = msg;
            lastChatFailed.value = true;
            chatHistory.value[assistantIndex].content = `❌ 对话失败：${msg}`;
            console.error('AI 对话失败:', error);
        }
        finally {
            streaming.value = false;
        }
    };
    /**
     * 发送 AI 对话（逐字显示）
     * 当前实现：先请求完整回复，再模拟逐字输出（后端 SSE 上线后可替换为真正流式）。
     */
    const sendChatStreaming = async (message, ctx) => {
        if (!message.trim())
            return;
        lastChatRequest.value = { message, ctx };
        lastChatFailed.value = false;
        chatHistory.value.push({
            role: 'user',
            content: message,
            timestamp: Date.now(),
            nodeId: ctx?.selectedNodeId
        });
        const assistantIndex = chatHistory.value.push({
            role: 'assistant',
            content: '',
            timestamp: Date.now()
        }) - 1;
        streaming.value = true;
        try {
            const response = await aiApi.aiChat({
                message,
                modelSelection: currentModel.value,
                mapId: ctx?.mapId,
                selectedNodeId: ctx?.selectedNodeId,
                serviceType: serviceType.value
            });
            const text = response.data.reply || '';
            await typeOut(chatHistory.value[assistantIndex], text, 10);
        }
        catch (error) {
            const msg = error?.message || '对话失败';
            lastError.value = msg;
            lastChatFailed.value = true;
            chatHistory.value[assistantIndex].content = `❌ 对话失败：${msg}`;
            console.error('AI 对话失败:', error);
        }
        finally {
            streaming.value = false;
        }
    };
    const typeOut = (message, fullText, delayMs) => {
        message.content = '';
        return new Promise((resolve) => {
            let i = 0;
            const timer = window.setInterval(() => {
                i += 1;
                message.content = fullText.slice(0, i);
                if (i >= fullText.length) {
                    window.clearInterval(timer);
                    resolve();
                }
            }, Math.max(0, delayMs));
        });
    };
    const normalizeResultText = (text) => {
        const trimmed = text.trim();
        if (trimmed.startsWith('```')) {
            return trimmed.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/, '');
        }
        return trimmed;
    };
    const expandNode = async (data) => {
        try {
            buildLoading.value = true;
            buildResultStatus.value = 'loading';
            lastError.value = '';
            const payload = {
                ...data,
                modelSelection: currentModel.value,
                serviceType: serviceType.value
            };
            const res = await aiApi.expandNode(payload);
            const resultText = res.data.result || res.data.summary || '';
            buildResult.value = resultText;
            buildResultStatus.value = 'ready';
            // 自动解析 JSON 并创建子节点
            if (resultText && data.nodeId) {
                const mapStore = useMapStore();
                const children = parseExpandResult(resultText);
                if (children.length > 0) {
                    await createNodesRecursive(data.nodeId, children, mapStore);
                }
                await mapStore.loadMap();
            }
            return res.data;
        }
        catch (error) {
            lastError.value = error?.message || '展开节点失败';
            buildResultStatus.value = 'idle';
            throw error;
        }
        finally {
            buildLoading.value = false;
        }
    };
    const parseExpandResult = (text) => {
        const normalized = normalizeResultText(text);
        if (!normalized)
            return [];
        try {
            const parsed = JSON.parse(normalized);
            // 支持 { children: [...] } 格式
            if (parsed?.children && Array.isArray(parsed.children)) {
                return parsed.children.filter((item) => item?.text);
            }
            // 支持直接数组格式
            if (Array.isArray(parsed)) {
                return parsed.filter((item) => item?.text || typeof item === 'string')
                    .map((item) => typeof item === 'string' ? { text: item } : item);
            }
            // 支持有 text 的单个节点
            if (parsed?.text) {
                return parsed.children ? [parsed] : [parsed];
            }
        }
        catch (e) {
            console.warn('解析 expandNode 返回的 JSON 失败:', e);
        }
        return [];
    };
    const createNodesRecursive = async (parentId, nodes, mapStore) => {
        for (const node of nodes) {
            await mapStore.createNode(parentId, node.text);
            // 注意：createNode 返回 void，暂不支持深层嵌套创建
            // 如需深层嵌套，需要 mapStore.createNode 返回新节点ID
        }
    };
    const summarize = async (data) => {
        try {
            buildLoading.value = true;
            buildResultStatus.value = 'loading';
            lastError.value = '';
            const res = await aiApi.summarizeBranch({
                ...data,
                modelSelection: currentModel.value,
                serviceType: serviceType.value
            });
            buildResult.value = res.data.summary || '';
            buildResultStatus.value = 'ready';
            const mapStore = useMapStore();
            await mapStore.loadMap();
            return res.data;
        }
        catch (error) {
            lastError.value = error?.message || '分支摘要失败';
            throw error;
        }
        finally {
            buildLoading.value = false;
        }
    };
    const tagNodes = async (data) => {
        try {
            buildLoading.value = true;
            buildResultStatus.value = 'loading';
            lastError.value = '';
            const res = await aiApi.autoTag({
                ...data,
                modelSelection: currentModel.value,
                serviceType: serviceType.value
            });
            const first = res.data.results?.[0];
            buildResult.value = first?.result || res.data.message || '';
            buildResultStatus.value = 'ready';
            const mapStore = useMapStore();
            await mapStore.loadMap();
            return res.data;
        }
        catch (error) {
            lastError.value = error?.message || '自动标签失败';
            throw error;
        }
        finally {
            buildLoading.value = false;
        }
    };
    const generateMindMap = async (topic, options) => {
        try {
            buildLoading.value = true;
            buildResultStatus.value = 'loading';
            lastError.value = '';
            const res = await aiApi.generateMindMap({
                topic,
                maxDepth: options?.maxDepth,
                modelSelection: currentModel.value,
                serviceType: serviceType.value
            });
            buildResult.value = res.data.result || `已生成 ${res.data.nodeCount || 0} 个节点`;
            buildResultStatus.value = 'ready';
            const mapStore = useMapStore();
            await mapStore.loadMap();
            return res.data;
        }
        catch (error) {
            lastError.value = error?.message || '生成导图失败';
            throw error;
        }
        finally {
            buildLoading.value = false;
        }
    };
    const parseTreeNodes = (text) => {
        const normalized = normalizeResultText(text);
        if (!normalized)
            return [];
        try {
            const parsed = JSON.parse(normalized);
            if (Array.isArray(parsed)) {
                return parsed.filter((item) => item?.text || typeof item === 'string')
                    .map((item) => typeof item === 'string' ? { text: item } : item);
            }
            if (parsed?.children && Array.isArray(parsed.children)) {
                return parsed.children.filter((item) => item?.text || typeof item === 'string')
                    .map((item) => typeof item === 'string' ? { text: item } : item);
            }
            if (parsed?.text) {
                return [parsed];
            }
        }
        catch (e) {
            console.warn('解析 AI 返回的 JSON 失败:', e);
        }
        return [];
    };
    const applyBuildResultToMap = async (targetNodeId, mapId) => {
        const mapStore = useMapStore();
        const treeNodes = parseTreeNodes(buildResult.value);
        if (!treeNodes.length) {
            throw new Error('结果中未解析到可应用的节点，请确保 AI 返回 JSON 数组或 children 结构');
        }
        for (const node of treeNodes) {
            await mapStore.createNode(targetNodeId, node.text);
        }
        if (mapId) {
            await mapStore.loadMap();
        }
    };
    const retryLastChat = async () => {
        if (!lastChatRequest.value)
            return;
        const { message, ctx } = lastChatRequest.value;
        await sendChatStreaming(message, ctx);
    };
    const sendSmart = async (input, ctx) => {
        if (!input.trim())
            return;
        loading.value = true;
        lastError.value = '';
        try {
            const response = await aiApi.smartRequest({
                input,
                mapId: ctx?.mapId,
                selectedNodeId: ctx?.selectedNodeId,
                modelSelection: currentModel.value
            });
            const output = typeof response.data.data === 'string' ? response.data.data : JSON.stringify(response.data.data || {}, null, 2);
            buildResult.value = output;
            return response.data;
        }
        catch (error) {
            lastError.value = error?.message || 'Auto 模式请求失败';
            throw error;
        }
        finally {
            loading.value = false;
        }
    };
    const keywordSearch = async (data) => {
        try {
            loading.value = true;
            lastError.value = '';
            const res = await aiApi.searchNodes(data);
            return res.data;
        }
        catch (error) {
            lastError.value = error?.message || '搜索失败';
            throw error;
        }
        finally {
            loading.value = false;
        }
    };
    /**
     * 清空对话历史
     */
    const clearChat = () => {
        chatHistory.value = [];
        lastChatFailed.value = false;
        lastChatRequest.value = null;
    };
    /**
     * 切换 AI 面板显示状态
     */
    const togglePanel = () => {
        panelVisible.value = !panelVisible.value;
    };
    /**
     * 显示 AI 面板
     */
    const showPanel = () => {
        panelVisible.value = true;
    };
    /**
     * 隐藏 AI 面板
     */
    const hidePanel = () => {
        panelVisible.value = false;
    };
    /**
     * 初始化 AI Store
     */
    const init = async () => {
        try {
            await fetchModelList();
            setMode(aiMode.value);
        }
        catch (error) {
            console.warn('AI Store 初始化失败:', error);
        }
    };
    const saveCustomModel = async (payload) => {
        await aiApi.saveModelConfig(payload);
        await fetchModelList();
    };
    // ==================== 返回 ====================
    return {
        // 状态
        modelList,
        currentModel,
        chatHistory,
        streaming,
        loading,
        panelVisible,
        aiMode,
        serviceType,
        buildLoading,
        buildResult,
        buildResultStatus,
        panelWidth,
        lastError,
        lastChatFailed,
        // 计算属性
        hasConfiguredModels,
        currentModelObj,
        // 方法
        fetchModelList,
        switchModel,
        sendChat,
        sendChatStreaming,
        clearChat,
        togglePanel,
        showPanel,
        hidePanel,
        setMode,
        setPanelWidth,
        init,
        expandNode,
        summarize,
        tagNodes,
        keywordSearch,
        generateMindMap,
        sendSmart,
        saveCustomModel,
        applyBuildResultToMap,
        retryLastChat
    };
});
