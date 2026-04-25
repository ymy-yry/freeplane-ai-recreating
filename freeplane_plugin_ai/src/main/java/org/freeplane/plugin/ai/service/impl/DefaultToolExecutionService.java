package org.freeplane.plugin.ai.service.impl;

import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.ai.service.ToolExecutionService;
import org.freeplane.plugin.ai.tools.AIToolSet;
import org.freeplane.plugin.ai.tools.edit.EditRequest;
import org.freeplane.plugin.ai.tools.edit.NodeContentEditItem;
import org.freeplane.plugin.ai.tools.create.CreateNodesRequest;
import org.freeplane.plugin.ai.tools.create.AnchorPlacement;
import org.freeplane.plugin.ai.tools.create.NodeCreationItem;
import org.freeplane.plugin.ai.tools.create.NodeFoldingState;
import org.freeplane.plugin.ai.tools.content.NodeContentWriteRequest;
import org.freeplane.plugin.ai.tools.content.AttributeEntry;
import org.freeplane.plugin.ai.tools.read.ReadNodesWithDescendantsRequest;
import org.freeplane.plugin.ai.tools.read.FetchNodesForEditingRequest;
import org.freeplane.plugin.ai.tools.selection.SelectionIdentifiersRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具执行服务实现
 * 提供直接的工具调用能力，绕过 LLM 聊天环节
 */
public class DefaultToolExecutionService implements ToolExecutionService {

    private AIToolSet toolSet;
    private final Map<String, ToolExecutor> toolExecutors;

    public DefaultToolExecutionService() {
        this.toolExecutors = new HashMap<>();
        initializeToolExecutors();
    }

    private void initializeToolExecutors() {
        // 注册工具执行器
        toolExecutors.put("readNodesWithDescendants", this::executeReadNodesWithDescendants);
        toolExecutors.put("fetchNodesForEditing", this::executeFetchNodesForEditing);
        toolExecutors.put("getSelectedMapAndNodeIdentifiers", this::executeGetSelectedMapAndNodeIdentifiers);
        toolExecutors.put("createNodes", this::executeCreateNodes);
        toolExecutors.put("edit", this::executeEdit);
        // 可以添加更多工具执行器
    }

