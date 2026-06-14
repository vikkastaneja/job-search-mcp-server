package com.jobsearch.mcpserver.handlers;

import com.jobsearch.mcpserver.protocol.JsonRpcRequest;

import io.javalin.http.sse.SseClient;

public class BaseHandler {
    public void handle(JsonRpcRequest request, SseClient client) {
        client.sendEvent("message", getResponse(request));
    }

    protected String getResponse(JsonRpcRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResponse'");
    }
}
