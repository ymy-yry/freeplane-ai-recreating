package org.freeplane.plugin.ai.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 导图数据接口控制器。
 * 负责处理 /api/map/* 路径下的所有请求。
 * 主要将 Freeplane 内存中的 MapModel/NodeModel 对象序列化为 JSON 返回给前端。
 */
public class MapRestController {

    private final AvailableMaps availableMaps;
    private final ObjectMapper objectMapper;

    public MapRestController(AvailableMaps availableMaps) {
        this.availableMaps = availableMaps;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * GET /api/map/current
     * 返回当前 Freeplane 中打开的导图节点树（JSON 格式）。
     */
    public void handleCurrentMap(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            MapModel mapModel = availableMaps.getCurrentMapModel();
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open in Freeplane");
                return;
            }

            UUID mapId = availableMaps.getCurrentMapIdentifier();
            NodeModel rootNode = mapModel.getRootNode();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mapId", mapId != null ? mapId.toString() : "unknown");
            result.put("title", rootNode != null ? rootNode.getText() : "");
            result.put("root", rootNode != null ? serializeNode(rootNode) : null);

            sendJson(exchange, 200, result);
        } catch (Exception e) {
            LogUtils.warn("MapRestController.handleCurrentMap error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * GET /api/nodes/{nodeId}
     * 返回单个节点的详细信息（含属性、备注、子节点列表）。
     */
    public void handleGetNode(HttpExchange exchange, String nodeId) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            MapModel mapModel = availableMaps.getCurrentMapModel();
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }

            NodeModel node = mapModel.getNodeForID(nodeId);
            if (node == null) {
                sendError(exchange, 404, "Node not found: " + nodeId);
                return;
            }

            sendJson(exchange, 200, serializeNodeDetail(node));
        } catch (Exception e) {
            LogUtils.warn("MapRestController.handleGetNode error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * 将节点递归序列化为 Map（用于 JSON 输出），包含完整子节点树。
     * 字段与前端 types/mindmap.ts 中 MindMapNode 接口完全对应。
     */
    private Map<String, Object> serializeNode(NodeModel node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.getID());
        map.put("text", node.getText());
        map.put("parentId", node.getParentNode() != null ? node.getParentNode().getID() : null);
        map.put("folded", node.isFolded());
        map.put("note", ""); // Note 字段需要 NoteModel 扩展，此处简化

        List<Map<String, Object>> children = new ArrayList<>();
        for (NodeModel child : node.getChildren()) {
            children.add(serializeNode(child));
        }
        map.put("children", children);
        return map;
    }

    /**
     * 序列化节点详情（含属性键值对）。
     */
    private Map<String, Object> serializeNodeDetail(NodeModel node) {
        Map<String, Object> map = serializeNode(node);
        // 属性字段（attributes）暂返回空数组，后续可扩展读取 NodeAttributeTableModel
        map.put("attributes", new ArrayList<>());
        return map;
    }

    // ──────────────────────────────────────────────
    // 通用工具方法
    // ──────────────────────────────────────────────

    void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", message);
        sendJson(exchange, statusCode, error);
    }
}
