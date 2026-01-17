package com.hulft.mcp;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods", "PMD.CyclomaticComplexity", "PMD.ExcessiveClassLength"})
public class MCPServer {
    @SuppressWarnings("PMD.FieldNamingConventions") // Object instances, not primitive constants
    private static final Gson gson = new Gson();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();
    
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider credentialsProvider = 
        software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider.create("predev");
    
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final software.amazon.awssdk.services.textract.TextractClient textractClient = 
        software.amazon.awssdk.services.textract.TextractClient.builder()
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .credentialsProvider(credentialsProvider)
            .build();
    
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final software.amazon.awssdk.services.comprehend.ComprehendClient comprehendClient =
        software.amazon.awssdk.services.comprehend.ComprehendClient.builder()
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .credentialsProvider(credentialsProvider)
            .build();
    
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient =
        software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient.builder()
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .credentialsProvider(credentialsProvider)
            .build();
    
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Map<String, String> customSchemas = new ConcurrentHashMap<>();
    
    private static final ThreadLocal<Float> ocrConfidence = new ThreadLocal<>();
    
    static class JobStatus {
        String status; // "processing", "completed", "failed"
        Map<String, Object> result;
        String error;
    }

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
        if (log.isInfoEnabled()) {
            log.info("Response: {}", gson.toJson(response));
        }
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
                            "description", "Upload multiple files (PDF, Excel, Image, or Archive). Supports async processing. Returns structured JSON, markdown, and classification.",
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
                                    ),
                                    "async", Map.of("type", "boolean", "description", "Process asynchronously (returns job ID)")
                                ),
                                "required", List.of("files")
                            )
                        ),
                        Map.of(
                            "name", "check_job",
                            "description", "Check status of async job",
                            "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "job_id", Map.of("type", "string", "description", "Job ID from async upload")
                                ),
                                "required", List.of("job_id")
                            )
                        ),
                        Map.of(
                            "name", "add_schema",
                            "description", "Add custom extraction schema for a document type",
                            "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "doc_type", Map.of("type", "string", "description", "Document type (e.g., CUSTOM_INVOICE)"),
                                    "schema", Map.of("type", "string", "description", "JSON schema for field extraction")
                                ),
                                "required", List.of("doc_type", "schema")
                            )
                        ),
                        Map.of(
                            "name", "list_schemas",
                            "description", "List all custom schemas",
                            "inputSchema", Map.of("type", "object", "properties", Map.of())
                        ),
                        Map.of(
                            "name", "get_schema",
                            "description", "Get schema for a document type",
                            "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "doc_type", Map.of("type", "string", "description", "Document type")
                                ),
                                "required", List.of("doc_type")
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
                    case "upload_files" -> {
                        List<Map<String, Object>> files = (List<Map<String, Object>>) arguments.get("files");
                        boolean async = arguments.containsKey("async") && (Boolean) arguments.get("async");
                        
                        if (async) {
                            String jobId = java.util.UUID.randomUUID().toString();
                            JobStatus status = new JobStatus();
                            status.status = "processing";
                            jobs.put(jobId, status);
                            
                            executor.submit(() -> {
                                try {
                                    String result = handleMultiFileUpload(files);
                                    status.result = Map.of("text", result);
                                    status.status = "completed";
                                } catch (Exception e) { // NOPMD - Catch all for async error handling
                                    status.error = e.getMessage();
                                    status.status = "failed";
                                }
                            });
                            
                            yield "Job started: " + jobId + "\nUse check_job tool to get status.";
                        } else {
                            yield handleMultiFileUpload(files);
                        }
                    }
                    case "check_job" -> {
                        String jobId = (String) arguments.get("job_id");
                        JobStatus status = jobs.get(jobId);
                        if (status == null) {
                            yield "Job not found: " + jobId;
                        } else if (status.status.equals("completed")) {
                            yield (String) status.result.get("text");
                        } else if (status.status.equals("failed")) {
                            yield "Job failed: " + status.error;
                        } else {
                            yield "Job status: " + status.status;
                        }
                    }
                    case "add_schema" -> {
                        String docType = (String) arguments.get("doc_type");
                        String schema = (String) arguments.get("schema");
                        customSchemas.put(docType, schema);
                        yield "Schema added for: " + docType;
                    }
                    case "list_schemas" -> {
                        if (customSchemas.isEmpty()) {
                            yield "No custom schemas defined. Using built-in schemas.";
                        } else {
                            StringBuilder sb = new StringBuilder("Custom schemas:\n");
                            customSchemas.forEach((type, schema) -> 
                                sb.append("- ").append(type).append("\n"));
                            yield sb.toString();
                        }
                    }
                    case "get_schema" -> {
                        String docType = (String) arguments.get("doc_type");
                        String schema = customSchemas.getOrDefault(docType, getSchemaForDocType(docType));
                        yield "Schema for " + docType + ":\n" + schema;
                    }
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
    
    static String handleMultiFileUpload(List<Map<String, Object>> files) {
        try {
            StringBuilder result = new StringBuilder();
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("uploadTime", java.time.Instant.now().toString());
            metadata.put("fileCount", files.size());
            List<Map<String, Object>> fileMetadata = new java.util.ArrayList<>();
            
            // Check if any file is an archive
            boolean hasArchive = files.stream()
                .anyMatch(f -> "archive".equals(f.get("type")));
            
            if (hasArchive) {
                // All files go into one job folder
                String jobId = java.util.UUID.randomUUID().toString();
                String jobPath = createJobFolder(jobId);
                metadata.put("jobId", jobId);
                metadata.put("type", "archive");
                result.append(String.format("Job ID: %s (archive extraction)\n", jobId));
                result.append(String.format("Files: %d\n\n", files.size()));
                
                for (Map<String, Object> file : files) {
                    String filename = (String) file.get("filename");
                    String content = (String) file.get("content");
                    String type = (String) file.get("type");
                    
                    byte[] fileBytes = java.util.Base64.getDecoder().decode(content);
                    
                    // Detect actual file type
                    String detectedType = detectFileType(fileBytes, filename);
                    log.info("File {} - Declared: {}, Detected: {}", filename, type, detectedType);
                    
                    Map<String, Object> fileMeta = new java.util.HashMap<>();
                    fileMeta.put("filename", filename);
                    fileMeta.put("declaredType", type);
                    fileMeta.put("detectedType", detectedType);
                    fileMeta.put("size", fileBytes.length);
                    
                    if ("archive".equals(type)) {
                        java.nio.file.Path archivePath = java.nio.file.Paths.get(jobPath, filename);
                        java.nio.file.Files.write(archivePath, fileBytes);
                        
                        // Extract archive
                        int extractedCount = extractArchive(archivePath.toString(), jobPath);
                        result.append(String.format("✓ %s (archive) - %d bytes - extracted %d files\n", 
                            filename, fileBytes.length, extractedCount));
                    } else {
                        java.nio.file.Path filePath = java.nio.file.Paths.get(jobPath, filename);
                        java.nio.file.Files.write(filePath, fileBytes);
                        
                        // Extract text and structured data
                        String textractResult;
                        Map<String, Object> structuredData = new java.util.HashMap<>();
                        String markdown = "";
                        
                        if ("excel".equals(type) || detectedType.contains("spreadsheet") || detectedType.contains("ooxml")) {
                            textractResult = extractExcelText(fileBytes);
                            structuredData = extractStructuredFromExcel(fileBytes);
                            markdown = convertExcelToMarkdown(fileBytes);
                        } else {
                            textractResult = analyzeWithTextract(fileBytes, filename);
                            structuredData = extractStructuredWithTextract(fileBytes);
                            markdown = convertToMarkdown(textractResult, structuredData);
                        }
                        
                        fileMeta.put("textractAnalysis", textractResult);
                        fileMeta.put("structuredData", structuredData);
                        fileMeta.put("markdown", markdown);
                        
                        result.append(String.format("✓ %s (%s) - %d bytes\n", filename, type, fileBytes.length));
                    }
                    
                    fileMetadata.add(fileMeta);
                    log.info("Saved {} to {}", filename, jobPath);
                }
                
                metadata.put("files", fileMetadata);
                saveMetadata(jobPath, metadata);
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
                    
                    // Detect actual file type
                    String detectedType = detectFileType(fileBytes, filename);
                    log.info("File {} - Declared: {}, Detected: {}", filename, type, detectedType);
                    
                    java.nio.file.Path filePath = java.nio.file.Paths.get(jobPath, filename);
                    java.nio.file.Files.write(filePath, fileBytes);
                    
                    // Extract text and structured data
                    String textractResult;
                    Map<String, Object> structuredData = new java.util.HashMap<>();
                    String markdown = "";
                    
                    if ("excel".equals(type) || detectedType.contains("spreadsheet") || detectedType.contains("ooxml")) {
                        textractResult = extractExcelText(fileBytes);
                        structuredData = extractStructuredFromExcel(fileBytes);
                        markdown = convertExcelToMarkdown(fileBytes);
                    } else {
                        textractResult = analyzeWithTextract(fileBytes, filename);
                        structuredData = extractStructuredWithTextract(fileBytes);
                        markdown = convertToMarkdown(textractResult, structuredData);
                    }
                    
                    // Save metadata for this job
                    Map<String, Object> jobMeta = new java.util.HashMap<>();
                    jobMeta.put("jobId", jobId);
                    jobMeta.put("uploadTime", java.time.Instant.now().toString());
                    jobMeta.put("type", "single");
                    jobMeta.put("filename", filename);
                    jobMeta.put("declaredType", type);
                    jobMeta.put("detectedType", detectedType);
                    jobMeta.put("size", fileBytes.length);
                    jobMeta.put("textractAnalysis", textractResult);
                    jobMeta.put("structuredData", structuredData);
                    jobMeta.put("markdown", markdown);
                    
                    // Add OCR confidence if available
                    Float confidence = ocrConfidence.get();
                    if (confidence != null) {
                        jobMeta.put("ocrConfidence", confidence);
                        ocrConfidence.remove();
                    }
                    
                    // Classify document
                    Map<String, Object> classification = classifyDocument(textractResult);
                    Map<String, Object> consensus = getConsensusClassification(classification);
                    
                    // Extract structured fields with Bedrock
                    Map<String, Object> extractedFields = extractFieldsWithBedrock(textractResult, (String) consensus.get("type"));
                    
                    jobMeta.put("classification", classification);
                    jobMeta.put("finalClassification", consensus);
                    jobMeta.put("extractedFields", extractedFields);
                    
                    saveMetadata(jobPath, jobMeta);
                    
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
    
    @SuppressWarnings("PMD.GuardLogStatement") // Simple log, not expensive
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
    
    private static String detectFileType(byte[] fileBytes, String filename) {
        try {
            // Use Apache Tika to detect MIME type from content
            org.apache.tika.Tika tika = new org.apache.tika.Tika();
            String mimeType = tika.detect(fileBytes);
            
            log.info("Detected MIME type for {}: {}", filename, mimeType);
            
            // Map MIME type to our file types
            if (mimeType.contains("pdf")) return "pdf";
            if (mimeType.contains("spreadsheet") || mimeType.contains("excel")) return "excel";
            if (mimeType.contains("image")) return "image";
            if (mimeType.contains("zip") || mimeType.contains("tar") || mimeType.contains("archive")) return "archive";
            
            return mimeType;
        } catch (Exception e) {
            log.error("Error detecting file type", e);
            return "unknown";
        }
    }
    
    private static String analyzeWithTextract(byte[] fileBytes, String filename) {
        try {
            software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest request = 
                software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest.builder()
                    .document(software.amazon.awssdk.services.textract.model.Document.builder()
                        .bytes(software.amazon.awssdk.core.SdkBytes.fromByteArray(fileBytes))
                        .build())
                    .build();
            
            software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse response = 
                textractClient.detectDocumentText(request);
            
            StringBuilder text = new StringBuilder();
            java.util.List<Float> confidences = new java.util.ArrayList<>();
            
            for (software.amazon.awssdk.services.textract.model.Block block : response.blocks()) {
                if (block.blockType() == software.amazon.awssdk.services.textract.model.BlockType.LINE) {
                    text.append(block.text()).append("\n");
                    if (block.confidence() != null) {
                        confidences.add(block.confidence());
                    }
                }
            }
            
            // Calculate average confidence
            float avgConfidence = confidences.isEmpty()
                ? 0
                : (float) confidences.stream().mapToDouble(Float::doubleValue).average().orElse(0);
            
            String extractedText = text.toString();
            log.info("Textract extracted {} characters from {} (avg confidence: {:.2f}%)", 
                extractedText.length(), filename, avgConfidence);
            
            // Store confidence in thread-local for metadata
            ocrConfidence.set(avgConfidence);
            
            return extractedText;
            
        } catch (Exception e) {
            log.error("Error with Textract analysis", e);
            return "Textract analysis failed: " + e.getMessage();
        }
    }
    
    private static String extractExcelText(byte[] fileBytes) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(fileBytes));
            StringBuilder text = new StringBuilder();
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        text.append(cell.toString()).append(" ");
                    }
                    text.append("\n");
                }
            }
            workbook.close();
            return text.toString();
        } catch (Exception e) {
            log.error("Error extracting Excel text", e);
            return "Excel extraction failed: " + e.getMessage();
        }
    }
    
    private static Map<String, Object> extractStructuredWithTextract(byte[] fileBytes) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        // Extract tables with Tabula
        try {
            java.io.File tempFile = java.io.File.createTempFile("doc", ".pdf");
            java.nio.file.Files.write(tempFile.toPath(), fileBytes);
            
            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(tempFile);
            technology.tabula.ObjectExtractor extractor = new technology.tabula.ObjectExtractor(document);
            technology.tabula.PageIterator pages = extractor.extract();
            
            java.util.List<java.util.List<java.util.List<String>>> allTables = new java.util.ArrayList<>();
            while (pages.hasNext()) {
                technology.tabula.Page page = pages.next();
                java.util.List<technology.tabula.Table> tables = new technology.tabula.extractors.BasicExtractionAlgorithm().extract(page);
                for (technology.tabula.Table table : tables) {
                    java.util.List<java.util.List<String>> tableData = new java.util.ArrayList<>();
                    for (java.util.List<technology.tabula.RectangularTextContainer> row : table.getRows()) {
                        java.util.List<String> rowData = new java.util.ArrayList<>();
                        for (technology.tabula.RectangularTextContainer cell : row) {
                            rowData.add(cell.getText());
                        }
                        tableData.add(rowData);
                    }
                    allTables.add(tableData);
                }
            }
            extractor.close();
            tempFile.delete();
            
            result.put("tables", allTables);
        } catch (Exception e) {
            log.warn("Tabula extraction failed: {}", e.getMessage());
            result.put("tables", java.util.List.of());
        }
        
        result.put("keyValues", Map.of());
        return result;
    }
    
    private static Map<String, Object> extractStructuredFromExcel(byte[] fileBytes) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(fileBytes));
            Map<String, Object> result = new java.util.HashMap<>();
            java.util.List<Map<String, Object>> sheets = new java.util.ArrayList<>();
            
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                Map<String, Object> sheetData = new java.util.HashMap<>();
                sheetData.put("name", sheet.getSheetName());
                
                java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    java.util.List<String> rowData = new java.util.ArrayList<>();
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        rowData.add(cell.toString());
                    }
                    rows.add(rowData);
                }
                sheetData.put("rows", rows);
                sheets.add(sheetData);
            }
            
            result.put("sheets", sheets);
            workbook.close();
            return result;
        } catch (Exception e) {
            log.error("Error extracting structured Excel data", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    private static String convertToMarkdown(String text, Map<String, Object> structured) {
        StringBuilder md = new StringBuilder();
        md.append("# Document\n\n");
        
        // Add key-value pairs
        if (structured.containsKey("keyValues")) {
            @SuppressWarnings("unchecked")
            Map<String, String> kv = (Map<String, String>) structured.get("keyValues");
            if (!kv.isEmpty()) {
                md.append("## Fields\n\n");
                kv.forEach((k, v) -> md.append("- **").append(k).append("**: ").append(v).append("\n"));
                md.append("\n");
            }
        }
        
        // Add raw text
        md.append("## Content\n\n");
        md.append("```\n").append(text).append("\n```\n");
        
        return md.toString();
    }
    
    private static String convertExcelToMarkdown(byte[] fileBytes) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(fileBytes));
            StringBuilder md = new StringBuilder();
            
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                md.append("# ").append(sheet.getSheetName()).append("\n\n");
                
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    md.append("| ");
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        md.append(cell.toString()).append(" | ");
                    }
                    md.append("\n");
                    
                    // Add header separator after first row
                    if (row.getRowNum() == 0) {
                        md.append("| ");
                        for (int i = 0; i < row.getLastCellNum(); i++) {
                            md.append("--- | ");
                        }
                        md.append("\n");
                    }
                }
                md.append("\n");
            }
            
            workbook.close();
            return md.toString();
        } catch (Exception e) {
            log.error("Error converting Excel to markdown", e);
            return "Error: " + e.getMessage();
        }
    }
    
    private static void saveMetadata(String jobPath, Map<String, Object> metadata) {
        try {
            java.nio.file.Path metaPath = java.nio.file.Paths.get(jobPath, "meta.json");
            String json = gson.toJson(metadata);
            java.nio.file.Files.writeString(metaPath, json);
            log.info("Saved metadata to {}", metaPath);
        } catch (Exception e) {
            log.error("Error saving metadata", e);
        }
    }
    
    private static Map<String, Object> classifyDocument(String textractText) {
        Map<String, Object> classification = new java.util.HashMap<>();
        
        // Method 1: Regex extraction
        String regexType = extractPurposeCode(textractText);
        classification.put("regex", Map.of("type", regexType, "confidence", regexType.equals("UNKNOWN") ? 0.0 : 1.0));
        
        // Method 2: AWS Comprehend
        try {
            Map<String, Object> comprehendResult = classifyWithComprehend(textractText);
            classification.put("comprehend", comprehendResult);
        } catch (Exception e) {
            log.error("Comprehend classification failed", e);
            classification.put("comprehend", Map.of("error", e.getMessage()));
        }
        
        // Method 3: AWS Bedrock (Claude)
        try {
            Map<String, Object> bedrockResult = classifyWithBedrock(textractText);
            classification.put("bedrock", bedrockResult);
        } catch (Exception e) {
            log.error("Bedrock classification failed", e);
            classification.put("bedrock", Map.of("error", e.getMessage()));
        }
        
        return classification;
    }
    
    private static String extractPurposeCode(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Purpose:\\s*([A-Z_]+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "UNKNOWN";
    }
    
    private static Map<String, Object> classifyWithComprehend(String text) {
        // Detect entities to help classify
        software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest request = 
            software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest.builder()
                .text(text.substring(0, Math.min(5000, text.length())))
                .languageCode("en")
                .build();
        
        software.amazon.awssdk.services.comprehend.model.DetectEntitiesResponse response = 
            comprehendClient.detectEntities(request);
        
        // Simple heuristic based on entities
        String docType = "UNKNOWN";
        double confidence = 0.0;
        
        for (software.amazon.awssdk.services.comprehend.model.Entity entity : response.entities()) {
            if (entity.text().contains("Invoice") || entity.text().contains("INV-")) {
                docType = "INVOICE_PRODUCTION";
                confidence = entity.score();
            } else if (entity.text().contains("PO-") || entity.text().contains("Purchase")) {
                docType = "PURCHASE_ORDER";
                confidence = entity.score();
            } else if (entity.text().contains("Schedule") || entity.text().contains("Production")) {
                docType = "SCHEDULE_PRODUCTION";
                confidence = entity.score();
            } else if (entity.text().contains("Customs") || entity.text().contains("Declaration")) {
                docType = "CUSTOMS_DECLARATION";
                confidence = entity.score();
            }
        }
        
        return Map.of("type", docType, "confidence", confidence);
    }
    
    private static Map<String, Object> classifyWithBedrock(String text) {
        String prompt = String.format(
            "Classify this document as exactly one of: SCHEDULE_PRODUCTION, INVOICE_PRODUCTION, PURCHASE_ORDER, CUSTOMS_DECLARATION\n\n" +
            "Guidelines:\n" +
            "- INVOICE_PRODUCTION: Contains invoice number, customer, items, amounts, total\n" +
            "- PURCHASE_ORDER: Contains PO number, vendor, items to purchase, delivery info\n" +
            "- SCHEDULE_PRODUCTION: Contains production schedule, quantities, dates, line assignments\n" +
            "- CUSTOMS_DECLARATION: Contains customs/declaration info, origin, destination, HS codes\n\n" +
            "Respond with ONLY JSON: {\"type\": \"XXX\", \"confidence\": 0.95}\n\n" +
            "Document:\n%s",
            text.substring(0, Math.min(2000, text.length()))
        );
        
        String requestBody = gson.toJson(Map.of(
            "anthropic_version", "bedrock-2023-05-31",
            "max_tokens", 100,
            "messages", List.of(Map.of(
                "role", "user",
                "content", prompt
            ))
        ));
        
        software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest request = 
            software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest.builder()
                .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                .body(software.amazon.awssdk.core.SdkBytes.fromUtf8String(requestBody))
                .build();
        
        software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse response = 
            bedrockClient.invokeModel(request);
        
        String responseBody = response.body().asUtf8String();
        Map<String, Object> responseJson = gson.fromJson(responseBody, Map.class);
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseJson.get("content");
        String claudeResponse = (String) content.get(0).get("text");
        
        // Parse Claude's JSON response
        return gson.fromJson(claudeResponse, Map.class);
    }
    
    private static int extractArchive(String archivePath, String destPath) {
        try {
            java.nio.file.Path archive = java.nio.file.Paths.get(archivePath);
            java.nio.file.Path dest = java.nio.file.Paths.get(destPath);
            
            if (archivePath.endsWith(".zip")) {
                return extractZip(archive, dest);
            } else if (archivePath.endsWith(".tar") || archivePath.endsWith(".tar.gz")) {
                return extractTar(archive, dest);
            }
            
            log.warn("Unsupported archive format: {}", archivePath);
            return 0;
        } catch (Exception e) {
            log.error("Error extracting archive", e);
            return 0;
        }
    }
    
    private static int extractZip(java.nio.file.Path zipFile, java.nio.file.Path destDir) throws Exception {
        int count = 0;
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                java.nio.file.Files.newInputStream(zipFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    java.nio.file.Path filePath = destDir.resolve(entry.getName());
                    java.nio.file.Files.createDirectories(filePath.getParent());
                    java.nio.file.Files.copy(zis, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    count++;
                    log.info("Extracted: {}", entry.getName());
                }
                zis.closeEntry();
            }
        }
        return count;
    }
    
    private static int extractTar(java.nio.file.Path tarFile, java.nio.file.Path destDir) throws Exception {
        int count = 0;
        java.io.InputStream fileStream = java.nio.file.Files.newInputStream(tarFile);
        
        // Handle .tar.gz
        if (tarFile.toString().endsWith(".gz")) {
            fileStream = new java.util.zip.GZIPInputStream(fileStream);
        }
        
        try (org.apache.commons.compress.archivers.tar.TarArchiveInputStream tis = 
                new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(fileStream)) {
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                if (!entry.isDirectory()) {
                    java.nio.file.Path filePath = destDir.resolve(entry.getName());
                    java.nio.file.Files.createDirectories(filePath.getParent());
                    java.nio.file.Files.copy(tis, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    count++;
                    log.info("Extracted: {}", entry.getName());
                }
            }
        }
        return count;
    }
    
    private static Map<String, Object> getConsensusClassification(Map<String, Object> classification) {
        // Extract results from each method
        Map<String, Object> regex = (Map<String, Object>) classification.get("regex");
        Map<String, Object> comprehend = (Map<String, Object>) classification.getOrDefault("comprehend", Map.of());
        Map<String, Object> bedrock = (Map<String, Object>) classification.getOrDefault("bedrock", Map.of());
        
        String regexType = (String) regex.get("type");
        double regexConf = ((Number) regex.get("confidence")).doubleValue();
        
        String comprehendType = (String) comprehend.getOrDefault("type", "UNKNOWN");
        double comprehendConf = comprehend.containsKey("confidence") ? 
            ((Number) comprehend.get("confidence")).doubleValue() : 0.0;
        
        String bedrockType = (String) bedrock.getOrDefault("type", "UNKNOWN");
        double bedrockConf = bedrock.containsKey("confidence") ? 
            ((Number) bedrock.get("confidence")).doubleValue() : 0.0;
        
        // Voting: if 2+ agree, use that
        if (regexType.equals(comprehendType) || regexType.equals(bedrockType)) {
            return Map.of(
                "type", regexType,
                "confidence", Math.max(regexConf, Math.max(comprehendConf, bedrockConf)),
                "method", "consensus"
            );
        }
        
        if (comprehendType.equals(bedrockType) && !comprehendType.equals("UNKNOWN")) {
            return Map.of(
                "type", comprehendType,
                "confidence", (comprehendConf + bedrockConf) / 2,
                "method", "consensus"
            );
        }
        
        // No consensus - use highest confidence
        if (regexConf >= comprehendConf && regexConf >= bedrockConf) {
            return Map.of("type", regexType, "confidence", regexConf, "method", "regex");
        } else if (bedrockConf >= comprehendConf) {
            return Map.of("type", bedrockType, "confidence", bedrockConf, "method", "bedrock");
        } else {
            return Map.of("type", comprehendType, "confidence", comprehendConf, "method", "comprehend");
        }
    }
    
    private static Map<String, Object> extractFieldsWithBedrock(String text, String docType) {
        try {
            String schema = getSchemaForDocType(docType);
            String prompt = String.format(
                "Extract fields from this document and return ONLY a JSON object (no markdown, no explanation).\n\nSchema:\n%s\n\nDocument:\n%s\n\nJSON:",
                schema, text.substring(0, Math.min(2000, text.length()))
            );
            
            String requestBody = gson.toJson(Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 1000,
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt
                ))
            ));
            
            software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest request = 
                software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest.builder()
                    .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                    .body(software.amazon.awssdk.core.SdkBytes.fromUtf8String(requestBody))
                    .build();
            
            software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
            String extractedText = (String) content.get(0).get("text");
            
            // Extract JSON from response (handle markdown code blocks)
            extractedText = extractedText.trim();
            if (extractedText.startsWith("```")) {
                int start = extractedText.indexOf('{');
                int end = extractedText.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    extractedText = extractedText.substring(start, end + 1);
                }
            }
            
            // Find first { and last }
            int jsonStart = extractedText.indexOf('{');
            int jsonEnd = extractedText.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                extractedText = extractedText.substring(jsonStart, jsonEnd + 1);
            }
            
            return gson.fromJson(extractedText, Map.class);
            
        } catch (Exception e) {
            log.error("Bedrock field extraction failed: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
    
    private static String getSchemaForDocType(String docType) {
        // Check for custom schema first
        if (customSchemas.containsKey(docType)) {
            return customSchemas.get(docType);
        }
        
        // Fall back to built-in schemas
        return switch (docType) {
            case "INVOICE_PRODUCTION" -> """
                {
                  "invoice_number": "string",
                  "date": "string",
                  "customer": "string",
                  "total_amount": "string",
                  "items": [{"name": "string", "amount": "string"}]
                }
                """;
            case "PURCHASE_ORDER" -> """
                {
                  "po_number": "string",
                  "date": "string",
                  "vendor": "string",
                  "total_amount": "string",
                  "items": [{"name": "string", "quantity": "string"}]
                }
                """;
            case "SCHEDULE_PRODUCTION" -> """
                {
                  "date": "string",
                  "product": "string",
                  "quantity": "string",
                  "start_time": "string",
                  "end_time": "string",
                  "line": "string"
                }
                """;
            case "CUSTOMS_DECLARATION" -> """
                {
                  "declaration_number": "string",
                  "date": "string",
                  "origin": "string",
                  "destination": "string",
                  "items": [{"description": "string", "value": "string"}]
                }
                """;
            default -> "{}";
        };
    }
}
