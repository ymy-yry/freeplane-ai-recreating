package org.freeplane.plugin.ai.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.ai.maps.AvailableMaps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 节点操作接口控制器。
 * 负责处理 /api/nodes/* 路径下的增、删、改、查请求。
 * 所有写操作均通过 Freeplane 的 MMapController 执行，保证与 Swing UI 状态一致。
 */
public class NodeRestController {

    private final AvailableMaps availableMaps;
    private final ObjectMapper objectMapper;

    public NodeRestController(AvailableMaps availableMaps) {
        this.availableMaps = availableMaps;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * POST /api/nodes/search
     * 关键词搜索节点，在当前导图中遍历所有节点文本进行匹配。
     */
    public void handleSearch(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String query = (String) body.get("query");
            boolean caseSensitive = Boolean.TRUE.equals(body.get("caseSensitive"));

            MapModel mapModel = resolveMapModel(body);
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }

            List<Map<String, Object>> results = new ArrayList<>();
            searchNodes(mapModel.getRootNode(), query, caseSensitive, results, "");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("results", results);
            response.put("totalCount", results.size());
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("NodeRestController.handleSearch error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/nodes/create
     * 在指定父节点下新建子节点。通过 Freeplane MMapController 在 EDT 中执行。
     */
    public void handleCreate(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String parentId = (String) body.get("parentId");
            String text = (String) body.get("text");

            if (parentId == null || text == null) {
                sendError(exchange, 400, "parentId and text are required");
                return;
            }

            MapModel mapModel = resolveMapModel(body);
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }

            NodeModel parentNode = mapModel.getNodeForID(parentId);
            if (parentNode == null) {
                sendError(exchange, 404, "Parent node not found: " + parentId);
                return;
            }

            // 使用 MMapController 在 Swing EDT 中创建节点，确保 UI 同步刷新
            MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
            final String finalText = text;
            NodeModel newNode = mapController.addNewNode(parentNode, parentNode.getChildCount(),
                node -> node.setText(finalText));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("nodeId", newNode.getID());
            response.put("text", text);
            response.put("parentId", parentId);
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("NodeRestController.handleCreate error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/nodes/edit
     * 修改节点文本内容。
     */
    public void handleEdit(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String nodeId = (String) body.get("nodeId");
            String text = (String) body.get("text");

            if (nodeId == null || text == null) {
                sendError(exchange, 400, "nodeId and text are required");
                return;
            }

            MapModel mapModel = resolveMapModel(body);
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }

            NodeModel node = mapModel.getNodeForID(nodeId);
            if (node == null) {
                sendError(exchange, 404, "Node not found: " + nodeId);
                return;
            }

            MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
            final String finalText2 = text;
            node.setText(finalText2);
            mapController.nodeChanged(node);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("nodeId", nodeId);
            response.put("text", text);
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("NodeRestController.handleEdit error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/nodes/toggle-fold
     * 切换节点的折叠/展开状态。
     */
    public void handleToggleFold(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String nodeId = (String) body.get("nodeId");
            Boolean folded = (Boolean) body.get("folded");

            if (nodeId == null || folded == null) {
                sendError(exchange, 400, "nodeId and folded are required");
                return;
            }

            MapModel mapModel = resolveMapModel(body);
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }

            NodeModel node = mapModel.getNodeForID(nodeId);
            if (node == null) {
                sendError(exchange, 404, "Node not found: " + nodeId);
                return;
            }

            MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
            org.freeplane.features.filter.Filter filter = Controller.getCurrentController().getSelection().getFilter();
            mapController.setFolded(node, folded.booleanValue(), filter);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("nodeId", nodeId);
            response.put("folded", folded);
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("NodeRestController.handleToggleFold error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * POST /api/nodes/delete
     * 删除指定节点（不能删除根节点）。
     */
    public void handleDelete(HttpExchange exchange) throws IOException {
        CorsFilter.addCorsHeaders(exchange);
        if (CorsFilter.handlePreflight(exchange)) return;

        try {
            Map<?, ?> body = readBody(exchange);
            String nodeId = (String) body.get("nodeId");

            if (nodeId == null) {
                sendError(exchange, 400, "nodeId is required");
                return;
            }

            MapModel mapModel = resolveMapModel(body);
            if (mapModel == null) {
                sendError(exchange, 404, "No map is currently open");
                return;
            }

            NodeModel node = mapModel.getNodeForID(nodeId);
            if (node == null) {
                sendError(exchange, 404, "Node not found: " + nodeId);
                return;
            }

            if (node.isRoot()) {
                sendError(exchange, 400, "Cannot delete root node");
                return;
            }

            MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
            mapController.deleteNode(node);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("deleted", true);
            response.put("nodeId", nodeId);
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            LogUtils.warn("NodeRestController.handleDelete error", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 内部工具方法
    // ──────────────────────────────────────────────

    /**
     * 根据请求体中的 mapId 字段解析目标导图。
     * 若 mapId 存在且有效，则查找对应导图；否则回退到当前激活的导图。
     */
    private MapModel resolveMapModel(Map<?, ?> body) {
        Object mapIdObj = body.get("mapId");
        if (mapIdObj instanceof String) {
            String mapIdStr = (String) mapIdObj;
            try {
                UUID mapId = UUID.fromString(mapIdStr);
                MapModel found = availableMaps.findMapModel(mapId);
                if (found != null) {
                    return found;
                }
            } catch (IllegalArgumentException ignored) {
                // mapId 格式无效，回退到当前导图
            }
        }
        return availableMaps.getCurrentMapModel();
    }

    private void searchNodes(NodeModel node, String query, boolean caseSensitive,
                             List<Map<String, Object>> results, String path) {
        if (node == null) return;
        String text = node.getText();
        String searchTarget = caseSensitive ? text : text.toLowerCase();
        String searchQuery = caseSensitive ? query : query.toLowerCase();

        if (searchTarget.contains(searchQuery)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nodeId", node.getID());
            item.put("text", text);
            item.put("path", path.isEmpty() ? text : path + " > " + text);
            results.add(item);
        }

        String currentPath = path.isEmpty() ? text : path + " > " + text;
        for (NodeModel child : node.getChildren()) {
            searchNodes(child, query, caseSensitive, results, currentPath);
        }
    }

    Map<?, ?> readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, Map.class);
        }
    }

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
