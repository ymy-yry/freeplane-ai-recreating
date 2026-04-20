package org.freeplane.plugin.ai.buffer;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓冲层请求上下文对象。
 * 在缓冲层各组件间传递的请求数据容器。
 */
public class BufferRequest {

    /** 用户原始输入 */
    private String userInput;

    /** 请求类型（自动生成/展开/摘要等） */
    private RequestType requestType;

    /** 附加参数 */
    private Map<String, Object> parameters;

    /** 请求时间戳 */
    private long timestamp;

    public enum RequestType {
        MINDMAP_GENERATION,   // 思维导图生成
        NODE_EXPANSION,       // 节点展开
        BRANCH_SUMMARY,       // 分支摘要
        AUTO_TAGGING,         // 自动标签
        GENERAL_CHAT          // 通用对话
    }

    public BufferRequest() {
        this.timestamp = System.currentTimeMillis();
        this.parameters = new HashMap<>();
    }

    public BufferRequest(String userInput) {
        this();
        this.userInput = userInput;
    }

    // Getters and Setters

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 添加参数
     */
    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    /**
     * 获取参数
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key) {
        return (T) this.parameters.get(key);
    }

    /**
     * 获取参数，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        Object value = this.parameters.get(key);
        return value != null ? (T) value : defaultValue;
    }

    @Override
    public String toString() {
        return "BufferRequest{" +
                "userInput='" + userInput + '\'' +
                ", requestType=" + requestType +
                ", parameters=" + parameters +
                '}';
    }
}