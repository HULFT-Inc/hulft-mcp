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
                        "protocolVersion", clientProtocol,
                        "capabilities", Map.of(
                            "tools", Map.of(),
                            "resources", Map.of(),
                            "prompts", Map.of()
                        ),
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
                        ),
                        Map.of(
                            "name", "upload_files",
                            "description", "Upload multiple files (PDF, Excel, Image, or Archive). Archives are extracted into one job folder, other files get separate job folders.",
                            "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "files", Map.of(
                                        "type", "array",
                                        "description", "Array of files to upload",
                                        "items", Map.of(
                                            "type", "object",
                                            "properties", Map.of(
                                                "filename", Map.of("type", "string", "description", "File name"),
                                                "content", Map.of("type", "string", "description", "Base64 encoded content"),
                                                "type", Map.of("type", "string", "enum", List.of("pdf", "excel", "image", "archive"), "description", "File type")
                                            ),
                                            "required", List.of("filename", "content", "type")
                                        )
                                    )
                                ),
                                "required", List.of("files")
                            )
                        ),
                        Map.of(
                            "name", "list_resources",
                            "description", "List all available resources",
                            "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of()
                            )
                        ),
                        Map.of(
                            "name", "read_resource",
                            "description", "Read a resource by URI",
                            "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "uri", Map.of("type", "string", "description", "Resource URI (e.g., file:///example.txt)")
                                ),
                                "required", List.of("uri")
                            )
                        ),
                        Map.of(
                            "name", "get_prompt",
                            "description", "Get a prompt template (code-review)",
                            "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "name", Map.of("type", "string", "description", "Prompt name"),
                                    "code", Map.of("type", "string", "description", "Code to review")
                                ),
                                "required", List.of("name", "code")
                            )
                        )
                    )
                )
            );
            case "tools/call" -> {
                Map<String, Object> params = (Map<String, Object>) request.get("params");
                String toolName = (String) params.get("name");
                Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
                
                String resultText = switch (toolName) {
                    case "echo" -> "Echo: " + arguments.get("text");
                    case "list_resources" -> "Available resources:\n- file:///example.txt (Example File) - An example text resource";
                    case "read_resource" -> {
                        String uri = (String) arguments.get("uri");
                        yield "Content of " + uri + ":\nThis is example content from: " + uri;
                    }
                    case "get_prompt" -> {
                        String code = (String) arguments.get("code");
                        yield "Code Review Prompt:\nPlease review this code:\n\n" + code;
                    }
                    case "upload_files" -> handleMultiFileUpload((List<Map<String, Object>>) arguments.get("files"));
                    default -> "Unknown tool: " + toolName;
                };
                
                yield Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                        "content", List.of(Map.of(
                            "type", "text",
                            "text", resultText
                        ))
                    )
                );
            }
            case "resources/list" -> Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Map.of(
                    "resources", List.of(
                        Map.of(
                            "uri", "file:///example.txt",
                            "name", "Example File",
                            "description", "An example text resource",
                            "mimeType", "text/plain"
                        )
                    )
                )
            );
            case "resources/read" -> {
                Map<String, Object> params = (Map<String, Object>) request.get("params");
                String uri = (String) params.get("uri");
                yield Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                        "contents", List.of(Map.of(
                            "uri", uri,
                            "mimeType", "text/plain",
                            "text", "This is example content from: " + uri
                        ))
                    )
                );
            }
            case "prompts/list" -> Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Map.of(
                    "prompts", List.of(
                        Map.of(
                            "name", "code-review",
                            "description", "Review code for best practices",
                            "arguments", List.of(
                                Map.of(
                                    "name", "code",
                                    "description", "Code to review",
                                    "required", true
                                )
                            )
                        )
                    )
                )
            );
            case "prompts/get" -> {
                Map<String, Object> params = (Map<String, Object>) request.get("params");
                String name = (String) params.get("name");
                Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
                String code = (String) arguments.get("code");
                
                yield Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                        "description", "Code review prompt",
                        "messages", List.of(
                            Map.of(
                                "role", "user",
                                "content", Map.of(
                                    "type", "text",
                                    "text", "Please review this code:\n\n" + code
                                )
                            )
                        )
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
    
    private static String handleMultiFileUpload(List<Map<String, Object>> files) {
        try {
            StringBuilder result = new StringBuilder();
            
            // Check if any file is an archive
            boolean hasArchive = files.stream()
                .anyMatch(f -> "archive".equals(f.get("type")));
            
            if (hasArchive) {
                // All files go into one job folder
                String jobId = java.util.UUID.randomUUID().toString();
                String jobPath = createJobFolder(jobId);
                result.append(String.format("Job ID: %s (archive extraction)\n", jobId));
                result.append(String.format("Files: %d\n\n", files.size()));
                
                for (Map<String, Object> file : files) {
                    String filename = (String) file.get("filename");
                    String content = (String) file.get("content");
                    String type = (String) file.get("type");
                    
                    byte[] fileBytes = java.util.Base64.getDecoder().decode(content);
                    
                    if ("archive".equals(type)) {
                        // TODO: Extract archive (zip, tar, etc.)
                        java.nio.file.Path archivePath = java.nio.file.Paths.get(jobPath, filename);
                        java.nio.file.Files.write(archivePath, fileBytes);
                        result.append(String.format("✓ %s (archive) - %d bytes [TODO: extract]\n", filename, fileBytes.length));
                    } else {
                        java.nio.file.Path filePath = java.nio.file.Paths.get(jobPath, filename);
                        java.nio.file.Files.write(filePath, fileBytes);
                        result.append(String.format("✓ %s (%s) - %d bytes\n", filename, type, fileBytes.length));
                    }
                    log.info("Saved {} to {}", filename, jobPath);
                }
                result.append(String.format("\nPath: %s", jobPath));
            } else {
                // Each file gets its own job folder
                result.append(String.format("Files: %d (separate jobs)\n\n", files.size()));
                
                for (Map<String, Object> file : files) {
                    String filename = (String) file.get("filename");
                    String content = (String) file.get("content");
                    String type = (String) file.get("type");
                    
                    String jobId = java.util.UUID.randomUUID().toString();
                    String jobPath = createJobFolder(jobId);
                    byte[] fileBytes = java.util.Base64.getDecoder().decode(content);
                    
                    java.nio.file.Path filePath = java.nio.file.Paths.get(jobPath, filename);
                    java.nio.file.Files.write(filePath, fileBytes);
                    
                    result.append(String.format("✓ %s (%s)\n  Job ID: %s\n  Size: %d bytes\n\n", 
                        filename, type, jobId, fileBytes.length));
                    log.info("Saved {} to {}", filename, filePath);
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error uploading files", e);
            return "Error uploading files: " + e.getMessage();
        }
    }
    
    private static String handlePdfUpload(String filename, String base64Content) {
        log.info("PDF upload: {} ({} bytes base64)", filename, base64Content.length());
        
        try {
            String jobId = java.util.UUID.randomUUID().toString();
            String jobPath = createJobFolder(jobId);
            byte[] pdfBytes = java.util.Base64.getDecoder().decode(base64Content);
            
            java.nio.file.Path filePath = java.nio.file.Paths.get(jobPath, filename);
            java.nio.file.Files.write(filePath, pdfBytes);
            
            log.info("PDF saved to: {}", filePath);
            
            // TODO: Extract text from PDF using Apache PDFBox
            // TODO: Index PDF content for search
            // TODO: Extract metadata
            
            return String.format("PDF uploaded successfully!\nJob ID: %s\nPath: %s\nSize: %d bytes", 
                jobId, filePath, pdfBytes.length);
        } catch (Exception e) {
            log.error("Error uploading PDF", e);
            return "Error uploading PDF: " + e.getMessage();
        }
    }
    
    private static String handleExcelUpload(String filename, String base64Content) {
        log.info("Excel upload: {} ({} bytes base64)", filename, base64Content.length());
        
        try {
            String jobId = java.util.UUID.randomUUID().toString();
            String jobPath = createJobFolder(jobId);
            byte[] excelBytes = java.util.Base64.getDecoder().decode(base64Content);
            
            java.nio.file.Path filePath = java.nio.file.Paths.get(jobPath, filename);
            java.nio.file.Files.write(filePath, excelBytes);
            
            log.info("Excel saved to: {}", filePath);
            
            // TODO: Parse Excel using Apache POI
            // TODO: Convert to JSON or CSV
            // TODO: Store data in database
            
            return String.format("Excel uploaded successfully!\nJob ID: %s\nPath: %s\nSize: %d bytes", 
                jobId, filePath, excelBytes.length);
        } catch (Exception e) {
            log.error("Error uploading Excel", e);
            return "Error uploading Excel: " + e.getMessage();
        }
    }
    
    private static String handleImageUpload(String filename, String base64Content) {
        log.info("Image upload: {} ({} bytes base64)", filename, base64Content.length());
        
        try {
            String jobId = java.util.UUID.randomUUID().toString();
            String jobPath = createJobFolder(jobId);
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Content);
            
            java.nio.file.Path filePath = java.nio.file.Paths.get(jobPath, filename);
            java.nio.file.Files.write(filePath, imageBytes);
            
            log.info("Image saved to: {}", filePath);
            
            // TODO: Validate image format
            // TODO: Resize/optimize image
            // TODO: Extract EXIF metadata
            // TODO: Generate thumbnail
            
            return String.format("Image uploaded successfully!\nJob ID: %s\nPath: %s\nSize: %d bytes", 
                jobId, filePath, imageBytes.length);
        } catch (Exception e) {
            log.error("Error uploading image", e);
            return "Error uploading image: " + e.getMessage();
        }
    }
    
    private static String createJobFolder(String jobId) throws java.io.IOException {
        java.time.LocalDate now = java.time.LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String day = String.format("%02d", now.getDayOfMonth());
        
        String jobPath = String.format("jobs/%s/%s/%s/%s", year, month, day, jobId);
        java.nio.file.Path path = java.nio.file.Paths.get(jobPath);
        java.nio.file.Files.createDirectories(path);
        
        log.info("Created job folder: {}", jobPath);
        return jobPath;
    }
}
