# Project State — Java MCP Server (Learning Project)

Last updated: 2026-06-16

## What This Project Is

A Java MCP server built from scratch (no SDK) exposing a `search_jobs` tool. Purpose is learning MCP protocol internals — initialization handshake, SSE transport, session routing, tool registration, and async plugin fan-out.

Plan file: `2026-06-12-001-feat-java-mcp-server-mvp-plan.md`

Stack: Javalin 6.x, Jackson, Java 21, Maven fat JAR, Docker. Transport: HTTP+SSE (spec 2024-11-05).

---

## Progress

**All units U1–U7 complete.** Verified end-to-end with a real Claude Code client (not just curl) — including against the Dockerized server.

---

## Files (current, real layout — README had drifted from this)

### handlers/
- `BaseHandler.java` — base class; default `handle()` sends `getResponse()` via SSE event `"message"`
- `InitializeHandler.java` — `initialize` response (protocolVersion, capabilities, serverInfo)
- `ToolsListHandler.java` — `tools/list` response (returns `search_jobs` tool definition)
- `SearchJobsHandler.java` — `tools/call` response; overrides `handle()` directly (doesn't use `getResponse()`) because it needs full control of success/error response shapes
- `ErrorHandler.java` — generic protocol error (`-32601` method not found), used by `McpRouter`'s default case

### transport/
- `McpRouter.java` — dispatches by `method`: `initialize`, `tools/list`, `notifications/initialized`, `tools/call`, default→error
- `SessionStore.java` — `ConcurrentHashMap<sessionId, SseClient>`

### protocol/
- `JsonRpcRequest.java`, `JsonRpcResponse.java`, `ErrorObject.java`

### tools/
- `SearchJobsTool.java` — `CompletableFuture.supplyAsync` wraps `AmazonPlugin.search()`, exceptions caught inside the lambda and converted to empty list (partial-failure tolerant), `allOf().join()` collects

### plugins/
- `JobPlugin.java` — interface
- `JobListing.java` — data model (title, company, location, description, url, source)
- `AmazonPlugin.java` — implements `JobPlugin`; builds URI via `AmazonJobSearchUrlBuilder`, HTTP GET via `java.net.http.HttpClient`, delegates parsing to `util.AmazonJobParser`
- `AmazonJobSearchService.java` — alternate/earlier HTTP-call wrapper (hardcoded Seattle SDM query) — largely superseded by the logic now inlined in `AmazonPlugin` itself; not currently used in the active call path
- ⚠️ `plugins/AmazonJobParser.java` — **unused duplicate**, leftover copy of `util/AmazonJobParser.java`. Only the `util` package version is actually imported anywhere. Candidate for deletion.

### util/
- `AmazonJobSearchUrlBuilder.java` — fluent builder for `amazon.jobs/en/search.json` (radius, offset, resultLimit, sort, locGroupId, locQuery, baseQuery)
- `AmazonJobParser.java` — the one actually used; parses `"jobs"` array, maps title/company_name/normalized_location/description_short/job_path → JobListing
- `JsonUtil.java` — shared `ObjectMapper`-based `fromJson`

**Note:** the README describes `RemotivePlugin`/`ArbeitnowPlugin` per the original plan — those were never built. The actual implementation pivoted to a single `AmazonPlugin` hitting `amazon.jobs/en/search.json` directly (a real, undocumented-but-public JSON endpoint, not scraping). README needs to reflect this pivot.

---

## Key Bug Found & Fixed (U6)

`SearchJobsHandler` originally sent its SSE events with `client.sendEvent("response", ...)` instead of `client.sendEvent("message", ...)`. The real MCP client only listens for SSE events named `"message"` per spec — `initialize`/`tools/list` worked because they go through `BaseHandler.handle()` which correctly uses `"message"`, but `SearchJobsHandler` overrides `handle()` directly and had drifted to a different event name. Symptom: server logs showed correct JSON-RPC responses being built and "sent," but the real Claude Code client never received them (looked like a hang/timeout). Fixed by changing both `sendEvent` calls (success + error path) in `SearchJobsHandler` to use `"message"`.

---

## Design Decisions

- URL building, HTTP transport, and JSON parsing are separate classes
- Plugins stay synchronous — `SearchJobsTool` owns async fan-out via `CompletableFuture`
- Single plugin (Amazon) by deliberate choice — second plugin (Remotive/Arbeitnow per original plan) deferred; not worth building until the one-plugin path was proven end-to-end
- `query` param maps to `withBaseQuery(query)` on the URL builder
- Plugin failures return empty list rather than propagating (partial results > total failure)
- Tool-name validation lives in `SearchJobsHandler` (-32602 invalid params for unknown tool name), distinct from protocol-level method-not-found (-32601) in `ErrorHandler`

---

## Verified End-to-End

- Real Claude Code client connects via `claude mcp add --transport sse job-search http://localhost:8080/sse`
- Full handshake (`initialize` → `notifications/initialized` → `tools/list`) works
- `tools/call search_jobs` returns live Amazon Jobs listings through the real client
- Same flow verified again against the **Dockerized** server (`docker build` + `docker run -p 8080:8080`) — no client-side config change needed, confirming HTTP+SSE's container-friendliness vs stdio

---

## Remaining / Deferred (not blocking, from original plan)

- Second plugin (Remotive/Arbeitnow, or another real source)
- Streamable HTTP transport upgrade (2025-03-26+ spec)
- Resume-aware/server-side relevance filtering (currently Claude reasons over raw listings manually)
- Delete unused duplicate `plugins/AmazonJobParser.java`
- Decide fate of `AmazonJobSearchService.java` (superseded by logic now in `AmazonPlugin`) — keep as reference or delete
