# AI 配置问题排查与修复全过程

## 概述

本文记录了 Freeplane AI 插件集成百度文心（ERNIE）API 时，从报错 `500 - AI service not initialized` 到最终成功调用的完整排查与修复过程。涉及 OSGi 插件配置机制、secrets 文件读取逻辑、模型选择解析格式等多个层面的问题。

---

## 一、问题现象

前端调用 AI 对话接口时报错：

```
❌ 对话失败：Request failed with status code 500
```

后端响应体：

```json
{"error": "AI service not initialized. Please configure AI provider in preferences."}
```

---

## 二、环境说明

| 项目 | 值 |
|------|-----|
| 应用 | Freeplane 1.13.x |
| AI 插件 | `freeplane_plugin_ai` |
| AI Provider | 百度文心（ERNIE）`ernie-4.5-turbo-128k` |
| API 地址 | `https://qianfan.baidubce.com/v2` |
| 用户数据目录 | `%APPDATA%\Freeplane\1.12.x\`（1.13.x 与 1.12.x 共用前两位版本号） |
| REST API 端口 | 6299 |

---

## 三、排查过程

### 3.1 初步分析：服务未初始化

检查 `DefaultChatService.ensureServiceInitialized()` 逻辑：

```java
// DefaultChatService.java
private void ensureServiceInitialized() {
    if (chatService == null) {
        synchronized (DefaultChatService.class) {
            if (chatService == null) {
                AIProviderConfiguration configuration = new AIProviderConfiguration();
                if (!isProviderConfigured(configuration)) {
                    LogUtils.warn("DefaultChatService: No AI provider configured");
                    return;  // ← 直接返回，chatService 始终为 null
                }
                // ...
            }
        }
    }
}
```

`isProviderConfigured()` 当时只检查 openrouter/gemini/ollama，**没有检查 ernie 和 dashscope**，导致即使配置了 ernie key 也判断为"未配置"。

**修复**：在 `isProviderConfigured()` 中增加 ernie 和 dashscope 的判断。

---

### 3.2 第二层问题：Missing model selection

修复 `isProviderConfigured()` 后，服务能进入初始化，但又报新错误：

```
java.lang.IllegalArgumentException: Missing model selection
    at AIChatModelFactory.createChatLanguageModel(AIChatModelFactory.java:27)
```

`AIChatModelFactory.createChatLanguageModel()` 调用链：

```java
AIModelSelection selection = AIModelSelection.fromSelectionValue(
    configuration.getSelectedModelValue()  // ← 返回 null
);
if (selection == null) {
    throw new IllegalArgumentException("Missing model selection");
}
```

`getSelectedModelValue()` 读取属性 `ai_selected_model`，但该值为空字符串。

**修复方向**：在 `AIProviderConfiguration.getSelectedModelValue()` 中增加 fallback 推断逻辑。

---

### 3.3 Secrets 文件机制分析

#### 属性读取优先级

Freeplane 使用 `ApplicationPropertyStore` 管理属性，初始化结构如下：

```
defProps（defaults.properties 插件默认值）
    ↓ 继承
props（freeplane.properties 用户配置）
    ↓ 继承
secretsProps（secrets.properties 敏感配置）
    ↓ 继承
