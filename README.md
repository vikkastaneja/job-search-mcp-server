# Job Search MCP Server

A Model Context Protocol (MCP) server built in Java from scratch — no SDK — that lets Claude search live Amazon job listings.

This is a learning project: every protocol layer (initialization handshake, SSE transport, session routing, tool registration, async fan-out) is hand-implemented to understand what an MCP SDK normally does for you.

## What it does

Exposes a `search_jobs` tool to Claude. Claude calls it with a keyword query; the server hits Amazon's public job-search JSON endpoint, parses the results, and returns raw listings via SSE. Claude reasons about relevance — the server does no filtering or ranking itself.

## Architecture

- **Transport:** HTTP + SSE (Server-Sent Events), MCP spec `2024-11-05` — works the same whether the server runs on bare metal or inside Docker
- **Protocol:** MCP over JSON-RPC 2.0, parsed and dispatched manually (no Spring AI MCP SDK)
- **Plugin pattern:** `JobPlugin` interface with swappable implementations — currently one (`AmazonPlugin`)
- **Parallelism:** `SearchJobsTool` wraps each plugin call in `CompletableFuture.supplyAsync`, fans out via `CompletableFuture.allOf()`; a failing plugin returns an empty list instead of failing the whole search

## Job Sources

| Plugin | Source | Auth Required |
|---|---|---|
| `AmazonPlugin` | `amazon.jobs/en/search.json` (public JSON endpoint, not scraping) | No |

> Note: the original plan called for Remotive + Arbeitnow as two free no-auth APIs. The implementation pivoted to Amazon's own job-search JSON endpoint instead, since it returns real, directly relevant listings. Adding a second plugin is a deferred next step — the `JobPlugin` interface makes that a drop-in addition, no changes needed elsewhere.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for containerized run)

## Build

```bash
mvn package
```

Produces `target/job-search-mcp-server.jar` (fat jar, all dependencies bundled via `maven-shade-plugin`).

## Run locally

```bash
java -jar target/job-search-mcp-server.jar
```

Server starts on `http://localhost:8080`.

## Run in Docker

```bash
docker build -t job-search-mcp-server .
docker run -p 8080:8080 job-search-mcp-server
```

`http://localhost:8080` works identically whether the server is running directly or inside the container — HTTP+SSE just needs a published port, unlike `stdio` transport which would require spawning the server as a child process (impossible across a container boundary).

## Connect to Claude Code

**CLI:**
```bash
claude mcp add --transport sse job-search http://localhost:8080/sse
```

**Or `.mcp.json` in the project root:**
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

Start a new Claude Code session in this directory, run `/mcp` to confirm `job-search` is connected, then ask something like *"find me software development manager jobs"*. Claude calls `search_jobs` and summarizes the results.

## Project Structure

```
src/main/java/com/jobsearch/mcpserver/
├── McpServer.java                  — entry point; Javalin app, GET /sse + POST /message routes
├── transport/
│   ├── SessionStore.java           — sessionId → SseClient map
│   └── McpRouter.java              — dispatches JSON-RPC method to the right handler
├── handlers/
│   ├── BaseHandler.java            — default handle(): sendEvent("message", getResponse(request))
│   ├── InitializeHandler.java      — initialize response
│   ├── ToolsListHandler.java       — tools/list response
│   ├── SearchJobsHandler.java      — tools/call response (overrides handle() directly)
│   └── ErrorHandler.java           — protocol-level error (-32601 method not found)
├── protocol/
│   ├── JsonRpcRequest.java         — incoming message POJO
│   ├── JsonRpcResponse.java        — outgoing message POJO
│   └── ErrorObject.java            — JSON-RPC error shape
├── tools/
│   └── SearchJobsTool.java         — CompletableFuture fan-out across registered plugins
├── plugins/
│   ├── JobPlugin.java              — plugin interface: search(String query)
│   ├── JobListing.java             — shared result type
│   └── AmazonPlugin.java           — builds URL, makes HTTP call, delegates parsing
└── util/
    ├── AmazonJobSearchUrlBuilder.java — fluent builder for amazon.jobs/en/search.json
    ├── AmazonJobParser.java           — maps raw JSON "jobs" array → List<JobListing>
    └── JsonUtil.java                  — shared ObjectMapper helpers
```

## MCP Protocol Flow

```
Claude                      Your Server
  |── GET /sse ─────────────────────►|
  |◄── event: endpoint                |  delivers POST URL with sessionId
  |                                   |
  |── POST /message ─────────────────►|  initialize
  |◄── event: message (InitializeResult) |
  |                                   |
  |── POST /message (notification) ──►|  notifications/initialized
  |◄── 202 (no SSE response)          |
  |                                   |
  |── POST /message ─────────────────►|  tools/list
  |◄── event: message (ToolsListResult) |
  |                                   |
  |── POST /message ─────────────────►|  tools/call search_jobs
  |◄── event: message (job listings)   |
```

**Important wire-format detail:** every JSON-RPC response sent back over SSE must use the event name `message` — the real MCP client only listens for that event type. (`SearchJobsHandler` originally sent `event: response` by mistake; the response was correctly built and logged server-side but silently dropped client-side until fixed.)

## Adding a New Job Source

Implement the `JobPlugin` interface:

```java
public class MyJobsPlugin implements JobPlugin {
    @Override
    public List<JobListing> search(String query) throws Exception {
        // call your API, return List<JobListing>
    }
}
```

Wrap it in a `CompletableFuture.supplyAsync` inside `SearchJobsTool` alongside the existing plugin call, and include it in the `allOf(...)` — no other changes needed.

## Known Cleanup Items

- `plugins/AmazonJobParser.java` is an unused duplicate of `util/AmazonJobParser.java` — only the `util` version is imported anywhere; safe to delete
- `plugins/AmazonJobSearchService.java` is an earlier hardcoded-query (Seattle SDM only) HTTP-call wrapper, superseded by the logic now inlined in `AmazonPlugin.search()` — not on the active call path

## Roadmap

- [ ] Second plugin (Remotive, Arbeitnow, or another real source)
- [ ] Streamable HTTP transport (MCP spec 2025-03-26+)
- [ ] User profile + resume store for server-side relevance filtering
- [ ] Semantic/embedding-based job matching (RAG)
- [ ] API key management for authenticated job boards

## License

MIT
