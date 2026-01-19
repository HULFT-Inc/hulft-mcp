package com.hulft.mcp;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

/**
 * Extracts structured fields from documents using AWS Bedrock.
 */
@Slf4j
public class FieldExtractor {
    private static final Gson gson = new Gson();
    
    private final software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient;
    private final SchemaManager schemaManager;
    
    public FieldExtractor(
            software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient,
            SchemaManager schemaManager) {
        this.bedrockClient = bedrockClient;
        this.schemaManager = schemaManager;
    }
    
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public java.util.Map<String, Object> extractFields(String text, String docType) {
        try {
            String schema = schemaManager.getSchema(docType);
            String prompt = String.format(
                "Extract fields from this document and return ONLY a JSON object (no markdown, no explanation).\n\nSchema:\n%s\n\nDocument:\n%s\n\nJSON:",
                schema, text.substring(0, Math.min(2000, text.length()))
            );
            
            String requestBody = gson.toJson(java.util.Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 1000,
                "messages", java.util.List.of(java.util.Map.of(
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
            java.util.Map<String, Object> responseMap = gson.fromJson(responseBody, java.util.Map.class);
            java.util.List<java.util.Map<String, Object>> content = (java.util.List<java.util.Map<String, Object>>) responseMap.get("content");
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
            
            return gson.fromJson(extractedText, java.util.Map.class);
            
        } catch (Exception e) {
            log.error("Bedrock field extraction failed: {}", e.getMessage());
            return java.util.Map.of("error", e.getMessage());
        }
    }
}
