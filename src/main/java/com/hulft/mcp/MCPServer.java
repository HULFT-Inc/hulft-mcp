package com.hulft.mcp;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class MCPServer {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        log.info("Starting MCP Server");
        
        Javalin app = Javalin.create().start(3333);

        app.post("/mcp", ctx -> handlePost(ctx));
        app.get("/mcp", ctx -> handleGet(ctx));

        log.info("MCP Server running on http://localhost:3333/mcp");
    }

    private static void handlePost(Context ctx) {
        String accept = ctx.header("Accept");
        String auth = ctx.header("Authorization");
        
        log.info("Authorization header: {}", auth != null ? "Bearer ***" : "none");
        
        if (accept == null || (!accept.contains("application/json") && !accept.contains("text/event-stream"))) {
            ctx.status(400).result("Accept header must include application/json or text/event-stream");
            return;
        }

        String body = ctx.body();
        log.info("POST /mcp: {}", body);

        Map<String, Object> request = gson.fromJson(body, Map.class);
        String method = (String) request.get("method");
        Object id = request.get("id");

        // Handle notifications (no response needed)
        if (id == null) {
            log.info("✓ Received notification: {} - Accepting with 202", method);
            ctx.status(202).result("");
            return;
        }

        Map<String, Object> response = createResponse(method, request, id);
        
        ctx.contentType("application/json");
        ctx.json(response);
        log.info("Response: {}", gson.toJson(response));
    }

    private static void handleGet(Context ctx) {
        String accept = ctx.header("Accept");
        if (accept == null || !accept.contains("text/event-stream")) {
            ctx.status(405).result("Method Not Allowed");
            return;
        }

        log.info("GET /mcp - SSE stream requested");
        ctx.contentType("text/event-stream");
        ctx.result(""); // Keep connection open for SSE
    }

    private static Map<String, Object> createResponse(String method, Map<String, Object> request, Object id) {
        // Convert double IDs to integers for cleaner JSON
        if (id instanceof Double) {
            double d = (Double) id;
            if (d == Math.floor(d)) {
                id = (int) d;
            }
        }
        
        return switch (method) {
            case "initialize" -> {
                Map<String, Object> params = (Map<String, Object>) request.get("params");
                String clientProtocol = (String) params.get("protocolVersion");
                yield Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                        "protocolVersion", clientProtocol, // Match client's protocol version
                        "capabilities", Map.of("tools", Map.of()),
                        "serverInfo", Map.of(
                            "name", "hulft-mcp",
                            "version", "1.0.0"
                        )
                    )
                );
            }
            case "tools/list" -> Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Map.of(
                    "tools", List.of(
                        Map.of(
                            "name", "echo",
                            "description", "Echoes back the input text",
                            "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "text", Map.of("type", "string", "description", "Text to echo")
                                ),
                                "required", List.of("text")
                            )
                        )
                    )
                )
            );
            case "tools/call" -> {
                Map<String, Object> params = (Map<String, Object>) request.get("params");
                Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
                yield Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                        "content", List.of(Map.of(
                            "type", "text",
                            "text", "Echo: " + arguments.get("text")
                        ))
                    )
                );
            }
            default -> Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "error", Map.of(
                    "code", -32601,
                    "message", "Method not found: " + method
                )
            );
        };
    }
}