securedProps（运行时安全属性，getProperty 最终从这里读）
```

关键代码（`ApplicationPropertyStore` 构造器）：

```java
props = new SortedProperties(defProps);
hasLoadedAutoProperties = loadUserProperties(props, autoPropertiesFile);
secretsProps = new SortedProperties(props);
hasLoadedSecretsProperties = loadUserProperties(secretsProps, secretsPropertiesFile);
securedProps = new Properties(secretsProps);
```

#### persistPropertyInSecretsFile 的作用

```java
void persistPropertyInSecretsFile(String key) {
    if (props.containsKey(key)) {
        // 将该 key 从 props 移到 secretsProps
        String value = props.getProperty(key);
        if (value != null) {
            secretsProps.setProperty(key, value);
        }
        props.remove(key);
    }
    persistedInSecretsFilePropertyKeys.add(key);  // 标记：此 key 今后只写 secrets 文件
}
```

**结论**：若某个 key 被注册为 secrets key，但 `secrets.properties` 里没有它，`getProperty()` 只能从更低层的 `defProps`（defaults.properties）读到默认值。API key 这类敏感信息不应写在 defaults.properties 里。

#### Freeplane 1.13.x 用户目录问题

Freeplane 1.13.x 实际读写的用户目录是 **`%APPDATA%\Freeplane\1.12.x\`**（按前两位版本号共用），而非 `1.13.x`。

ernie key 之前被写入了根目录的 `freeplane.properties`，但实际有效的 secrets 文件路径是：

```
C:\Users\<用户名>\AppData\Roaming\Freeplane\1.12.x\secrets.properties
```

---

### 3.4 第三层问题：Activator 未注册 ernie/dashscope key

检查 `Activator.java` 的 `markSecretsForSeparatePersistence()`：

**修复前**（缺少 ernie/dashscope 注册）：

```java
private void markSecretsForSeparatePersistence(ResourceController resourceController) {
    resourceController.persistPropertyInSecretsFile(OPENROUTER_KEY_PROPERTY);
    resourceController.persistPropertyInSecretsFile(GEMINI_KEY_PROPERTY);
    resourceController.persistPropertyInSecretsFile(OLLAMA_API_KEY_PROPERTY);
    resourceController.persistPropertyInSecretsFile(MCP_TOKEN_PROPERTY);
    // ← ernie 和 dashscope 未注册！
}
```

**修复后**：

```java
private static final String DASHSCOPE_KEY_PROPERTY = "ai_dashscope_key";
private static final String ERNIE_KEY_PROPERTY = "ai_ernie_key";

private void markSecretsForSeparatePersistence(ResourceController resourceController) {
    resourceController.persistPropertyInSecretsFile(OPENROUTER_KEY_PROPERTY);
    resourceController.persistPropertyInSecretsFile(GEMINI_KEY_PROPERTY);
    resourceController.persistPropertyInSecretsFile(OLLAMA_API_KEY_PROPERTY);
    resourceController.persistPropertyInSecretsFile(MCP_TOKEN_PROPERTY);
    resourceController.persistPropertyInSecretsFile(DASHSCOPE_KEY_PROPERTY);
    resourceController.persistPropertyInSecretsFile(ERNIE_KEY_PROPERTY);  // ← 新增
}
```

---

### 3.5 第四层问题：secrets.properties 写入时 key 被换行截断

将 ernie key 通过 PowerShell `Add-Content` 追加到 secrets.properties 时，由于终端行宽限制，key 被截断为两行：

```properties
ai_ernie_key=bce-v3/ALTAK-AfROKZ9YqhoydcZnJK
KFH/8146984583225f5262f7e6b5e20f07221e016a5c
```

Java `Properties.load()` 不识别这种换行续行（需显式 `\` 才能续行），导致 key 值被截断，`hasErnieKey()` 返回 false。

**修复**：使用 `[System.IO.File]::WriteAllText()` 直接写入，避免换行问题：

```powershell
$content = "#Freeplane 1.13.3`r`n`r`nai_ernie_key=bce-v3/ALTAK-AfROKZ9YqhoydcZnJKKFH/8146984583225f5262f7e6b5e20f07221e016a5c`r`n"
[System.IO.File]::WriteAllText(
    "C:\Users\<用户名>\AppData\Roaming\Freeplane\1.12.x\secrets.properties",
    $content
)
```

---

### 3.6 根本原因：ai_selected_model 格式错误（分隔符错误）

服务初始化能走通后，测试接口仍返回 500，日志显示：

```
java.lang.IllegalArgumentException: Missing model selection
```

检查 `defaults.properties`：

```properties
ai_selected_model = ernie:ernie-4.5   ← 错误！使用了冒号
```

但 `AIModelSelection` 类使用 **管道符 `|`** 作为分隔符：

```java
// AIModelSelection.java
static final String SELECTION_SEPARATOR = "|";

