package org.freeplane.plugin.ai.service.scheduling;

import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.ai.service.AIServiceResponse;
import org.freeplane.plugin.ai.service.impl.DefaultAgentService;

/**
 * 构建任务执行器 - 使用现有代理服务执行构建任务
 */
public class BuildTaskExecutor {
    // 单例实例
    private static final BuildTaskExecutor instance = new BuildTaskExecutor();
    // 代理服务
    private final DefaultAgentService agentService;
    
    /**
     * 私有构造函数 - 初始化执行器组件
     */
    private BuildTaskExecutor() {
        this.agentService = new DefaultAgentService();
    }
    
    /**
     * 获取执行器单例实例
     * @return 执行器实例
     */
    public static BuildTaskExecutor getInstance() {
        return instance;
    }
    
    /**
     * 执行任务
     * @param task 任务对象
     * @return 服务响应
     */
    public AIServiceResponse executeTask(BuildTask task) {
        LogUtils.info("BuildTaskExecutor: 执行任务: " + task);
        
        try {
            // 使用现有代理服务执行任务
            AIServiceResponse response = agentService.processRequest(task.getRequest());
            
            if (response.isSuccess()) {
                LogUtils.info("BuildTaskExecutor: 任务执行成功: " + task.getId());
            } else {
                LogUtils.warn("BuildTaskExecutor: 任务执行失败: " + task.getId() + ", error: " + response.getErrorMessage());
            }
            
            return response;
        } catch (Exception e) {
            LogUtils.warn("BuildTaskExecutor: 执行任务异常: " + task.getId(), e);
            throw new RuntimeException("执行任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 异步执行任务
     * @param task 任务对象
     * @param callback 回调接口
     */
    public void executeTaskAsync(BuildTask task, TaskExecutionCallback callback) {
        new Thread(() -> {
            try {
                AIServiceResponse response = executeTask(task);
                callback.onComplete(task, response);
            } catch (Exception e) {
                callback.onError(task, e);
            }
        }).start();
    }
    
    /**
     * 任务执行回调接口
     */
    public interface TaskExecutionCallback {
        /**
         * 任务完成回调
         * @param task 任务对象
         * @param response 服务响应
         */
        void onComplete(BuildTask task, AIServiceResponse response);
        
        /**
         * 任务错误回调
         * @param task 任务对象
         * @param error 异常
         */
        void onError(BuildTask task, Exception error);
    }
}