    @Override
    public Object executeTool(String toolName, Map<String, Object> parameters) {
        if (!isToolSupported(toolName)) {
            throw new IllegalArgumentException("Tool not supported: " + toolName);
        }

        if (toolSet == null) {
            throw new IllegalStateException("AIToolSet not initialized");
        }

        ToolExecutor executor = toolExecutors.get(toolName);
        try {
            LogUtils.info("ToolExecutionService: Executing tool " + toolName);
            Object result = executor.execute(parameters);
            LogUtils.info("ToolExecutionService: Tool " + toolName + " executed successfully");
            return result;
        } catch (Exception e) {
            LogUtils.warn("ToolExecutionService: Error executing tool " + toolName, e);
            throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] getSupportedTools() {
        return toolExecutors.keySet().toArray(new String[0]);
    }

    @Override
    public boolean isToolSupported(String toolName) {
        return toolExecutors.containsKey(toolName);
    }

    @Override
    public void setToolSet(AIToolSet toolSet) {
        this.toolSet = toolSet;
    }

    @Override
    public AIToolSet getToolSet() {
        return toolSet;
    }

    // 工具执行器接口
    @FunctionalInterface
    private interface ToolExecutor {
        Object execute(Map<String, Object> parameters) throws Exception;
    }

    // 工具执行实现
    private Object executeReadNodesWithDescendants(Map<String, Object> parameters) {
        String mapIdentifier = (String) parameters.get("mapIdentifier");
        List<String> nodeIdentifiers = (List<String>) parameters.get("nodeIdentifiers");
        List<String> contextSectionsStr = (List<String>) parameters.get("contextSections");
        Integer fullContentDepth = (Integer) parameters.get("fullContentDepth");
        Integer summaryDepth = (Integer) parameters.get("summaryDepth");
        Integer maximumTotalTextCharacters = (Integer) parameters.get("maximumTotalTextCharacters");
        
        // 转换 contextSections 为枚举类型
        List<org.freeplane.plugin.ai.tools.read.ContextSection> contextSections = null;
        if (contextSectionsStr != null) {
            contextSections = contextSectionsStr.stream()
                    .map(section -> org.freeplane.plugin.ai.tools.read.ContextSection.valueOf(section))
                    .collect(Collectors.toList());
        }
        
        ReadNodesWithDescendantsRequest request = new ReadNodesWithDescendantsRequest(
                mapIdentifier,
                nodeIdentifiers,
                contextSections,
                fullContentDepth,
                summaryDepth,
                maximumTotalTextCharacters
        );
        return toolSet.readNodesWithDescendants(request);
    }

    private Object executeFetchNodesForEditing(Map<String, Object> parameters) {
        String mapIdentifier = (String) parameters.get("mapIdentifier");
        List<String> nodeIdentifiers = (List<String>) parameters.get("nodeIdentifiers");
        List<String> editableContentFields = (List<String>) parameters.get("editableContentFields");
        
        // 转换 editableContentFields 为枚举类型
        List<org.freeplane.plugin.ai.tools.content.EditableContentField> fields = editableContentFields.stream()
                .map(field -> org.freeplane.plugin.ai.tools.content.EditableContentField.valueOf(field))
                .collect(Collectors.toList());
        
        FetchNodesForEditingRequest request = new FetchNodesForEditingRequest(mapIdentifier, nodeIdentifiers, fields);
        return toolSet.fetchNodesForEditing(request);
    }

    private Object executeGetSelectedMapAndNodeIdentifiers(Map<String, Object> parameters) {
        String selectionModeStr = (String) parameters.get("selectionCollectionMode");
        org.freeplane.plugin.ai.tools.selection.SelectionCollectionMode selectionMode = null;
        if (selectionModeStr != null) {
            selectionMode = org.freeplane.plugin.ai.tools.selection.SelectionCollectionMode.valueOf(selectionModeStr);
        }
        SelectionIdentifiersRequest request = new SelectionIdentifiersRequest(selectionMode);
        return toolSet.getSelectedMapAndNodeIdentifiers(request);
    }

    private Object executeCreateNodes(Map<String, Object> parameters) {
        String mapIdentifier = (String) parameters.get("mapIdentifier");
        String userSummary = (String) parameters.get("userSummary");
        
        // 解析 anchorPlacement
        Map<String, Object> anchorPlacementMap = (Map<String, Object>) parameters.get("anchorPlacement");
        String anchorNodeIdentifier = (String) anchorPlacementMap.get("anchorNodeIdentifier");
        String placementMode = (String) anchorPlacementMap.get("placementMode");
        AnchorPlacement anchorPlacement = new AnchorPlacement(
                anchorNodeIdentifier,
                org.freeplane.plugin.ai.tools.create.AnchorPlacementMode.valueOf(placementMode)
        );
        
        // 解析 nodes
        List<Map<String, Object>> nodesMap = (List<Map<String, Object>>) parameters.get("nodes");
        List<NodeCreationItem> nodes = new ArrayList<>();
        for (int i = 0; i < nodesMap.size(); i++) {
            Map<String, Object> nodeMap = nodesMap.get(i);
            
            // 解析 content
            Map<String, Object> contentMap = (Map<String, Object>) nodeMap.get("content");
            NodeContentWriteRequest content = null;
            if (contentMap != null) {
                content = new NodeContentWriteRequest(
                        (String) contentMap.get("text"),
                        contentMap.containsKey("textContentType") ? 
                                org.freeplane.plugin.ai.tools.content.ContentType.valueOf((String) contentMap.get("textContentType")) : null,
                        (String) contentMap.get("details"),
                        contentMap.containsKey("detailsContentType") ? 
                                org.freeplane.plugin.ai.tools.content.ContentType.valueOf((String) contentMap.get("detailsContentType")) : null,
                        (String) contentMap.get("note"),
                        contentMap.containsKey("noteContentType") ? 
                                org.freeplane.plugin.ai.tools.content.ContentType.valueOf((String) contentMap.get("noteContentType")) : null,
                        null, // attributes
                        (List<String>) contentMap.get("tags"),
                        (List<String>) contentMap.get("icons"),
                        (String) contentMap.get("hyperlink")
                );
            }
            
            NodeCreationItem node = new NodeCreationItem(
                    (Integer) nodeMap.get("index"),
                    (Integer) nodeMap.get("parentIndex"),
                    content,
                    nodeMap.containsKey("foldingState") ? 
                            org.freeplane.plugin.ai.tools.create.NodeFoldingState.valueOf((String) nodeMap.get("foldingState")) : null,
                    (String) nodeMap.get("mainStyle")
            );
            nodes.add(node);
        }
        
        CreateNodesRequest request = new CreateNodesRequest(mapIdentifier, userSummary, anchorPlacement, nodes);
        return toolSet.createNodes(request);
    }

    private Object executeEdit(Map<String, Object> parameters) {
        String mapIdentifier = (String) parameters.get("mapIdentifier");
        String userSummary = (String) parameters.get("userSummary");
        
        // 解析 items
        List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) parameters.get("items");
        List<NodeContentEditItem> items = new ArrayList<>();
        for (Map<String, Object> itemMap : itemsMap) {
            NodeContentEditItem item = new NodeContentEditItem(
                    (String) itemMap.get("nodeIdentifier"),
                    org.freeplane.plugin.ai.tools.edit.EditedElement.valueOf((String) itemMap.get("editedElement")),
                    itemMap.containsKey("originalContentType") ? 
                            org.freeplane.plugin.ai.tools.content.ContentType.valueOf((String) itemMap.get("originalContentType")) : null,
                    (String) itemMap.get("value"),
                    (Integer) itemMap.get("index"),
                    itemMap.containsKey("operation") ? 
                            org.freeplane.plugin.ai.tools.edit.EditOperation.valueOf((String) itemMap.get("operation")) : null,
                    (String) itemMap.get("targetKey")
            );
            items.add(item);
        }
        
        EditRequest request = new EditRequest(mapIdentifier, userSummary, items);
        return toolSet.edit(request);
    }
}