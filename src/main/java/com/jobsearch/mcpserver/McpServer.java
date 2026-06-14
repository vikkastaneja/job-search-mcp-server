package com.jobsearch.mcpserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsearch.mcpserver.plugins.AmazonJobSearchService;
import com.jobsearch.mcpserver.plugins.JobListing;
import com.jobsearch.mcpserver.protocol.JsonRpcRequest;
import com.jobsearch.mcpserver.protocol.JsonRpcResponse;

import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;

import java.util.List;
import java.util.UUID;

import com.jobsearch.mcpserver.transport.McpRouter;
import com.jobsearch.mcpserver.transport.SessionStore;
import com.jobsearch.mcpserver.util.AmazonJobSearchUrlBuilder;

public class McpServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Mcp server running...");
        String req1 = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"searchJobs\",\"params\":{\"query\":\"software engineer\"}}";
        ObjectMapper mapper = new ObjectMapper();
        JsonRpcRequest parsed = mapper.readValue(req1, JsonRpcRequest.class);
        System.out.println(parsed.getId()); // should be 1
        System.out.println(parsed.getMethod()); // should be "searchJobs "
        System.out.println(parsed.getParams().get("query").asText()); // should be "software engineer"

        // Test 2: parse a notification (no id)
        String req2 = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
        JsonRpcRequest notif = mapper.readValue(req2, JsonRpcRequest.class);
        System.out.println(notif.getId()); // should be null

        // Test 3: serialize a response
        JsonRpcResponse resp = new JsonRpcResponse();
        resp.setJsonrpc("2.0");
        resp.setId(1);
        resp.setResult(mapper.createObjectNode().put("status","ok"));
        System.out.println(mapper.writeValueAsString(resp));

        Javalin app = Javalin.create();

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

            SessionStore.add(sessionId, client);
            client.onClose(() -> SessionStore.remove(sessionId));
            client.keepAlive();
        });

        // POST /message endpoint for routing MCP messages
        app.post("/message", ctx -> {
            String sessionId = ctx.queryParam("sessionId");
            if (sessionId == null) {
                ctx.status(400);
                return;
            }

            SseClient sseClient = SessionStore.get(sessionId);
            System.out.println("Retrieved SSE client for session ID: " + sessionId + " - " + (sseClient != null ? "found" : "not found"));
            if (sseClient == null) {
                ctx.status(400);
                return;
            }

            boolean isNotification = McpRouter.route(sessionId, ctx.body(), sseClient);
            ctx.status(isNotification ? 202 : 200); // 202 Accepted for notifications, 200 OK for regular requests
        });

        //app.start(8080);

        // 1. Generate the exact default URL you provided
        AmazonJobSearchUrlBuilder defaultSearch = new AmazonJobSearchUrlBuilder();
        System.out.println("Default URL:");
        System.out.println(defaultSearch.build());
        System.out.println();

        // 2. Generate a custom URL modifying only a few parameters (e.g., pagination or role)
        String customUrl = new AmazonJobSearchUrlBuilder()
                .withBaseQuery("Senior Software Engineer")
                .withOffset(25) // Next page
                .withResultLimit(50)
                .build();

        System.out.println("Customized URL:");
        System.out.println(customUrl);


        AmazonJobSearchService service = new AmazonJobSearchService();
        
        System.out.println("Initiating HTTP connection context to Amazon Jobs API platform...");
        List<JobListing> activeJobs = service.fetchSeattleSoftwareManagerJobs();
        
        System.out.println("\n--- Discovery Processing Loop Finished ---");
        System.out.println("Total mapped domain instances loaded: " + activeJobs.size());
        System.out.println("------------------------------------------");
        
        // Output clean structured results extracted straight from the target JSON payload arrays
        activeJobs.stream().limit(3).forEach(job -> {
            System.out.println("Role Title : " + job.getTitle());
            System.out.println("Company    : " + job.getCompany());
            System.out.println("Location   : " + job.getLocation());
            System.out.println("Direct link: " + job.getUrl());
            System.out.println("Source Node: " + job.getSource());
            System.out.println();
        });
    }
}