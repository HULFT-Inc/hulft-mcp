package com.hulft.mcp;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

/**
 * Extracts structured fields from documents using AWS Bedrock.
 */
@Slf4j
public class FieldExtractor {
    @SuppressWarnings("PMD.FieldNamingConventions") // Gson instance, not primitive constant
    private static final Gson gson = new Gson();
    
    private final software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient;
    private final SchemaManager schemaManager;
    
    public FieldExtractor(
            final software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient,
            final SchemaManager schemaManager) {
        this.bedrockClient = bedrockClient;
        this.schemaManager = schemaManager;
    }
    
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public java.util.Map<String, Object> extractFields(final String text, final String docType) {
        try {
            final String schema = schemaManager.getSchema(docType);
            final String prompt = String.format(
                "Extract fields from this document and return ONLY a JSON object (no markdown, no explanation).\n\nSchema:\n%s\n\nDocument:\n%s\n\nJSON:",
                schema, text.substring(0, Math.min(2000, text.length()))
            );
            
            final String requestBody = gson.toJson(java.util.Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 1000,
                "messages", java.util.List.of(java.util.Map.of(
                    "role", "user",
                    "content", prompt
                ))
            ));
            
            final software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest request = 
                software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest.builder()
                    .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                    .body(software.amazon.awssdk.core.SdkBytes.fromUtf8String(requestBody))
                    .build();
            
            final software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse response = bedrockClient.invokeModel(request);
            final String responseBody = response.body().asUtf8String();
            final java.util.Map<String, Object> responseMap = gson.fromJson(responseBody, java.util.Map.class);
            final java.util.List<java.util.Map<String, Object>> content = (java.util.List<java.util.Map<String, Object>>) responseMap.get("content");
            String extractedText = (String) content.get(0).get("text");
            
            // Extract JSON from response (handle markdown code blocks)
            extractedText = extractedText.trim();
            if (extractedText.startsWith("```")) {
                final int start = extractedText.indexOf('{');
                final int end = extractedText.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    extractedText = extractedText.substring(start, end + 1);
                }
            }
            
            // Find first { and last }
            final int jsonStart = extractedText.indexOf('{');
            final int jsonEnd = extractedText.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                extractedText = extractedText.substring(jsonStart, jsonEnd + 1);
            }
            
            return gson.fromJson(extractedText, java.util.Map.class);
            
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Bedrock field extraction failed: {}", e.getMessage());
            }
            return java.util.Map.of("error", e.getMessage());
        }
    }
}
