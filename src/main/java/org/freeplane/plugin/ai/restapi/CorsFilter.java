package org.freeplane.plugin.ai.restapi;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * 跨域处理工具类。
 * 浏览器的同源策略会阻止 Vue 前端（localhost:5173）访问后端（localhost:6299）。
 * 通过在响应头中添加 CORS 相关字段，告知浏览器允许跨域访问。
 */
public class CorsFilter {

    /**
     * 为所有响应添加 CORS 响应头，允许来自任意来源的请求。
     */
    public static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
    }

    /**
     * 处理浏览器的 OPTIONS 预检请求（Preflight）。
     * 在发送实际请求前，浏览器会先发一个 OPTIONS 请求询问服务器是否允许跨域。
     *
     * @return true 表示是 OPTIONS 请求，已处理完毕，调用方应直接返回
     */
    public static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}
