package com.jobsearch.mcpserver.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobsearch.mcpserver.protocol.JsonRpcRequest;

public class ErrorHandler extends BaseHandler {
    @Override
    protected String getResponse(JsonRpcRequest request) {
        int requestId = request.getId() instanceof Integer ? (Integer) request.getId() : 1;
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectMapper mapper = new ObjectMapper();

        // 1. Build "error": { "code": -32601, "message": "Method not found" }
        ObjectNode error = factory.objectNode()
                .put("code", -32601)
                .put("message", "Method not found");

        // 2. Build the root JSON object
        ObjectNode root = factory.objectNode()
                .put("jsonrpc", "2.0");
        
        // JSON-RPC specification allows 'id' to be null in error scenarios
        root.put("id", requestId);
        
        
        root.set("error", error);

        // 3. Serialize to String (Pretty Printed)
        String jsonString = null;
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println(jsonString);
        return jsonString;
    }
    
}
