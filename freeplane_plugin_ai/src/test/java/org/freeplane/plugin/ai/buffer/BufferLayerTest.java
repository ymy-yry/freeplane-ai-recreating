package org.freeplane.plugin.ai.buffer;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓冲层核心对象单元测试
 */
public class BufferLayerTest {

    private BufferRequest request;
    private BufferResponse response;

    @Before
    public void setUp() {
        request = new BufferRequest();
        response = new BufferResponse();
    }

    @Test
    public void testBufferRequestCreation() {
        // 测试请求对象创建
        BufferRequest req = new BufferRequest("帮我生成思维导图");
        assertThat(req.getUserInput()).isEqualTo("帮我生成思维导图");
        assertThat(req.getParameters()).isNotNull();
        assertThat(req.getTimestamp()).isGreaterThan(0);
    }

    @Test
    public void testBufferRequestParameters() {
        // 测试参数添加和获取
        request.addParameter("topic", "Java 学习");
        request.addParameter("maxDepth", 3);

        String topic = request.getParameter("topic");
        Integer maxDepth = request.getParameter("maxDepth");
        String nonExistent = request.getParameter("nonExistent");
        String withDefault = request.getParameter("nonExistent", "default");

        assertThat(topic).isEqualTo("Java 学习");
        assertThat(maxDepth).isEqualTo(3);
        assertThat(nonExistent).isNull();
        assertThat(withDefault).isEqualTo("default");
    }

    @Test
    public void testBufferRequestType() {
        // 测试请求类型设置
        request.setRequestType(BufferRequest.RequestType.MINDMAP_GENERATION);
        assertThat(request.getRequestType()).isEqualTo(BufferRequest.RequestType.MINDMAP_GENERATION);
    }

    @Test
    public void testBufferResponseSuccess() {
        // 测试成功响应
        response.setSuccess(true);
        response.setUsedModel("openai/gpt-4o");
        response.setQualityScore(88.5);
        response.addLog("需求识别: MINDMAP_GENERATION");
        response.addLog("模型选择: openai/gpt-4o");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getUsedModel()).isEqualTo("openai/gpt-4o");
        assertThat(response.getQualityScore()).isEqualTo(88.5);
        assertThat(response.getLogs()).hasSize(2);
    }

    @Test
    public void testBufferResponseError() {
        // 测试错误响应
        response.setSuccess(false);
        response.setErrorMessage("测试错误信息");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("测试错误信息");
    }

    @Test
    public void testBufferResponseStaticMethods() {
        // 测试静态工厂方法
        BufferResponse successResponse = BufferResponse.success(null);
        assertThat(successResponse.isSuccess()).isTrue();

        BufferResponse errorResponse = BufferResponse.error("错误");
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getErrorMessage()).isEqualTo("错误");
    }

    @Test
    public void testBufferResponsePutData() {
        // 测试数据设置
        response.putData("nodeCount", 10);
        response.putData("topic", "测试主题");

        assertThat(response.getData().get("nodeCount")).isEqualTo(10);
        assertThat(response.getData().get("topic")).isEqualTo("测试主题");
    }

    @Test
    public void testBufferRequestToString() {
        // 测试 toString 方法
        request.setUserInput("测试输入");
        request.setRequestType(BufferRequest.RequestType.MINDMAP_GENERATION);

        String str = request.toString();
        assertThat(str).contains("测试输入");
        assertThat(str).contains("MINDMAP_GENERATION");
    }

    @Test
    public void testBufferResponseToString() {
        // 测试 toString 方法
        response.setSuccess(true);
        response.setUsedModel("openai/gpt-4o");

        String str = response.toString();
        assertThat(str).contains("openai/gpt-4o");
        assertThat(str).contains("success=true");
    }
}