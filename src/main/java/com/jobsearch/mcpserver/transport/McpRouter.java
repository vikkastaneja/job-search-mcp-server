package com.jobsearch.mcpserver.transport;

import com.jobsearch.mcpserver.handlers.BaseHandler;
import com.jobsearch.mcpserver.handlers.ErrorHandler;
import com.jobsearch.mcpserver.handlers.InitializeHandler;
import com.jobsearch.mcpserver.handlers.ToolsListHandler;
import com.jobsearch.mcpserver.protocol.JsonRpcRequest;
import com.jobsearch.mcpserver.util.JsonUtil;

import io.javalin.http.sse.SseClient;

public class McpRouter {
    public static boolean route(String sessionId, String body, SseClient client) {
        JsonRpcRequest request = JsonUtil.fromJson(body, JsonRpcRequest.class);
        System.out.println("Received request: " + request.getMethod() + " with params: " + request.getParams());
        BaseHandler handler = null;
        boolean isNotification = false;
        switch (request.getMethod()) {
            case "initialize":
                handler = new InitializeHandler();
                handler.handle(request, client);
                break;
            case "tools/list":
                handler = new ToolsListHandler();
                handler.handle(request, client);
                break;
            case "notifications/initialized":
                // Handle notifications/initialized if needed
                isNotification = true;
                break;
            default:
                System.out.println("Unknown method: " + request.getMethod());
                handler = new ErrorHandler();
                handler.handle(request, client);
                break;
        }

        return isNotification;
    }
}
