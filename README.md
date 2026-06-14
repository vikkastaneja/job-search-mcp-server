# Job Search MCP Server

  A Model Context Protocol (MCP) server built in Java from scratch that enables Claude to search for
  jobs across multiple job boards simultaneously.

  ## What it does

  Exposes a `search_jobs` tool to Claude that fans out to multiple job board APIs in parallel,
  aggregates the results, and returns them. Claude then reasons about relevance based on your query.

  ## Architecture

  - **Transport:** HTTP + SSE (Server-Sent Events) — works across Docker and remote deployments
  - **Protocol:** MCP over JSON-RPC 2.0 (implemented manually without the Spring AI MCP SDK)
  - **Plugin pattern:** Java interface (`JobPlugin`) with swappable implementations
  - **Parallelism:** `CompletableFuture.allOf()` fans out to all plugins simultaneously

  ## Job Sources

  | Plugin | API | Auth Required |
  |---|---|---|
  | RemotivePlugin | remotive.com/api/remote-jobs | No |
  | ArbeitnowPlugin | arbeitnow.com/api/job-board-api | No |

  ## Prerequisites

  - Java 21+
  - Maven 3.9+
  - Docker (for containerized deployment)

  ## Build

  ```bash
  mvn package

  Produces target/job-search-mcp-server.jar

  Run locally
  
  java -jar target/job-search-mcp-server.jar

  Server starts on http://localhost:8080

  Run in Docker

  docker build -t job-search-mcp-server .
  docker run -p 8080:8080 job-search-mcp-server

  Connect to Claude Code

  Add to your Claude Code MCP settings:

  {
    "mcpServers": {
      "job-search": {
        "type": "sse",
        "url": "http://localhost:8080/sse"
      }
    }
  }

  Then ask Claude: "Find me software engineer jobs" — it will call search_jobs and summarize results.

  Project Structure
  
  src/main/java/com/jobsearch/mcpserver/
  ├── McpServer.java            — Entry point, Javalin HTTP server
  ├── transport/
  │   ├── SessionStore.java     — SSE session management
  │   └── McpRouter.java        — JSON-RPC method dispatcher
  ├── protocol/
  │   ├── JsonRpcRequest.java   — Incoming message POJO
  │   ├── JsonRpcResponse.java  — Outgoing message POJO
  │   └── ErrorObject.java      — JSON-RPC error shape
  ├── tools/
  │   ├── ToolRegistry.java     — Tool registration and dispatch
  │   └── SearchJobsTool.java   — search_jobs implementation
  ├── plugins/
  │   ├── JobPlugin.java        — Plugin interface
  │   ├── JobListing.java       — Shared result type
  │   ├── RemotivePlugin.java   — Remotive API
  │   └── ArbeitnowPlugin.java  — Arbeitnow API
  └── util/
      └── JsonUtil.java         — Shared ObjectMapper


  MCP Protocol Flow

  Claude          Your Server
    |── GET /sse ──────────────►|
    |◄── event: endpoint        |   server delivers POST URL with sessionId
    |                           |
    |── POST /message ─────────►|   initialize
    |◄── SSE: InitializeResult  |
    |                           |
    |── POST /message ─────────►|   tools/list
    |◄── SSE: ToolsListResult   |
    |                           |
    |── POST /message ─────────►|   tools/call search_jobs
    |◄── SSE: job listings      |   aggregated from all plugins

  Adding a New Job Source

  Implement the JobPlugin interface:
  
  public class MyJobsPlugin implements JobPlugin {
      @Override
      public List<JobListing> search(String query) throws Exception {
          // call your API, return List<JobListing>
      }
  }

  Register it in SearchJobsTool — no other changes needed.

  Roadmap
  
  - [ ] User profile + resume store for server-side relevance filtering
  - [ ] Semantic/embedding-based job matching (RAG)
  - [ ] More job board plugins (LinkedIn, Google Jobs, Greenhouse)
  - [ ] Streamable HTTP transport (MCP spec 2025-03-26+)
  - [ ] API key management for authenticated job boards

  License

  MIT
  
  That covers everything someone finding the repo on GitHub needs: what it does, how to run it, how to
   connect it to Claude, and how to extend it with new plugins.