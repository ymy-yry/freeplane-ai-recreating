package org.freeplane.plugin.ai.mcpserver;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonReferenceSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.freeplane.core.util.LogUtils;

public class ModelContextProtocolToolRegistry {
    private static final Map<String, Object> EMPTY_SCHEMA = createEmptySchema();

    private final Object toolSet;
    private final ObjectMapper objectMapper;

    /**
     * 方案B：Schema懒加载缓存。
     * volatile 保证多线程可见性，防止指令重排序导致拿到半初始化对象。
     */
    private volatile List<ModelContextProtocolTool> cachedTools = null;

    public ModelContextProtocolToolRegistry(Object toolSet, ObjectMapper objectMapper) {
        this.toolSet = Objects.requireNonNull(toolSet, "toolSet");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * 列出所有已注册工具的 Schema 列表。
     * 采用双重检查锁（Double-Checked Locking）实现懒加载：
     *   - 冷路径（首次调用）：触发反射扫描 + 递归展开 Schema，结果写入 cachedTools
     *   - 热路径（后续调用）：第一重检查直接返回，O(1)且无锁竞争
     */
    public List<ModelContextProtocolTool> listTools() {
        // 第一重检查：不加锁，99%情况下缓存已就绪，直接返回
        if (cachedTools == null) {
            synchronized (this) {
                // 第二重检查：加锁后再判断一次，防止多线程同时通过第一重检查导致重复构建
                if (cachedTools == null) {
                    LogUtils.info("ModelContextProtocolToolRegistry: building tool schema cache");
                    cachedTools = buildToolList();
                    LogUtils.info("ModelContextProtocolToolRegistry: cached " + cachedTools.size() + " tool schemas");
                }
            }
        }
        return cachedTools;
    }

    /**
     * 使缓存失效。
     * 当动态工具注册发生变化时调用，下次 listTools() 将重新构建 Schema 列表。
     */
    public void invalidateCache() {
        synchronized (this) {
            cachedTools = null;
        }
        LogUtils.info("ModelContextProtocolToolRegistry: tool schema cache invalidated");
    }

    /**
     * 内部构建方法：执行反射扫描 + 递归 Schema 展开。
     * 只在缓存未命中或失效时被调用。
     */
    private List<ModelContextProtocolTool> buildToolList() {
        List<ToolSpecification> specifications = ToolSpecifications.toolSpecificationsFrom(toolSet);
        List<ModelContextProtocolTool> tools = new ArrayList<>(specifications.size());
        for (ToolSpecification specification : specifications) {
            Map<String, Object> inputSchema = EMPTY_SCHEMA;
            JsonObjectSchema parameters = specification.parameters();
            if (parameters != null) {
                inputSchema = jsonSchemaElementToMap(parameters);
            }
            tools.add(new ModelContextProtocolTool(
                specification.name(),
                specification.description(),
                inputSchema
            ));
        }
        return Collections.unmodifiableList(tools);
    }

    private Map<String, Object> jsonSchemaElementToMap(JsonSchemaElement schemaElement) {
        if (schemaElement instanceof JsonObjectSchema jsonObjectSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "object");
            if (jsonObjectSchema.description() != null) {
                map.put("description", jsonObjectSchema.description());
            }
            Map<String, Object> properties = new LinkedHashMap<>();
            for (Map.Entry<String, JsonSchemaElement> entry : jsonObjectSchema.properties().entrySet()) {
                properties.put(entry.getKey(), jsonSchemaElementToMap(entry.getValue()));
            }
            map.put("properties", properties);
            if (jsonObjectSchema.required() != null) {
                map.put("required", jsonObjectSchema.required());
            }
            if (jsonObjectSchema.additionalProperties() != null) {
                map.put("additionalProperties", jsonObjectSchema.additionalProperties());
            }
            Map<String, JsonSchemaElement> definitions = jsonObjectSchema.definitions();
            if (definitions != null && !definitions.isEmpty()) {
                Map<String, Object> defs = new LinkedHashMap<>();
                for (Map.Entry<String, JsonSchemaElement> entry : definitions.entrySet()) {
                    defs.put(entry.getKey(), jsonSchemaElementToMap(entry.getValue()));
                }
                map.put("$defs", defs);
            }
            return map;
        }
        if (schemaElement instanceof JsonArraySchema jsonArraySchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "array");
            if (jsonArraySchema.description() != null) {
                map.put("description", jsonArraySchema.description());
            }
            JsonSchemaElement items = jsonArraySchema.items();
            if (items != null) {
                map.put("items", jsonSchemaElementToMap(items));
            }
            return map;
        }
        if (schemaElement instanceof JsonEnumSchema jsonEnumSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "string");
            map.put("enum", jsonEnumSchema.enumValues());
            if (jsonEnumSchema.description() != null) {
                map.put("description", jsonEnumSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonStringSchema jsonStringSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "string");
            if (jsonStringSchema.description() != null) {
                map.put("description", jsonStringSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonIntegerSchema jsonIntegerSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "integer");
            if (jsonIntegerSchema.description() != null) {
                map.put("description", jsonIntegerSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonNumberSchema jsonNumberSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "number");
            if (jsonNumberSchema.description() != null) {
                map.put("description", jsonNumberSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonBooleanSchema jsonBooleanSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "boolean");
            if (jsonBooleanSchema.description() != null) {
                map.put("description", jsonBooleanSchema.description());
            }
            return map;
        }
        if (schemaElement instanceof JsonReferenceSchema jsonReferenceSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("$ref", "#/$defs/" + jsonReferenceSchema.reference());
            return map;
        }
        if (schemaElement instanceof JsonAnyOfSchema jsonAnyOfSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            if (jsonAnyOfSchema.description() != null) {
                map.put("description", jsonAnyOfSchema.description());
            }
            List<Map<String, Object>> anyOf = new ArrayList<>(jsonAnyOfSchema.anyOf().size());
            for (JsonSchemaElement element : jsonAnyOfSchema.anyOf()) {
                anyOf.add(jsonSchemaElementToMap(element));
            }
            map.put("anyOf", anyOf);
            return map;
        }
        if (schemaElement instanceof JsonNullSchema) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "null");
            return map;
        }
        if (schemaElement instanceof JsonRawSchema jsonRawSchema) {
            return parseRawSchema(jsonRawSchema.schema());
        }
        throw new IllegalArgumentException("Unsupported schema element: " + schemaElement.getClass());
    }

    private Map<String, Object> parseRawSchema(String schema) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(schema, Map.class);
            return map;
        } catch (IOException error) {
            throw new IllegalArgumentException("Invalid raw schema JSON.", error);
        }
    }

    private static Map<String, Object> createEmptySchema() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "object");
        map.put("properties", Collections.emptyMap());
        map.put("required", Collections.emptyList());
        return Collections.unmodifiableMap(map);
    }
}
