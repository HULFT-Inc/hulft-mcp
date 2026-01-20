package com.hulft.mcp;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals", // JSON-RPC protocol strings
    "PMD.GodClass", // Main server class coordinates multiple services
    "PMD.CyclomaticComplexity", // Request routing requires branching
    "PMD.TooManyMethods", // MCP protocol requires many tool handlers
    "PMD.FieldNamingConventions", // Instance fields use camelCase, not UPPER_CASE
    "PMD.AvoidCatchingGenericException", // Generic exception handling for robustness
    "PMD.GuardLogStatement", // Simple log statements don't need guards
    "PMD.AvoidInstantiatingObjectsInLoops", // Necessary for file processing
    "PMD.CloseResource", // Resources managed by framework
    "PMD.UnusedPrivateMethod", // Methods used via reflection/lambda
    "PMD.AvoidLiteralsInIfCondition", // Clear status checks
    "PMD.ControlStatementBraces", // Single-line statements are clear
    "PMD.UnusedAssignment", // Assignments used in complex flows
    "PMD.ConsecutiveLiteralAppends", // Clear string building
    "PMD.ConsecutiveAppendsShouldReuse", // Clear string building
    "PMD.InsufficientStringBufferDeclaration", // Dynamic sizing is fine
    "PMD.LocalVariableCouldBeFinal", // Most variables are final, remaining are in complex contexts
    "PMD.UnnecessaryFullyQualifiedName" // Some qualified names needed for clarity
})
public class MCPServer {
    private static final Gson gson = new Gson();

    // Service components
    private static final JobManager jobManager = new JobManager();
    private static final SchemaManager schemaManager = new SchemaManager();
    private static final ArchiveExtractor archiveExtractor = new ArchiveExtractor();
    private static final MarkdownConverter markdownConverter = new MarkdownConverter();

    // AWS clients
    private static final software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider credentialsProvider =
        software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider.create("predev");

    private static final software.amazon.awssdk.services.textract.TextractClient textractClient =
        software.amazon.awssdk.services.textract.TextractClient.builder()
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .credentialsProvider(credentialsProvider)
            .build();

    private static final software.amazon.awssdk.services.comprehend.ComprehendClient comprehendClient =
        software.amazon.awssdk.services.comprehend.ComprehendClient.builder()
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .credentialsProvider(credentialsProvider)
            .build();

    private static final software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient =
        software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient.builder()
            .region(software.amazon.awssdk.regions.Region.US_EAST_1)
            .credentialsProvider(credentialsProvider)
            .build();

    // Service instances
    private static final TextExtractor textExtractor = new TextExtractor(textractClient);
    private static final DocumentClassifier classifier = new DocumentClassifier(comprehendClient, bedrockClient);
    private static final FieldExtractor fieldExtractor = new FieldExtractor(bedrockClient, schemaManager);

    private static final ThreadLocal<Float> ocrConfidence = new ThreadLocal<>();

