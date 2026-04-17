package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

/**
 * Agent 增强配置类
 * 支持更高级的 Agent 推理模式和行为控制
 */
public class AIAgentConfiguration {
    
    // 配置属性键
    private static final String AGENT_ENABLE_CHAIN_OF_THOUGHT = "ai_agent_enable_chain_of_thought";
    private static final String AGENT_ENABLE_TOOL_VALIDATION = "ai_agent_enable_tool_validation";
    private static final String AGENT_ENABLE_QUALITY_CHECK = "ai_agent_enable_quality_check";
    private static final String AGENT_MAX_TOOL_CALLS_PER_TURN = "ai_agent_max_tool_calls_per_turn";
    private static final String AGENT_ENABLE_SELF_CORRECTION = "ai_agent_enable_self_correction";
    
    private final ResourceController resourceController;
    
    public AIAgentConfiguration() {
        this(ResourceController.getResourceController());
    }
    
    AIAgentConfiguration(ResourceController resourceController) {
        this.resourceController = resourceController;
    }
    
    /**
     * 是否启用思维链推理
     * 默认：true
     */
    public boolean isChainOfThoughtEnabled() {
        String value = resourceController.getProperty(AGENT_ENABLE_CHAIN_OF_THOUGHT, "true");
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 是否启用工具调用前验证
     * 默认：true
     */
    public boolean isToolValidationEnabled() {
        String value = resourceController.getProperty(AGENT_ENABLE_TOOL_VALIDATION, "true");
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 是否启用响应质量检查
     * 默认：true
     */
    public boolean isQualityCheckEnabled() {
        String value = resourceController.getProperty(AGENT_ENABLE_QUALITY_CHECK, "true");
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 每轮对话最大工具调用次数
     * 默认：10
     */
    public int getMaxToolCallsPerTurn() {
        String value = resourceController.getProperty(AGENT_MAX_TOOL_CALLS_PER_TURN, "10");
        try {
            int maxCalls = Integer.parseInt(value);
            return Math.max(1, Math.min(maxCalls, 50)); // 限制在 1-50 之间
        } catch (NumberFormatException e) {
            return 10;
        }
    }
    
    /**
     * 是否启用自我修正机制
     * 默认：true
     */
    public boolean isSelfCorrectionEnabled() {
        String value = resourceController.getProperty(AGENT_ENABLE_SELF_CORRECTION, "true");
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 获取 Agent 配置摘要（用于日志和调试）
     */
    public String getConfigurationSummary() {
        return String.format(
            "Agent Configuration [ChainOfThought: %s, ToolValidation: %s, QualityCheck: %s, MaxToolCalls: %d, SelfCorrection: %s]",
            isChainOfThoughtEnabled(),
            isToolValidationEnabled(),
            isQualityCheckEnabled(),
            getMaxToolCallsPerTurn(),
            isSelfCorrectionEnabled()
        );
    }
}
