package com.jobsearch.mcpserver;

import com.jobsearch.mcpserver.handlers.SearchJobsHandler;
import com.jobsearch.mcpserver.plugins.AmazonPlugin;
import com.jobsearch.mcpserver.tools.SearchJobsTool;

import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;

import com.jobsearch.mcpserver.transport.McpRouter;
import com.jobsearch.mcpserver.transport.SessionStore;

public class McpServer {
    public static void main(String[] args) throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        AmazonPlugin amazonPlugin = new AmazonPlugin(httpClient);
        SearchJobsTool searchJobsTool = new SearchJobsTool(List.of(amazonPlugin));
        SearchJobsHandler searchHandler = new SearchJobsHandler(searchJobsTool);
        McpRouter router = new McpRouter(searchHandler);
        SessionStore sessionStore = new SessionStore();

        Javalin app = Javalin.create();
        app.before(ctx -> System.out.println("Incoming: " + ctx.method() + " " + ctx.path()));
        // GET /sse endpoint for SSE connections
        app.sse("/sse", client -> {
            System.out.println("New SSE client connected");
            String sessionId = UUID.randomUUID().toString();
            System.out.println("Assigned session ID: " + sessionId);
            
            try {
                client.sendEvent("endpoint", "http://localhost:8080/message?sessionId=" + sessionId);
                System.out.println("Sent initial event with endpoint URL");
            } catch (Exception e) {
                System.out.println("Error sending initial event: " + e.getMessage());
            }

            sessionStore.add(sessionId, client);
            client.onClose(() -> sessionStore.remove(sessionId));
            client.keepAlive();
        });

        // POST /message endpoint for routing MCP messages
        app.post("/message", ctx -> {
            String sessionId = ctx.queryParam("sessionId");
            if (sessionId == null) {
                ctx.status(400);
                return;
            }

            SseClient sseClient = sessionStore.get(sessionId);
            System.out.println("Retrieved SSE client for session ID: " + sessionId + " - " + (sseClient != null ? "found" : "not found"));
            if (sseClient == null) {
                ctx.status(400);
                return;
            }

            boolean isNotification = router.route(sessionId, ctx.body(), sseClient);
            ctx.status(isNotification ? 202 : 200); // 202 Accepted for notifications, 200 OK for regular requests
        });

        app.start(8080);
    }
}