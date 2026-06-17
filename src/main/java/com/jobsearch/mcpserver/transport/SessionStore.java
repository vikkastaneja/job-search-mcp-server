package com.jobsearch.mcpserver.transport;

import io.javalin.http.sse.SseClient;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private ConcurrentHashMap<String, SseClient> sessions = new ConcurrentHashMap<>();

    public void add(String sessionId, SseClient client) {
        sessions.put(sessionId, client);
    }

    public SseClient get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