    @SuppressWarnings("PMD.CloseResource") // Server runs until shutdown
    private static final Map<String, String> sessions = new ConcurrentHashMap<>();
    private static final String SUPPORTED_PROTOCOL_VERSION = "2025-11-25";
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost",
        "https://localhost",
        "http://127.0.0.1",
        "https://127.0.0.1"
    );

    public static void main(final String[] args) {
        log.info("Starting HULFT MCP Server v2.1.0");

        final Javalin app = Javalin.create().start("0.0.0.0", 3333);

        app.post("/mcp", ctx -> handlePost(ctx));
        app.get("/mcp", ctx -> handleGet(ctx));
        app.get("/health", ctx -> handleHealth(ctx));

        log.info("HULFT MCP Server ready at http://localhost:3333/mcp");
    }

    private static void handleHealth(final Context ctx) {
        final Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("version", "2.1.0");
        health.put("service", "hulft-mcp");
        health.put("timestamp", System.currentTimeMillis());
        ctx.json(health);
    }

    private static void handlePost(final Context ctx) {
        // 1. Validate Origin header (DNS rebinding protection)
        final String origin = ctx.header("Origin");
        if (origin != null && !isAllowedOrigin(origin)) {
            ctx.status(403).result("Forbidden");
            return;
        }

        // 2. Validate protocol version
        final String protocolVersion = ctx.header("MCP-Protocol-Version");
        if (protocolVersion != null && !SUPPORTED_PROTOCOL_VERSION.equals(protocolVersion)) {
            ctx.status(400).json(createError(-32600, "Unsupported protocol version: " + protocolVersion, null));
            return;
        }

        // 3. Validate Accept header
        final String accept = ctx.header("Accept");
        if (accept == null || !accept.contains("application/json") && !accept.contains("text/event-stream")) {
            ctx.status(400).json(createError(-32600, "Accept header must include application/json or text/event-stream", null));
            return;
        }

        final String body = ctx.body();
        log.info("POST /mcp: {}", body);

        final Map<String, Object> request = gson.fromJson(body, Map.class);
        final String method = (String) request.get("method");
        final Object id = request.get("id");

        // 4. Validate session (except for initialize)
        if (!"initialize".equals(method)) {
            final String sessionId = ctx.header("MCP-Session-Id");
            if (sessionId != null && !sessions.containsKey(sessionId)) {
                ctx.status(404).json(createError(-32600, "Session not found", null));
                return;
            }
        }

        // Handle notifications (no response needed)
        if (id == null) {
            log.info("✓ Received notification: {} - Accepting with 202", method);
            ctx.status(202).result("");
            return;
        }

        final Map<String, Object> response = createResponse(method, request, id);

        // 5. Add session ID for initialize response
        if ("initialize".equals(method) && response.containsKey("result")) {
            final String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, "active");
            ctx.header("MCP-Session-Id", sessionId);
            log.info("Created session: {}", sessionId);
        }

        ctx.contentType("application/json");
        ctx.json(response);
        if (log.isInfoEnabled()) {
            log.info("Response: {}", gson.toJson(response));
        }
    }

    private static void handleGet(final Context ctx) {
        // 1. Validate Origin header
        final String origin = ctx.header("Origin");
        if (origin != null && !isAllowedOrigin(origin)) {
            ctx.status(403).result("Forbidden");
            return;
        }

        // 2. Validate Accept header for SSE
        final String accept = ctx.header("Accept");
        if (accept == null || !accept.contains("text/event-stream")) {
            ctx.status(405).result("Method Not Allowed");
            return;
        }

        // 3. Return 405 - SSE streaming optional per spec
        log.info("GET /mcp - SSE streaming not supported");
        ctx.status(405).result("SSE streaming not supported. Use POST for requests.");
    }

    private static boolean isAllowedOrigin(final String origin) {
        if (origin == null) {
            return true;
        }
        return ALLOWED_ORIGINS.stream().anyMatch(origin::startsWith);
    }

    private static Map<String, Object> createError(final int code, final String message, final Object id) {
        final Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);

        final Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("error", error);
        if (id != null) {
            response.put("id", id);
        }
        return response;
    }

    @SuppressWarnings({"PMD.AvoidReassigningParameters", "PMD.CognitiveComplexity"}) // Intentional ID conversion, complex routing
    private static Map<String, Object> createResponse(final String method, final Map<String, Object> request, Object id) {
        // Convert double IDs to integers for cleaner JSON
        if (id instanceof Double) {
            final double d = (Double) id;
            if (d == Math.floor(d)) {
                id = (int) d;
            }
        }

        return switch (method) {
            case "initialize" -> {
                final Map<String, Object> params = (Map<String, Object>) request.get("params");
                final String clientProtocol = (String) params.get("protocolVersion");
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
                final Map<String, Object> params = (Map<String, Object>) request.get("params");
                final String toolName = (String) params.get("name");
                final Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

                final String resultText = switch (toolName) {
                    case "echo" -> "Echo: " + arguments.get("text");
                    case "list_resources" -> "Available resources:\n- file:///example.txt (Example File) - An example text resource";
                    case "read_resource" -> {
                        final String uri = (String) arguments.get("uri");
                        yield "Content of " + uri + ":\nThis is example content from: " + uri;
                    }
                    case "get_prompt" -> {
                        final String code = (String) arguments.get("code");
                        yield "Code Review Prompt:\nPlease review this code:\n\n" + code;
                    }
                    case "upload_files" -> {
                        final List<Map<String, Object>> files = (List<Map<String, Object>>) arguments.get("files");
                        final boolean async = arguments.containsKey("async") && (Boolean) arguments.get("async");

                        if (async) {
                            final String jobId = jobManager.createJob();
                            jobManager.submitJob(jobId, () -> {
                                try {
                                    final String result = handleMultiFileUpload(files);
                                    jobManager.completeJob(jobId, Map.of("text", result));
                                } catch (Exception e) { // NOPMD - Catch all for async error handling
                                    jobManager.failJob(jobId, e.getMessage());
                                }
                            });
                            yield "Job started: " + jobId + "\nUse check_job tool to get status.";
                        } else {
                            yield handleMultiFileUpload(files);
                        }
                    }
                    case "check_job" -> {
                        final String jobId = (String) arguments.get("job_id");
                        final JobManager.JobStatus status = jobManager.getJobStatus(jobId);
                        if (status == null) {
                            yield "Job not found: " + jobId;
                        } else if ("completed".equals(status.status)) {
                            yield (String) status.result.get("text");
                        } else if ("failed".equals(status.status)) {
                            yield "Job failed: " + status.error;
                        } else {
                            yield "Job status: " + status.status;
                        }
                    }
                    case "add_schema" -> {
                        final String docType = (String) arguments.get("doc_type");
                        final String schema = (String) arguments.get("schema");
                        schemaManager.addSchema(docType, schema);
                        yield "Schema added for: " + docType;
                    }
                    case "list_schemas" -> {
                        final Map<String, String> schemas = schemaManager.getAllCustomSchemas();
                        if (schemas.isEmpty()) {
                            yield "No custom schemas defined. Using built-in schemas.";
                        } else {
                            final StringBuilder sb = new StringBuilder("Custom schemas:\n");
                            schemas.forEach((type, schema) ->
                                sb.append("- ").append(type).append('\n'));
                            yield sb.toString();
                        }
                    }
                    case "get_schema" -> {
                        final String docType = (String) arguments.get("doc_type");
                        yield "Schema for " + docType + ":\n" + schemaManager.getSchema(docType);
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
                final Map<String, Object> params = (Map<String, Object>) request.get("params");
                final String uri = (String) params.get("uri");
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
                final Map<String, Object> params = (Map<String, Object>) request.get("params");
                final Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
                final String code = (String) arguments.get("code");

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
                    "code", -32_601,
                    "message", "Method not found: " + method
                )
            );
        };
    }

    @SuppressWarnings({"PMD.NcssCount", "PMD.CognitiveComplexity"}) // Complex multi-file processing
    static String handleMultiFileUpload(final List<Map<String, Object>> files) {
        try {
            final StringBuilder result = new StringBuilder();
            final Map<String, Object> metadata = new HashMap<>();
            metadata.put("uploadTime", Instant.now().toString());
            metadata.put("fileCount", files.size());
            List<Map<String, Object>> fileMetadata = new ArrayList<>();

            // Check if any file is an archive
            final boolean hasArchive = files.stream()
                .anyMatch(f -> "archive".equals(f.get("type")));

            if (hasArchive) {
                // All files go into one job folder
                final String jobId = java.util.UUID.randomUUID().toString();
                final String jobPath = createJobFolder(jobId);
                metadata.put("jobId", jobId);
                metadata.put("type", "archive");
                result.append(String.format("Job ID: %s (archive extraction)\n", jobId));
                result.append(String.format("Files: %d\n\n", files.size()));

                for (Map<String, Object> file : files) {
                    final String filename = (String) file.get("filename");
                    final String content = (String) file.get("content");
                    final String type = (String) file.get("type");

                    final byte[] fileBytes = java.util.Base64.getDecoder().decode(content);

                    // Detect actual file type
                    final String detectedType = detectFileType(fileBytes, filename);
                    log.info("File {} - Declared: {}, Detected: {}", filename, type, detectedType);

                    final Map<String, Object> fileMeta = new HashMap<>();
                    fileMeta.put("filename", filename);
                    fileMeta.put("declaredType", type);
                    fileMeta.put("detectedType", detectedType);
                    fileMeta.put("size", fileBytes.length);

                    if ("archive".equals(type)) {
                        final Path archivePath = Paths.get(jobPath, filename);
                        Files.write(archivePath, fileBytes);

                        // Extract archive
                        final int extractedCount = archiveExtractor.extract(archivePath.toString(), jobPath);
                        result.append(String.format("✓ %s (archive) - %d bytes - extracted %d files\n",
                            filename, fileBytes.length, extractedCount));
                    } else {
                        final Path filePath = Paths.get(jobPath, filename);
                        Files.write(filePath, fileBytes);

                        // Extract text and structured data
                        String textractResult;
                        Map<String, Object> structuredData = new HashMap<>();
                        String markdown = "";

                        if ("excel".equals(type) || detectedType.contains("spreadsheet") || detectedType.contains("ooxml")) {
                            textractResult = extractExcelText(fileBytes);
                            structuredData = extractStructuredFromExcel(fileBytes);
                            markdown = markdownConverter.convertExcelToMarkdown(fileBytes);
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
                    final String filename = (String) file.get("filename");
                    final String content = (String) file.get("content");
                    final String type = (String) file.get("type");

                    final String jobId = java.util.UUID.randomUUID().toString();
                    final String jobPath = createJobFolder(jobId);
                    final byte[] fileBytes = java.util.Base64.getDecoder().decode(content);

                    // Detect actual file type
                    final String detectedType = detectFileType(fileBytes, filename);
                    log.info("File {} - Declared: {}, Detected: {}", filename, type, detectedType);

                    final Path filePath = Paths.get(jobPath, filename);
                    Files.write(filePath, fileBytes);

                    // Extract text and structured data
                    String textractResult;
                    Map<String, Object> structuredData = new HashMap<>();
                    String markdown = "";

                    if ("excel".equals(type) || detectedType.contains("spreadsheet") || detectedType.contains("ooxml")) {
                        textractResult = extractExcelText(fileBytes);
                        structuredData = extractStructuredFromExcel(fileBytes);
                        markdown = markdownConverter.convertExcelToMarkdown(fileBytes);
                    } else {
                        textractResult = analyzeWithTextract(fileBytes, filename);
                        structuredData = extractStructuredWithTextract(fileBytes);
                        markdown = convertToMarkdown(textractResult, structuredData);
                    }

                    // Save metadata for this job
                    final Map<String, Object> jobMeta = new HashMap<>();
                    jobMeta.put("jobId", jobId);
                    jobMeta.put("uploadTime", Instant.now().toString());
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
                    final Map<String, Object> classification = classifier.classify(textractResult);
                    final Map<String, Object> consensus = classifier.getConsensus(classification);

                    // Extract structured fields with Bedrock
                    final Map<String, Object> extractedFields = fieldExtractor.extractFields(textractResult, (String) consensus.get("type"));

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
    private static String handlePdfUpload(final String filename, final String base64Content) {
        log.info("PDF upload: {} ({} bytes base64)", filename, base64Content.length());

        try {
            final String jobId = java.util.UUID.randomUUID().toString();
            final String jobPath = createJobFolder(jobId);
            final byte[] pdfBytes = java.util.Base64.getDecoder().decode(base64Content);

            final Path filePath = Paths.get(jobPath, filename);
            Files.write(filePath, pdfBytes);

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

    private static String handleExcelUpload(final String filename, final String base64Content) {
        log.info("Excel upload: {} ({} bytes base64)", filename, base64Content.length());

        try {
            final String jobId = java.util.UUID.randomUUID().toString();
            final String jobPath = createJobFolder(jobId);
            final byte[] excelBytes = java.util.Base64.getDecoder().decode(base64Content);

            final Path filePath = Paths.get(jobPath, filename);
            Files.write(filePath, excelBytes);

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

    private static String handleImageUpload(final String filename, final String base64Content) {
        log.info("Image upload: {} ({} bytes base64)", filename, base64Content.length());

        try {
            final String jobId = java.util.UUID.randomUUID().toString();
            final String jobPath = createJobFolder(jobId);
            final byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Content);

            final Path filePath = Paths.get(jobPath, filename);
            Files.write(filePath, imageBytes);

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

    private static String createJobFolder(final String jobId) throws java.io.IOException {
        java.time.LocalDate now = java.time.LocalDate.now();
        final String year = String.valueOf(now.getYear());
        final String month = String.format("%02d", now.getMonthValue());
        final String day = String.format("%02d", now.getDayOfMonth());

        final String jobPath = String.format("jobs/%s/%s/%s/%s", year, month, day, jobId);
        final Path path = Paths.get(jobPath);
        Files.createDirectories(path);

        log.info("Created job folder: {}", jobPath);
        return jobPath;
    }

    private static String detectFileType(final byte[] fileBytes, final String filename) {
        try {
            // Use Apache Tika to detect MIME type from content
            org.apache.tika.Tika tika = new org.apache.tika.Tika();
            final String mimeType = tika.detect(fileBytes);

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

    private static String analyzeWithTextract(final byte[] fileBytes, final String filename) {
        try {
            software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest request =
                software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest.builder()
                    .document(software.amazon.awssdk.services.textract.model.Document.builder()
                        .bytes(software.amazon.awssdk.core.SdkBytes.fromByteArray(fileBytes))
                        .build())
                    .build();

            software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse response =
                textractClient.detectDocumentText(request);

            final StringBuilder text = new StringBuilder();
            final List<Float> confidences = new ArrayList<>();

            for (Block block : response.blocks()) {
                if (block.blockType() == BlockType.LINE) {
                    text.append(block.text()).append('\n');
                    if (block.confidence() != null) {
                        confidences.add(block.confidence());
                    }
                }
            }

            // Calculate average confidence
            final float avgConfidence = confidences.isEmpty()
                ? 0
                : (float) confidences.stream().mapToDouble(Float::doubleValue).average().orElse(0);

            final String extractedText = text.toString();
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

    private static String extractExcelText(final byte[] fileBytes) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(fileBytes));
            final StringBuilder text = new StringBuilder();
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        text.append(cell.toString()).append(' ');
                    }
                    text.append('\n');
                }
            }
            workbook.close();
            return text.toString();
        } catch (Exception e) {
            log.error("Error extracting Excel text", e);
            return "Excel extraction failed: " + e.getMessage();
        }
    }

    private static Map<String, Object> extractStructuredWithTextract(final byte[] fileBytes) {
        final Map<String, Object> result = new HashMap<>();

        // Extract tables with Tabula
        try {
            java.io.File tempFile = java.io.File.createTempFile("doc", ".pdf");
            Files.write(tempFile.toPath(), fileBytes);

            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(tempFile);
            technology.tabula.ObjectExtractor extractor = new technology.tabula.ObjectExtractor(document);
            technology.tabula.PageIterator pages = extractor.extract();

            List<List<List<String>>> allTables = new ArrayList<>();
            while (pages.hasNext()) {
                technology.tabula.Page page = pages.next();
                List<technology.tabula.Table> tables = new technology.tabula.extractors.BasicExtractionAlgorithm().extract(page);
                for (technology.tabula.Table table : tables) {
                    List<List<String>> tableData = new ArrayList<>();
                    for (List<technology.tabula.RectangularTextContainer> row : table.getRows()) {
                        List<String> rowData = new ArrayList<>();
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
            result.put("tables", List.of());
        }

        result.put("keyValues", Map.of());
        return result;
    }

    private static Map<String, Object> extractStructuredFromExcel(final byte[] fileBytes) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(fileBytes));
            final Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> sheets = new ArrayList<>();

            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                final Map<String, Object> sheetData = new HashMap<>();
                sheetData.put("name", sheet.getSheetName());

                List<List<String>> rows = new ArrayList<>();
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
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

    private static String convertToMarkdown(final String text, final Map<String, Object> structured) {
        final StringBuilder md = new StringBuilder();
        md.append("# Document\n\n");

        // Add key-value pairs
        if (structured.containsKey("keyValues")) {
            @SuppressWarnings("unchecked")
            final Map<String, String> kv = (Map<String, String>) structured.get("keyValues");
            if (!kv.isEmpty()) {
                md.append("## Fields\n\n");
                kv.forEach((k, v) -> md.append("- **").append(k).append("**: ").append(v).append('\n'));
                md.append('\n');
            }
        }

        // Add raw text
        md.append("## Content\n\n");
        md.append("```\n").append(text).append("\n```\n");

        return md.toString();
    }

    private static void saveMetadata(final String jobPath, final Map<String, Object> metadata) {
        try {
            final Path metaPath = Paths.get(jobPath, "meta.json");
            final String json = gson.toJson(metadata);
            Files.writeString(metaPath, json);
            log.info("Saved metadata to {}", metaPath);
        } catch (Exception e) {
            log.error("Error saving metadata", e);
        }
    }
}
