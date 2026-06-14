package com.jobsearch.mcpserver.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobsearch.mcpserver.protocol.JsonRpcRequest;


public class InitializeHandler extends BaseHandler {
    
    @Override
    protected String getResponse(JsonRpcRequest request) {
        // This function creates a json string that looks as the following:
        /*
        {
            "jsonrpc": "2.0",
            "id": <request id>,
            "result": {
            "protocolVersion": "2024-11-05",
            "capabilities": { "tools": {} },
            "serverInfo": { "name": "job-search-mcp", "version": "1.0.0" }
        }
        */
        // Define your dynamic request ID (can be an Integer or String)
        int requestId = request.getId() instanceof Integer ? (Integer) request.getId() : 1; // Default to 1 if not provided 

        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectMapper mapper = new ObjectMapper();

        // 1. Build "capabilities": { "tools": {} }
        ObjectNode capabilities = factory.objectNode()
                .set("tools", factory.objectNode());

        // 2. Build "serverInfo": { "name": "job-search-mcp", "version": "1.0.0" }
        ObjectNode serverInfo = factory.objectNode()
                .put("name", "job-search-mcp")
                .put("version", "1.0.0");

        // 3. Build "result": { ... } and nest capabilities & serverInfo
        ObjectNode result = factory.objectNode()
                .put("protocolVersion", "2024-11-05");
        result.set("capabilities", capabilities);
        result.set("serverInfo", serverInfo);

        // 4. Build the root JSON object
        ObjectNode root = factory.objectNode()
                .put("jsonrpc", "2.0")
                .put("id", requestId); // Handles numbers natively
        root.set("result", result);

        // 5. Serialize to String (Pretty Printed)
        String jsonString = "";
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println(jsonString);
        return jsonString;
    }
}
