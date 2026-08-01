---
title: "feat: Build Java MCP Server MVP (Raw HTTP+SSE)"
status: active
date: 2026-06-12
depth: standard
---

# feat: Build Java MCP Server MVP (Raw HTTP+SSE)

## Summary

Build a Java MCP server from scratch — without using the Spring AI MCP SDK — that exposes one `search_jobs` tool. The tool fans out to two job plugins in parallel, aggregates raw results, and returns them to Claude. Claude handles relevance reasoning. The server runs in Docker using HTTP+SSE transport.

This plan is a **learning guide**. Each unit ends with a concrete checkpoint you can verify before moving forward. The goal is to understand every layer of the MCP protocol by building it yourself.

---

## Problem Frame

MCP (Model Context Protocol) is a JSON-RPC 2.0 protocol over HTTP+SSE. Building it raw — without an SDK abstracting the protocol — forces you to understand the initialization handshake, SSE stream management, session routing, tool registration, and parallel plugin execution. After this MVP, the Spring AI MCP SDK will make obvious sense because you already know what it's doing for you.

**Scope boundary:** This MVP does not include real Amazon/Google job pages (which block scraping). Instead, two free public job APIs with no authentication — Remotive and Arbeitnow — serve as the two plugin targets. The plugin interface pattern is identical; swapping the underlying URL is trivial later.

---

## Key Technical Decisions

| Decision | Choice | Rationale |
|---|---|---|
| HTTP framework | Javalin 6.x | First-class SSE API, minimal boilerplate, single fat JAR, no XML config |
| MCP transport | Old HTTP+SSE (spec `2024-11-05`) | Still the transport Claude Desktop/Code expects; simpler than Streamable HTTP |
| JSON library | Jackson (jackson-databind) | No MCP SDK — you parse JSON-RPC manually to learn the protocol |
| Java version | Java 21 | LTS, virtual threads available for parallel plugin calls |
| Build | Maven + maven-shade-plugin | Fat JAR needed for Docker; shade plugin bundles all deps |
| Job API 1 | Remotive (`remotive.com/api/remote-jobs`) | Free, no auth, returns JSON |
| Job API 2 | Arbeitnow (`arbeitnow.com/api/job-board-api`) | Free, no auth, returns JSON |
| Parallelism | `CompletableFuture.allOf()` | Both plugins called in parallel; results merged before returning |
| Docker base image | `eclipse-temurin:21-jre-alpine` | Small JRE-only image, no JDK overhead in production |

---

## High-Level Technical Design

### Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     McpServer (Javalin)                      │
│                                                             │
│   GET /sse ──────────────────────► SseHandler               │
│                                        │ stores SseClient    │
│                                        ▼                     │
│                                   SessionStore               │
│                                    (sessionId → SseClient)  │
│                                        ▲                     │
│   POST /message ─────► McpRouter ──────┘ writes SSE response│
│                             │                               │
│                    ┌────────┼────────┐                      │
│                    ▼        ▼        ▼                      │
│             initialize  tools/list  tools/call              │
│                                        │                    │
│                                   ToolRegistry              │
│                                        │                    │
│                              SearchJobsTool                 │
│                              /              \               │
│                    RemotivePlugin      ArbeitnowPlugin      │
│                    (HTTP call)         (HTTP call)          │
│                    via CompletableFuture.allOf()             │
└─────────────────────────────────────────────────────────────┘
```

### MCP Protocol Sequence

```
Claude Code                               Your MCP Server
     |                                          |
     |── GET /sse ────────────────────────────►|
     |◄── event: endpoint                       |
     |    data: http://localhost:8080/message   |
     |         ?sessionId=abc123               |
     |                                          |
     |── POST /message (initialize) ──────────►|
     |◄── 200 OK                               |
     |◄── SSE: message (InitializeResult)       |
     |                                          |
     |── POST /message (notifications/         |
     |        initialized) ───────────────────►|
     |◄── 202 (no SSE response needed)         |
     |                                          |
     |── POST /message (tools/list) ──────────►|
     |◄── 200 OK                               |
     |◄── SSE: message (ToolsListResult)        |
     |                                          |
     |── POST /message (tools/call             |
     |        search_jobs) ───────────────────►|
     |◄── 200 OK                               |
     |  [CompletableFuture.allOf() waits]       |
     |◄── SSE: message (ToolCallResult)         |
```

### SSE Wire Format (What You Send on the Stream)

```
# Immediately after client connects to GET /sse:
event: endpoint\n
data: http://localhost:8080/message?sessionId=abc123\n
\n

