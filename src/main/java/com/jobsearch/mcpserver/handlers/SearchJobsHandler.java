package com.jobsearch.mcpserver.handlers;

import com.jobsearch.mcpserver.protocol.JsonRpcRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsearch.mcpserver.plugins.JobListing;
import com.jobsearch.mcpserver.tools.SearchJobsTool;
import io.javalin.http.sse.SseClient;

public class SearchJobsHandler extends BaseHandler {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonFactory FACTORY = new JsonFactory(MAPPER);

    private final SearchJobsTool searchJobsTool;

    public SearchJobsHandler(SearchJobsTool searchJobsTool) {
        this.searchJobsTool = searchJobsTool;
    }

    @Override
    public void handle(JsonRpcRequest request, SseClient client) {
        
        /*
        Request format:
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "tools/call",
            "params": {
                "name": "search_jobs",
                "arguments": {
                "query": "software development manager"
                }
            }
        }
         */

        // Read the json above and extract the query parameter
        String query = (String) request.getParams().get("arguments").get("query").asText();
        System.out.println("Received search query: " + query);
        String name = (String) request.getParams().get("name").asText();
        System.out.println("Received tool name: " + name);
        if (!"search_jobs".equals(name)) {
            // Handle error: method name mismatch
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("jsonrpc", "2.0");
            errorResponse.put("id", request.getId());
            Map<String, Object> error = new HashMap<>();
            error.put("code", -32602); // Invalid params
            error.put("message", "Invalid param: unknown tool " + name);
            errorResponse.put("error", error);
            try {
                String jsonResponse = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(errorResponse);
                client.sendEvent("message", jsonResponse);
            } catch (IOException e) {
                System.err.println("Error serializing error response: " + e.getMessage());
                client.sendEvent("message", "{}"); // Send an empty response on serialization error
            }
            return;
        }

        // In a real implementation, you would trigger the tool execution flow here
        // For example:
        List<JobListing> jobListings = searchJobsTool.execute(query);
        System.out.println("Found " + jobListings.size() + " job listings for query: " + query);
        for (JobListing job : jobListings) {
            System.out.println("Job: " + job.getTitle() + " at " + job.getCompany());
        }
        /*
        Response format:
        {
            "jsonrpc": "2.0",
            "id": <request id>,
            "result": {
                "content": [{ "type": "text", "text": "<serialized listings>" }],
                "isError": false
            }
        }
         */

        // First Serialize the job listings into a JSON string format
        String serializedListings = toJson(jobListings);
        System.out.println("Serialized job listings: " + serializedListings);
        // Then build the full JSON-RPC response
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", request.getId());
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> content = new ArrayList<>();
        Map<String, String> contentItem = new HashMap<>();
        contentItem.put("type", "text");
        contentItem.put("text", serializedListings);
        content.add(contentItem);
        result.put("content", content);
        result.put("isError", false);
        response.put("result", result);
        
        // Send the response back to the client
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            System.out.println("Sending response: " + jsonResponse);
            client.sendEvent("message", jsonResponse);
        } catch (IOException e) {
            System.err.println("Error serializing response: " + e.getMessage());
            client.sendEvent("message", "{}"); // Send an empty response on error
        }
    }

    private String toJson(List<JobListing> jobList) {
        StringWriter writer = new StringWriter();
        
        try (JsonGenerator gen = FACTORY.createGenerator(writer)) {
            gen.writeStartArray();
            for (JobListing job : jobList) {
                gen.writeObject(job); 
            }
            gen.writeEndArray();
        } catch (IOException e) {
            // Log and return empty array on error
            System.err.println("Error serializing job listings: " + e.getMessage());
            return "[]";
        }
        
        return writer.toString();
    }
    
}
