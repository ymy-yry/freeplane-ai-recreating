package org.freeplane.plugin.ai.buffer;

/**
 * 智能缓冲层标准接口。
 * 所有缓冲层实现都必须遵循此接口，确保可插拔性和一致性。
 */
public interface IBufferLayer {

    /**
     * 获取缓冲层名称
     * @return 缓冲层标识名称
     */
    String getName();

    /**
     * 判断是否能处理该请求
     * @param request 请求上下文
     * @return true 表示能处理
     */
    boolean canHandle(BufferRequest request);

    /**
     * 处理请求
     * @param request 请求上下文
     * @return 处理结果
     */
    BufferResponse process(BufferRequest request);

    /**
     * 获取缓冲层优先级（数字越小优先级越高）
     * @return 优先级值
     */
    default int getPriority() {
        return 100;
    }
}