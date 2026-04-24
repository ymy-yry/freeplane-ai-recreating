package org.freeplane.plugin.ai.restapi;

import com.sun.net.httpserver.HttpServer;
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.freeplane.plugin.ai.chat.AIChatPanel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 轻量级 REST API HTTP 服务器，监听端口 6299。
 * 基于 JDK 内置 com.sun.net.httpserver.HttpServer 实现，无需额外依赖。
 * 与现有 MCP Server（6298）并行运行，互不干扰。
 */
public class RestApiServer {

    public static final int PORT = 6299;

    private final HttpServer server;

    public RestApiServer(AvailableMaps availableMaps, AIChatPanel aiChatPanel) throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        RestApiRouter router = new RestApiRouter(availableMaps, aiChatPanel);
        router.registerAll(server);
        server.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void start() {
        server.start();
        LogUtils.info("RestApiServer: started on port " + PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            LogUtils.info("RestApiServer: stopped");
        }
    }
}
