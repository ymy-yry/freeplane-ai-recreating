package org.freeplane.plugin.ai.buffer;

import org.freeplane.core.util.LogUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓冲层路由器。
 * 负责识别功能类型，选择合适的缓冲层进行处理。
 * 支持缓存机制，提高处理效率。
 */
public class BufferLayerRouter {

    private final List<IBufferLayer> bufferLayers;
    
    // 缓存相关
    private final Map<String, CachedResponse> cache;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRY_TIME = TimeUnit.MINUTES.toMillis(10);

    public BufferLayerRouter() {
        this.bufferLayers = new ArrayList<>();
        this.cache = new ConcurrentHashMap<>();
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

        // 尝试从缓存获取
        String cacheKey = generateCacheKey(request);
        CachedResponse cachedResponse = cache.get(cacheKey);
        if (cachedResponse != null && !isCacheExpired(cachedResponse)) {
            LogUtils.info("BufferLayerRouter: cache hit for key - " + cacheKey);
            BufferResponse response = cachedResponse.getResponse();
            response.setProcessingTime(System.currentTimeMillis() - startTime);
            response.addLog("Cache hit: " + cacheKey);
            return response;
        }

        // 缓存未命中，正常处理
        LogUtils.info("BufferLayerRouter: cache miss for key - " + cacheKey);

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
            
            // 缓存成功的响应
            if (response.isSuccess()) {
                cacheResponse(cacheKey, response);
                response.addLog("Cache stored: " + cacheKey);
            }
            
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
     * 生成缓存键
     */
    private String generateCacheKey(BufferRequest request) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(request.getRequestType())
                 .append("|")
                 .append(request.getUserInput() != null ? request.getUserInput() : "");
        
        // 添加参数信息
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            keyBuilder.append("|");
            request.getParameters().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    keyBuilder.append(entry.getKey())
                             .append("=")
                             .append(entry.getValue())
                             .append("&");
                });
        }
        
        return keyBuilder.toString();
    }

    /**
     * 缓存响应
     */
    private void cacheResponse(String key, BufferResponse response) {
        // 检查缓存大小
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestCache();
        }
        
        cache.put(key, new CachedResponse(response));
        LogUtils.info("BufferLayerRouter: cached response for key - " + key);
    }

    /**
     * 检查缓存是否过期
     */
    private boolean isCacheExpired(CachedResponse cachedResponse) {
        return System.currentTimeMillis() - cachedResponse.getTimestamp() > CACHE_EXPIRY_TIME;
    }

    /**
     * 移除最旧的缓存
     */
    private void evictOldestCache() {
        String oldestKey = null;
        long oldestTimestamp = Long.MAX_VALUE;
        
        for (Map.Entry<String, CachedResponse> entry : cache.entrySet()) {
            if (entry.getValue().getTimestamp() < oldestTimestamp) {
                oldestTimestamp = entry.getValue().getTimestamp();
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
            LogUtils.info("BufferLayerRouter: evicted oldest cache - " + oldestKey);
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
        LogUtils.info("BufferLayerRouter: cache cleared");
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 获取所有已注册的缓冲层
     */
    public List<IBufferLayer> getBufferLayers() {
        return new ArrayList<>(bufferLayers);
    }

    /**
     * 缓存响应包装类
     */
    private static class CachedResponse {
        private final BufferResponse response;
        private final long timestamp;

        public CachedResponse(BufferResponse response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }

        public BufferResponse getResponse() {
            return response;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}