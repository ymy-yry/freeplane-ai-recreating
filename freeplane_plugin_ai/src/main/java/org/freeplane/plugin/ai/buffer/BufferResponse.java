package org.freeplane.plugin.ai.buffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓冲层响应对象。
 * 包含处理结果、质量评分、优化日志等信息。
 */
public class BufferResponse {

    /** 是否成功 */
    private boolean success;

    /** 处理结果数据 */
    private Map<String, Object> data;

    /** 使用的模型 */
    private String usedModel;

    /** 质量评分（0-100） */
    private double qualityScore;

    /** 处理时间（毫秒） */
    private long processingTime;

    /** 处理日志 */
    private List<String> logs;

    /** 错误信息 */
    private String errorMessage;

    public BufferResponse() {
        this.success = false;
        this.data = new HashMap<>();
        this.logs = new ArrayList<>();
        this.qualityScore = 0.0;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getUsedModel() {
        return usedModel;
    }

    public void setUsedModel(String usedModel) {
        this.usedModel = usedModel;
    }

    public double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 添加日志
     */
    public void addLog(String log) {
        this.logs.add(log);
    }

    /**
     * 设置结果数据
     */
    public void putData(String key, Object value) {
        this.data.put(key, value);
    }

    /**
     * 构建成功响应
     */
    public static BufferResponse success(Map<String, Object> data) {
        BufferResponse response = new BufferResponse();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    /**
     * 构建失败响应
     */
    public static BufferResponse error(String errorMessage) {
        BufferResponse response = new BufferResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    @Override
    public String toString() {
        return "BufferResponse{" +
                "success=" + success +
                ", usedModel='" + usedModel + '\'' +
                ", qualityScore=" + qualityScore +
                ", processingTime=" + processingTime +
                ", logs=" + logs +
                '}';
    }
}