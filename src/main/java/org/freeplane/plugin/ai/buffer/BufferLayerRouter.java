package org.freeplane.plugin.ai.buffer;

import org.freeplane.core.util.LogUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 缓冲层路由器。
 * 负责识别功能类型，选择合适的缓冲层进行处理。
 */
public class BufferLayerRouter {

    private final List<IBufferLayer> bufferLayers;

    public BufferLayerRouter() {
        this.bufferLayers = new ArrayList<>();
        initializeBufferLayers();
    }

    /**
     * 初始化缓冲层列表
     * 使用 ServiceLoader 自动发现所有 IBufferLayer 实现
     */
    private void initializeBufferLayers() {
        // 通过 ServiceLoader 自动发现
        ServiceLoader<IBufferLayer> loader = ServiceLoader.load(IBufferLayer.class);
        for (IBufferLayer layer : loader) {
            bufferLayers.add(layer);
            LogUtils.info("BufferLayerRouter: loaded buffer layer - " + layer.getName());
        }

        // 如果没有通过 ServiceLoader 找到，手动注册默认实现
        if (bufferLayers.isEmpty()) {
            registerDefaultBufferLayers();
        }

        // 按优先级排序
        bufferLayers.sort(Comparator.comparingInt(IBufferLayer::getPriority));
    }

    /**
     * 手动注册默认缓冲层
     */
    private void registerDefaultBufferLayers() {
        try {
            // 注册思维导图缓冲层
            Class<?> clazz = Class.forName("org.freeplane.plugin.ai.buffer.mindmap.MindMapBufferLayer");
            IBufferLayer mindMapLayer = (IBufferLayer) clazz.getDeclaredConstructor().newInstance();
            bufferLayers.add(mindMapLayer);
            LogUtils.info("BufferLayerRouter: registered MindMapBufferLayer");
        } catch (Exception e) {
            LogUtils.warn("BufferLayerRouter: failed to register MindMapBufferLayer", e);
        }
    }

    /**
     * 处理请求
     * @param request 请求上下文
     * @return 处理结果
     */
    public BufferResponse processRequest(BufferRequest request) {
        long startTime = System.currentTimeMillis();

        // 找到能处理该请求的缓冲层
        IBufferLayer selectedLayer = selectBufferLayer(request);
        if (selectedLayer == null) {
            BufferResponse response = new BufferResponse();
            response.setSuccess(false);
            response.setErrorMessage("未找到合适的缓冲层处理该请求");
            response.setProcessingTime(System.currentTimeMillis() - startTime);
            return response;
        }

        LogUtils.info("BufferLayerRouter: selected layer - " + selectedLayer.getName());

        try {
            // 委托给选中的缓冲层处理
            BufferResponse response = selectedLayer.process(request);
            response.setProcessingTime(System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            LogUtils.warn("BufferLayerRouter: processing failed", e);
            BufferResponse response = new BufferResponse();
            response.setSuccess(false);
            response.setErrorMessage("处理失败: " + e.getMessage());
            response.setProcessingTime(System.currentTimeMillis() - startTime);
            return response;
        }
    }

    /**
     * 选择合适的缓冲层
     */
    private IBufferLayer selectBufferLayer(BufferRequest request) {
        for (IBufferLayer layer : bufferLayers) {
            if (layer.canHandle(request)) {
                return layer;
            }
        }
        return null;
    }

    /**
     * 获取所有已注册的缓冲层
     */
    public List<IBufferLayer> getBufferLayers() {
        return new ArrayList<>(bufferLayers);
    }
}