package com.jobsearch.mcpserver.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobsearch.mcpserver.protocol.JsonRpcRequest;

public class ToolsListHandler extends BaseHandler {
    @Override
    protected String getResponse(JsonRpcRequest request) {
        /*
        {
            "jsonrpc" : "2.0",
            "id" : 123,
            "result" : {
                "tools" : [ {
                    "name" : "search_jobs",
                    "description" : "Searches for jobs matching a query across multiple job boards. Returns raw listings — Claude should filter for relevance.",
                        "inputSchema" : {
                            "type" : "object",
                            "properties" : {
                                "query" : {
                                    "type" : "string",
                                    "description" : "Job title or keywords to search for"
                            }
                            },
                            "required" : [ "query" ]
                        }
                } ]
            }
            }
         */
        int requestId = request.getId() instanceof Integer ? (Integer) request.getId() : 1; // Default to 1 if not provided 

        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectMapper mapper = new ObjectMapper();

        // 1. Build "properties": { "query": { "type": "string", "description": "..." } }
        ObjectNode queryProp = factory.objectNode()
                .put("type", "string")
                .put("description", "Job title or keywords to search for");

        ObjectNode properties = factory.objectNode();
        properties.set("query", queryProp);

        // 2. Build "required": ["query"]
        ArrayNode requiredFields = factory.arrayNode()
                .add("query");

        // 3. Build "inputSchema": { "type": "object", "properties": ..., "required": ... }
        ObjectNode inputSchema = factory.objectNode()
                .put("type", "object");
        inputSchema.set("properties", properties);
        inputSchema.set("required", requiredFields);

        // 4. Build the specific tool object
        ObjectNode searchJobsTool = factory.objectNode()
                .put("name", "search_jobs")
                .put("description", "Searches for jobs matching a query across multiple job boards. Returns raw listings — Claude should filter for relevance.");
        searchJobsTool.set("inputSchema", inputSchema);

        // 5. Build "tools": [...] array and add the tool
        ArrayNode toolsList = factory.arrayNode();
        toolsList.add(searchJobsTool);

        // 6. Build "result": { "tools": [...] }
        ObjectNode result = factory.objectNode();
        result.set("tools", toolsList);

        // 7. Build the root JSON object
        ObjectNode root = factory.objectNode()
                .put("jsonrpc", "2.0")
                .put("id", requestId);
        root.set("result", result);

        // 8. Serialize to String (Pretty Printed)
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