static AIModelSelection fromSelectionValue(String selectionValue) {
    int separatorIndex = selectionValue.indexOf(SELECTION_SEPARATOR);
    if (separatorIndex <= 0 || separatorIndex >= selectionValue.length() - 1) {
        return null;  // ← 冒号找不到管道符，返回 null
    }
    // ...
}
```

`"ernie:ernie-4.5"` 中找不到 `|`，`separatorIndex = -1`，`fromSelectionValue` 返回 `null`，最终抛出 `Missing model selection`。

---

### 3.7 模型名称不存在

修正分隔符后，API 能被调用，但返回：

```json
{"error": {"code": "invalid_model", "message": "The model does not exist or you do not have access to it."}}
```

原模型名称 `ernie-4.5` 不是百度千帆 API 的有效 model 入参。

查询[千帆模型列表官方文档](https://cloud.baidu.com/doc/qianfan-docs/s/7m95lyy43)，正确的 model 入参为：

| 模型名称 | model 参数接入点 ID |
|---------|-----------------|
| ERNIE 4.5 Turbo | `ernie-4.5-turbo-128k` |
| ERNIE 4.5 Turbo | `ernie-4.5-turbo-32k` |
| ERNIE Speed | `ernie-speed-pro-128k` |

---

## 四、全部修改清单

### 4.1 `defaults.properties`

```properties
# 修复前
ai_selected_model = ernie:ernie-4.5
ai_ernie_model_list=ernie-4.5

# 修复后
ai_selected_model = ernie|ernie-4.5-turbo-128k
ai_ernie_model_list=ernie-4.5-turbo-128k,ernie-4.5-turbo-32k,ernie-speed-pro-128k
```

### 4.2 `Activator.java`

新增常量并注册 ernie/dashscope key 到 secrets 机制：

```java
private static final String DASHSCOPE_KEY_PROPERTY = "ai_dashscope_key";
private static final String ERNIE_KEY_PROPERTY = "ai_ernie_key";

// markSecretsForSeparatePersistence 中追加：
resourceController.persistPropertyInSecretsFile(DASHSCOPE_KEY_PROPERTY);
resourceController.persistPropertyInSecretsFile(ERNIE_KEY_PROPERTY);
```

### 4.3 `AIProviderConfiguration.java`

1. `getSelectedModelValue()` 增加 fallback 推断逻辑：

```java
public String getSelectedModelValue() {
    String selectedModelValue = getStoredSelectedModelValue();
    if (selectedModelValue != null && !selectedModelValue.trim().isEmpty()) {
        return selectedModelValue;
    }
    // 旧式字段 fallback
    String providerName = resourceController.getProperty(AI_PROVIDER_NAME_PROPERTY);
    String modelName = resourceController.getProperty(AI_MODEL_NAME_PROPERTY);
    if (providerName != null && !providerName.isEmpty()
            && modelName != null && !modelName.isEmpty()) {
        return AIModelSelection.createSelectionValue(providerName, modelName);
    }
    // 最终 fallback：根据已配置的 key 自动推断
    return inferDefaultModelSelection();
}
```

2. 新增 `inferDefaultModelSelection()` 方法，按优先级（ernie > dashscope > openrouter > gemini > ollama）自动推断：

```java
private String inferDefaultModelSelection() {
    if (hasErnieKey()) {
        String models = getErnieModelListValue();
        String modelName = (models != null && !models.trim().isEmpty())
                ? models.split(",")[0].trim() : "ernie-4.5-turbo-128k";
        return AIModelSelection.createSelectionValue("ernie", modelName);
    }
    if (hasDashScopeKey()) {
        String models = getDashScopeModelListValue();
        String modelName = (models != null && !models.trim().isEmpty())
                ? models.split(",")[0].trim() : "qwen-max";
        return AIModelSelection.createSelectionValue("dashscope", modelName);
    }
    // ... openrouter / gemini / ollama
    return null;
}
```

### 4.4 `AIChatModelFactory.java`

ernie service address 处理：自动截断 `/chat/completions` 后缀，默认地址改为 `https://qianfan.baidubce.com/v2`：

