package org.freeplane.plugin.ai.restapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.ai.chat.AIChatPanel;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.maps.MapModelProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RestApiRouterIntegrationTest {
    private HttpServer server;
    private String serverUrl;
    private Controller originalController;

    @Before
    public void setUp() throws Exception {
        originalController = Controller.getCurrentController();
        Controller mockController = mock(Controller.class);
        ResourceController resourceController = mock(ResourceController.class);
        when(mockController.getResourceController()).thenReturn(resourceController);
        when(resourceController.getProperty("ai_openrouter_key")).thenReturn("");
        when(resourceController.getProperty("ai_gemini_key")).thenReturn("");
        when(resourceController.getProperty("ai_dashscope_key")).thenReturn("");
        when(resourceController.getProperty("ai_ernie_key")).thenReturn("");
        when(resourceController.getProperty("ai_ollama_service_address")).thenReturn("");
        Controller.setCurrentController(mockController);

        MapModelProvider mapModelProvider = mock(MapModelProvider.class);
        when(mapModelProvider.getCurrentMapModel()).thenReturn(null);
        when(mapModelProvider.getOpenMapModels()).thenReturn(Collections.emptyList());
        when(mapModelProvider.getCurrentSelectedNodeModel()).thenReturn(null);

        AvailableMaps availableMaps = new AvailableMaps(mapModelProvider);
        RestApiRouter router = new RestApiRouter(availableMaps, mock(AIChatPanel.class));

        int port = findAvailablePort();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        router.registerAll(server);
        server.start();
        serverUrl = "http://127.0.0.1:" + port;
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        Controller.setCurrentController(originalController);
    }

    @Test
    public void getCurrentMap_withoutOpenMap_returnsNotFound() throws Exception {
        Response response = sendRequest("GET", "/api/map/current", null);

        assertThat(response.status).isEqualTo(404);
        assertThat(response.body).contains("No map is currently open in Freeplane");
    }

    @Test
    public void optionsChatMessage_returnsNoContentAndCorsHeaders() throws Exception {
        Response response = sendRequest("OPTIONS", "/api/ai/chat/message", null);

        assertThat(response.status).isEqualTo(204);
        assertThat(response.header("Access-Control-Allow-Origin")).isEqualTo("*");
        assertThat(response.header("Access-Control-Allow-Methods")).contains("GET");
        assertThat(response.header("Access-Control-Allow-Methods")).contains("POST");
        assertThat(response.header("Access-Control-Allow-Headers")).contains("Content-Type");
    }

    @Test
    public void postChatMessage_withoutMessage_returnsBadRequest() throws Exception {
        Response response = sendRequest("POST", "/api/ai/chat/message", "{}");

        assertThat(response.status).isEqualTo(400);
        assertThat(response.body).contains("message is required");
    }

    @Test
    public void postExpandNode_withoutNodeId_returnsBadRequest() throws Exception {
        Response response = sendRequest("POST", "/api/ai/build/expand-node", "{}");

        assertThat(response.status).isEqualTo(400);
        assertThat(response.body).contains("nodeId is required");
    }

    @Test
    public void postChatMessage_withoutConfiguredProvider_returnsFriendlyServerError() throws Exception {
        Response response = sendRequest("POST", "/api/ai/chat/message", "{\"message\":\"hello\"}");

        assertThat(response.status).isEqualTo(500);
        assertThat(response.body).contains("AI service not initialized. Please configure AI provider in preferences.");
    }

    @Test
    public void postExpandNode_withoutConfiguredProvider_returnsFriendlyServerError() throws Exception {
        Response response = sendRequest("POST", "/api/ai/build/expand-node", "{\"nodeId\":\"NODE_1\"}");

        assertThat(response.status).isEqualTo(500);
        assertThat(response.body).contains("AI service not initialized. Please configure AI provider in preferences.");
    }

    @Test
    public void postSummarize_withoutConfiguredProvider_returnsFriendlyServerError() throws Exception {
        Response response = sendRequest("POST", "/api/ai/build/summarize", "{\"nodeId\":\"NODE_1\"}");

        assertThat(response.status).isEqualTo(500);
        assertThat(response.body).contains("AI service not initialized. Please configure AI provider in preferences.");
    }

    @Test
    public void postTag_withoutConfiguredProvider_returnsFriendlyServerError() throws Exception {
        Response response = sendRequest("POST", "/api/ai/build/tag", "{\"nodeIds\":[\"NODE_1\"]}");

        assertThat(response.status).isEqualTo(500);
        assertThat(response.body).contains("AI service not initialized. Please configure AI provider in preferences.");
    }

    @Test
    public void postCreateNode_withoutRequiredFields_returnsBadRequest() throws Exception {
        Response response = sendRequest("POST", "/api/nodes/create", "{}");

        assertThat(response.status).isEqualTo(400);
        assertThat(response.body).contains("parentId and text are required");
    }

    @Test
    public void requestToUnknownRoute_returnsNotFound() throws Exception {
        Response response = sendRequest("GET", "/api/ai/unknown", null);

        assertThat(response.status).isEqualTo(404);
        assertThat(response.body).contains("Not found");
    }

    private Response sendRequest(String method, String path, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl + path).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoInput(true);

        if (body != null) {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload);
            }
        }

        int status = connection.getResponseCode();
        byte[] responseBody = readResponseBody(connection);
        Map<String, List<String>> headers = connection.getHeaderFields();
        connection.disconnect();
        return new Response(status, responseBody, headers);
    }

    private byte[] readResponseBody(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getResponseCode() >= 400
            ? connection.getErrorStream()
            : connection.getInputStream();
        if (inputStream == null) {
            return new byte[0];
        }
        try (InputStream responseStream = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = responseStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private int findAvailablePort() throws IOException {
        int port = 0;
        while (port < 1024) {
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
        }
        return port;
    }

    private static class Response {
        private final int status;
        private final String body;
        private final Map<String, List<String>> headers;

        private Response(int status, byte[] bodyBytes, Map<String, List<String>> headers) {
            this.status = status;
            this.body = bodyBytes == null ? "" : new String(bodyBytes, StandardCharsets.UTF_8);
            this.headers = headers;
        }

        private String header(String name) {
            for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
                String headerName = headerEntry.getKey();
                if (headerName != null && headerName.equalsIgnoreCase(name)) {
                    List<String> values = headerEntry.getValue();
                    return values == null || values.isEmpty() ? "" : values.get(0);
                }
            }
            return "";
        }
    }
}