# After receiving a request and computing the response:
event: message\n
data: {"jsonrpc":"2.0","id":1,"result":{...}}\n
\n
```

---

## Output Structure

```
src/
└── main/
    └── java/com/example/mcpserver/
        ├── McpServer.java               ← entry point, Javalin app
        ├── transport/
        │   ├── SessionStore.java        ← sessionId → SseClient map
        │   └── McpRouter.java          ← JSON-RPC method dispatcher
        ├── protocol/
        │   ├── JsonRpcRequest.java     ← incoming message POJO
        │   └── JsonRpcResponse.java    ← outgoing message POJO
        ├── tools/
        │   ├── ToolRegistry.java       ← registers + invokes tools
        │   └── SearchJobsTool.java     ← search_jobs implementation
        └── plugins/
            ├── JobPlugin.java          ← interface
            ├── JobListing.java         ← shared result type
            ├── RemotivePlugin.java     ← Remotive API
            └── ArbeitnowPlugin.java    ← Arbeitnow API
pom.xml
Dockerfile
```

---

## Implementation Units

### U1. Maven Project Setup

**Goal:** Create a working Maven project that compiles and produces a runnable fat JAR.

**What you're learning:** How to wire Javalin and Jackson together without Spring Boot's auto-magic. How `maven-shade-plugin` creates a self-contained JAR.

**Approach:**
- Create a standard Maven project with `groupId: com.example`, `artifactId: mcp-server`, `version: 1.0-SNAPSHOT`
- Java 21 source/target in the compiler plugin
- Add these dependencies to `pom.xml`:
  - `io.javalin:javalin:6.3.0` — HTTP server and SSE
  - `com.fasterxml.jackson.core:jackson-databind:2.17.2` — JSON parsing
  - `org.slf4j:slf4j-simple:2.0.13` — Logging (Javalin needs an SLF4J impl)
- Configure `maven-shade-plugin` to produce a fat JAR with a `Main-Class` manifest entry pointing to `com.example.mcpserver.McpServer`

**Files:**
- `pom.xml`
- `src/main/java/com/example/mcpserver/McpServer.java` (stub — just a `main()` that prints "Server starting")

**Test scenarios:**
- Run `mvn package` — should produce `target/mcp-server-1.0-SNAPSHOT.jar` with no compilation errors
- Run `java -jar target/mcp-server-1.0-SNAPSHOT.jar` — should print "Server starting" and exit cleanly

**Verification:** `mvn package -q && java -jar target/mcp-server-1.0-SNAPSHOT.jar` exits 0 with output.

---

### U2. JSON-RPC Message Types

**Goal:** Create POJOs representing the JSON-RPC messages your server will send and receive.

**What you're learning:** The exact shape of MCP messages at the wire level. Jackson's `@JsonIgnoreProperties(ignoreUnknown = true)` lets you handle messages whose fields you haven't mapped yet.

**Dependencies:** U1

**Approach:**

Create POJOs for two shapes:

**Incoming (client → server), `JsonRpcRequest`:**
```
id        — Integer or String (null for notifications)
method    — String ("initialize", "tools/list", "tools/call", etc.)
params    — JsonNode (leave untyped — you'll cast per method)
```

**Outgoing (server → client via SSE), `JsonRpcResponse`:**
```
jsonrpc   — always "2.0"
id        — mirrors the request id
result    — JsonNode (for success)
error     — ErrorObject (for protocol errors: code + message)
```

Key insight: a `notifications/initialized` message has no `id`. Your POJO must tolerate `null` id — use `Integer` (nullable) not `int`.

**Files:**
- `src/main/java/com/example/mcpserver/protocol/JsonRpcRequest.java`
- `src/main/java/com/example/mcpserver/protocol/JsonRpcResponse.java`

**Test scenarios:**
- Parse this JSON with Jackson into `JsonRpcRequest`: `{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05"}}` — verify `getId()` == 1, `getMethod()` == "initialize"
- Parse a notification (no id): `{"jsonrpc":"2.0","method":"notifications/initialized"}` — verify `getId()` == null
- Serialize a `JsonRpcResponse` with a result node — verify the output contains `"jsonrpc":"2.0"`

**Verification:** Unit test or a `main()` test method that round-trips the above JSON shapes without throwing exceptions.

---

### U3. HTTP Server + SSE Transport

**Goal:** Build the two-endpoint HTTP transport that forms the backbone of the MCP server.

**What you're learning:** How SSE keeps a connection alive. How the session ID links the GET stream to the POST messages. Why responses go back on the stream instead of in the HTTP response body.

**Dependencies:** U1, U2

**Approach:**

`SessionStore` holds a `ConcurrentHashMap<String, SseClient>` — thread-safe because POST requests arrive concurrently.

`McpServer.main()` creates a Javalin app with two routes:

**GET `/sse` handler:**
1. Generate a `UUID` as the session ID
2. Call `client.keepAlive()` to prevent Javalin from closing the connection
3. Send the `endpoint` event: `"event: endpoint\ndata: http://localhost:8080/message?sessionId=<id>\n\n"`
4. Register `client` in `SessionStore` under the session ID
5. Register `client.onClose()` to remove from `SessionStore`

**POST `/message` handler:**
1. Extract `sessionId` from query params
2. Look up the `SseClient` — return 400 if not found
3. Read the request body as a string
4. Pass to `McpRouter` along with the `SseClient` for the response
5. Always return HTTP 200 (202 for notifications)

**Critical detail:** SSE writes from the POST handler thread must be synchronized on the client reference. Javalin's `SseClient.sendEvent()` is not thread-safe.

**Files:**
- `src/main/java/com/example/mcpserver/transport/SessionStore.java`
- `src/main/java/com/example/mcpserver/transport/McpRouter.java` (stub that just echoes the request id back for now)
- `src/main/java/com/example/mcpserver/McpServer.java` (updated with routes)

**Test scenarios:**
- `curl -N http://localhost:8080/sse` — should keep connection open and print an `endpoint` event with a URL containing a `sessionId`
- Extract the session ID from the URL, then POST to it: `curl -X POST "http://localhost:8080/message?sessionId=<id>" -d '{"jsonrpc":"2.0","id":1,"method":"ping"}' -H "Content-Type: application/json"` — the SSE stream should receive back a `message` event (even just an echo of id=1)
- Open two concurrent SSE connections — verify they get different session IDs

**Verification:** Both curl commands above work without errors. The SSE stream stays open after the POST.

---

### U4. MCP Protocol Handler (initialize + tools/list)

**Goal:** Implement the three messages that Claude sends before ever calling a tool: `initialize`, `notifications/initialized`, and `tools/list`.

**What you're learning:** The capability negotiation handshake. Why `initialized` is a notification (no response). What the `tools` capability object means in the initialize response.

**Dependencies:** U3

**Approach:**

`McpRouter` switches on `request.getMethod()` and dispatches:

**`initialize`:**
Build and send via SSE:
```json
{
  "jsonrpc": "2.0",
  "id": <request.id>,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": { "tools": {} },
    "serverInfo": { "name": "job-search-mcp", "version": "1.0.0" }
  }
}
```

**`notifications/initialized`:**
This is a notification — no `id`, no SSE response. Return HTTP 202 from the POST handler. Do nothing else.

**`tools/list`:**
Build and send via SSE a list containing the `search_jobs` tool definition:
```json
{
  "name": "search_jobs",
  "description": "Searches for jobs matching a query across multiple job boards. Returns raw listings — Claude should filter for relevance.",
  "inputSchema": {
    "type": "object",
    "properties": {
      "query": {
        "type": "string",
        "description": "Job title or keywords to search for (e.g., 'software engineer', 'SDM')"
      }
    },
    "required": ["query"]
  }
}
```

**Files:**
- `src/main/java/com/example/mcpserver/transport/McpRouter.java` (full implementation)
- `src/main/java/com/example/mcpserver/tools/ToolRegistry.java` (returns tool definitions list)

**Test scenarios:**
- Connect via SSE, then POST the `initialize` message — the SSE stream should receive an `InitializeResult` with `protocolVersion: "2024-11-05"` and `capabilities.tools` present
- POST `notifications/initialized` — HTTP response should be 202, no SSE message should appear on the stream
- POST `tools/list` — SSE stream should return a list containing exactly one tool named `search_jobs` with an `inputSchema` that has a required `query` property
- POST an unknown method — SSE stream should return a JSON-RPC error with code `-32601` (method not found)

**Verification:** Connect Claude Code or Claude Desktop to `http://localhost:8080/sse` and verify it lists the `search_jobs` tool without errors.

---

### U5. JobPlugin Interface + Two Implementations

**Goal:** Build the plugin system — a Java interface and two implementations that each hit a real job API.

**What you're learning:** How to structure a plugin architecture in Java. How `CompletableFuture` enables parallel HTTP calls without blocking each other.

**Dependencies:** U1

**Approach:**

`JobPlugin` interface has one method:
```
List<JobListing> search(String query) throws Exception
```

`JobListing` is a simple record/POJO:
```
title     — String
company   — String
location  — String
url       — String
source    — String (e.g., "remotive", "arbeitnow")
```

**`RemotivePlugin`:**
- URL: `https://remotive.com/api/remote-jobs?search={query}&limit=10`
- Response shape: `{ "jobs": [ { "title", "company_name", "candidate_required_location", "url" } ] }`
- Use `java.net.http.HttpClient` (built into Java 11+, no extra dependency)
- Parse with Jackson, map to `List<JobListing>`

**`ArbeitnowPlugin`:**
- URL: `https://www.arbeitnow.com/api/job-board-api?search={query}`
- Response shape: `{ "data": [ { "title", "company_name", "location", "url" } ] }`
- Same HTTP client and Jackson pattern

**Parallel fan-out in `SearchJobsTool`:**
```
CompletableFuture<List<JobListing>> f1 = CompletableFuture.supplyAsync(() -> remotivePlugin.search(query));
CompletableFuture<List<JobListing>> f2 = CompletableFuture.supplyAsync(() -> arbeitnowPlugin.search(query));
CompletableFuture.allOf(f1, f2).join();
List<JobListing> combined = Stream.concat(f1.join().stream(), f2.join().stream()).toList();
```

Handle exceptions per plugin — if one plugin fails, still return the other's results (don't let one failure kill both).

**Files:**
- `src/main/java/com/example/mcpserver/plugins/JobPlugin.java`
- `src/main/java/com/example/mcpserver/plugins/JobListing.java`
- `src/main/java/com/example/mcpserver/plugins/RemotivePlugin.java`
- `src/main/java/com/example/mcpserver/plugins/ArbeitnowPlugin.java`
- `src/main/java/com/example/mcpserver/tools/SearchJobsTool.java`

**Test scenarios:**
- Call `new RemotivePlugin().search("software engineer")` directly in a test main — should return a non-empty list with titles and URLs
- Call `new ArbeitnowPlugin().search("software engineer")` — same
- Call `SearchJobsTool.execute("software engineer")` — should return results from both sources (check the `source` field in each listing)
- Call with a nonsense query like `"xkcd1234unique"` — should return an empty list, not throw an exception
- Simulate one plugin failing (temporarily break its URL) — the other plugin's results should still appear in the combined list

**Verification:** `SearchJobsTool.execute("java developer")` returns at least one result with a non-null URL and `source` field set correctly.

---

### U6. tools/call Handler + Full Integration

**Goal:** Wire `tools/call` into the router, connect `SearchJobsTool`, and return results in the MCP tool response format.

**What you're learning:** The exact shape of a `tools/call` result. The difference between a tool-level error (`isError: true` in `result`) and a protocol-level error (top-level `error` field).

**Dependencies:** U4, U5

**Approach:**

`McpRouter` handles `tools/call`:
1. Extract `params.name` and `params.arguments.query` from the `JsonNode`
2. Look up the tool in `ToolRegistry` — return `-32602` (invalid params) if not found
3. Execute the tool
4. Serialize the `List<JobListing>` to a JSON string (pretty-print is fine for readability)
5. Send via SSE:

```json
{
  "jsonrpc": "2.0",
  "id": <request.id>,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "<serialized job listings>"
      }
    ],
    "isError": false
  }
}
```

If the tool throws an exception, return:
```json
{
  "result": {
    "content": [{ "type": "text", "text": "Search failed: <exception message>" }],
    "isError": true
  }
}
```

Note: `isError: true` still uses `result`, not the top-level `error` field. Protocol errors (unknown method, malformed JSON) use `error`. Tool execution failures use `result.isError`.

**Files:**
- `src/main/java/com/example/mcpserver/transport/McpRouter.java` (add `tools/call` case)
- `src/main/java/com/example/mcpserver/tools/ToolRegistry.java` (register `SearchJobsTool`)

**Test scenarios:**
- Full end-to-end via curl: POST `initialize`, POST `notifications/initialized`, POST `tools/list`, POST `tools/call` with `{"name":"search_jobs","arguments":{"query":"java developer"}}` — SSE stream should receive job listings as a text content block
- POST `tools/call` with unknown tool name `"nonexistent_tool"` — should return JSON-RPC error with code `-32602`
- POST `tools/call` with missing `query` argument — should return a result with `isError: true` (tool-level error), not a protocol error
- Connect Claude Code to the running server and ask "find me java developer jobs" — Claude should call `search_jobs` and summarize the results

**Verification:** Claude Code can call `search_jobs` and returns actual job listings from both Remotive and Arbeitnow.

---

### U7. Docker Packaging

**Goal:** Package the server into a Docker container and connect Claude Code to the containerized server.

**What you're learning:** Why HTTP+SSE is the right transport choice for Docker (unlike stdio, which requires spawning the process as a child). How to configure Claude Code to connect to a remote MCP server.

**Dependencies:** U6

**Approach:**

**`Dockerfile`:**
```
Base image: eclipse-temurin:21-jre-alpine
COPY the fat JAR from target/ to /app/mcp-server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/mcp-server.jar"]
```

Build and run the jar inside Docker using a multi-stage build (optional but recommended):
- Stage 1 (`maven:3.9-eclipse-temurin-21`): run `mvn package`
- Stage 2 (`eclipse-temurin:21-jre-alpine`): copy jar, set entrypoint

Single-stage is fine for learning — build locally with `mvn package`, copy the jar.

**Claude Code configuration** — add to your MCP settings:
```json
{
  "mcpServers": {
    "job-search": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Run: `docker run -p 8080:8080 job-search-mcp`

Claude Code connects to `http://localhost:8080/sse` — the server is inside Docker but Claude Code is on the host, so `localhost:8080` resolves correctly through the port mapping.

**Files:**
- `Dockerfile`

**Test scenarios:**
- `docker build -t job-search-mcp .` — builds successfully with no errors
- `docker run -p 8080:8080 job-search-mcp` — container starts, server logs appear, port 8080 is accessible from host
- `curl -N http://localhost:8080/sse` from the host — receives the `endpoint` event
- Claude Code with the SSE config above connects to the containerized server and lists `search_jobs`
- Claude Code calls `search_jobs` and returns live results from inside the container

**Verification:** Claude Code's tool list shows `search_jobs` with the container running via `docker run`.

---

## Scope Boundaries

### In scope (this MVP)
- One MCP tool: `search_jobs`
- Two plugins: Remotive and Arbeitnow (free, no auth)
- Old HTTP+SSE transport (2024-11-05)
- Parallel plugin fan-out with `CompletableFuture`
- Docker packaging

### Deferred to Follow-Up Work
- Real company job pages (Amazon Jobs, Google Careers, LinkedIn) — these require scraping or paid APIs; the plugin interface makes swapping trivial later
- User profile + resume store for server-side relevance filtering
- Semantic/embedding-based relevance (connects to RAG learning)
- Streamable HTTP transport (MCP spec 2025-03-26+) — upgrade path after the MVP works
- More than two plugins
- Tool authentication / API key management

### Outside scope entirely
- Web UI or dashboard
- Persistent job storage / database

---

## Risks and Dependencies

| Risk | Mitigation |
|---|---|
| Remotive or Arbeitnow API changes response shape | Check the actual response format in Phase U5 before building the POJO mappers — don't assume the field names above are exact |
| SSE connection drops silently | Implement a periodic heartbeat comment (`:\n\n`) every 30s to keep the connection alive through proxies and firewalls |
| `SseClient.sendEvent()` thread safety | Synchronize writes on the client reference in `SessionStore` — document this explicitly in code |
| CORS when Claude connects | Add `Access-Control-Allow-Origin: *` headers to both `/sse` and `/message` endpoints |
| Docker networking: `localhost` ambiguity | When running server inside Docker and connecting from a host process, `localhost:8080` resolves correctly. If connecting from *another container*, use the container name or `host.docker.internal` instead |

---

## Sources and Research

- [MCP HTTP+SSE Transport Spec (2024-11-05)](https://modelcontextprotocol.io/specification/2024-11-05/basic/transports)
- [MCP Tools Specification](https://modelcontextprotocol.io/specification/2025-06-18/server/tools)
- [Javalin SSE Documentation](https://javalin.io/documentation)
- [Remotive Public API](https://remotive.com/api/remote-jobs) — no auth required
- [Arbeitnow Public API](https://www.arbeitnow.com/api/job-board-api) — no auth required
- Official Java MCP SDK (modelcontextprotocol/java-sdk) — read as reference, not used directly