```java
if (PROVIDER_NAME_ERNIE.equalsIgnoreCase(providerName)) {
    String serviceAddress = configuration.getErnieServiceAddress();
    if (serviceAddress == null || serviceAddress.isEmpty()) {
        serviceAddress = "https://qianfan.baidubce.com/v2";
    } else if (serviceAddress.endsWith("/chat/completions")) {
        // OpenAI SDK 会自动追加 /chat/completions，需截断用户配置的完整路径
        serviceAddress = serviceAddress.substring(
                0, serviceAddress.length() - "/chat/completions".length());
    }
    return OpenAiChatModel.builder()
            .baseUrl(serviceAddress)
            .apiKey(configuration.getErnieKey())
            .modelName(modelName)
            .maxRetries(CHAT_MODEL_MAX_RETRIES)
            .build();
}
```

### 4.5 `secrets.properties`（用户数据文件）

路径：`%APPDATA%\Freeplane\1.12.x\secrets.properties`

```properties
#Freeplane 1.13.3

ai_ernie_key=bce-v3/ALTAK-AfROKZ9YqhoydcZnJKKFH/8146984583225f5262f7e6b5e20f07221e016a5c
```

---

## 五、验证结果

重新构建部署后，调用接口：

```http
POST http://localhost:6299/api/ai/chat/message
Content-Type: application/json

{"message": "你好，请做个简单的自我介绍"}
```

返回 HTTP 200：

```json
{
  "tokenUsage": {"outputTokens": 0, "inputTokens": 0},
  "reply": "你好！我是一个专注于**函数调用**和**任务处理**的助手，擅长通过组合工具来完成复杂需求..."
}
```

---

## 六、问题链路总结

```
500 AI service not initialized
    └─ isProviderConfigured() 不认识 ernie provider
           └─ [修复] 增加 ernie/dashscope 判断

Missing model selection（第一次）
    └─ getSelectedModelValue() 返回 null
    └─ ernie key 在 secrets 文件读不到（未注册到 persistPropertyInSecretsFile）
    └─ secrets.properties 写入时 key 被换行截断
           └─ [修复] Activator 注册 ernie key；用 WriteAllText 重写 secrets.properties

Missing model selection（第二次，新 jar 生效后）
    └─ defaults.properties 中 ai_selected_model = ernie:ernie-4.5（冒号 vs 管道符）
    └─ AIModelSelection.fromSelectionValue() 找不到 | 返回 null
           └─ [修复] 改为 ai_selected_model = ernie|ernie-4.5-turbo-128k

invalid_model API 错误
    └─ ernie-4.5 不是百度千帆有效的 model 入参
           └─ [修复] 改为 ernie-4.5-turbo-128k（查阅官方模型列表）
```

---

## 七、经验教训

1. **Java Properties 文件续行需显式 `\`**：用 PowerShell `Add-Content` 追加长字符串时，终端自动换行不等于文件换行，需用 `WriteAllText` 精确控制。

2. **Freeplane 版本目录规则**：1.13.x 和 1.12.x 共用同一用户目录（前两位版本号相同），写配置前需确认实际路径。

3. **secrets 机制的完整闭环**：仅在 `secrets.properties` 文件里写入 key 不够，还必须在 `Activator.markSecretsForSeparatePersistence()` 中调用 `persistPropertyInSecretsFile(key)` 完成注册，否则 `getProperty()` 无法正确读取（安全拦截）。

4. **AIModelSelection 分隔符**：`ai_selected_model` 的值必须使用 `|` 分隔 provider 和 model，格式为 `providerName|modelName`。

5. **OpenAI SDK baseUrl 规则**：`OpenAiChatModel` 会自动在 baseUrl 末尾追加 `/chat/completions`，用户配置的地址不应包含该后缀。
