package com.jobsearch.mcpserver.transport;

import io.javalin.http.sse.SseClient;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private static ConcurrentHashMap<String, SseClient> sessions = new ConcurrentHashMap<>();
    public static void add(String sessionId, SseClient client) {
        sessions.put(sessionId, client);
    }

    public static SseClient get(String sessionId) {
        return sessions.get(sessionId);
    }

    public static void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
