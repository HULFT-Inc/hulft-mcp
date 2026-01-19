package com.hulft.mcp;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles document classification using multiple methods and consensus voting.
 */
@Slf4j
public class DocumentClassifier {
    private static final Gson gson = new Gson();
    
    private final software.amazon.awssdk.services.comprehend.ComprehendClient comprehendClient;
    private final software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient;
    
    public DocumentClassifier(
            software.amazon.awssdk.services.comprehend.ComprehendClient comprehendClient,
            software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient) {
        this.comprehendClient = comprehendClient;
        this.bedrockClient = bedrockClient;
    }
    
    public java.util.Map<String, Object> classify(String text) {
        java.util.Map<String, Object> classification = new java.util.HashMap<>();
        
        // Method 1: Regex extraction
        String regexType = extractPurposeCode(text);
        classification.put("regex", java.util.Map.of("type", regexType, "confidence", regexType.equals("UNKNOWN") ? 0.0 : 1.0));
        
        // Method 2: AWS Comprehend
        try {
            java.util.Map<String, Object> comprehendResult = classifyWithComprehend(text);
            classification.put("comprehend", comprehendResult);
        } catch (Exception e) {
            log.error("Comprehend classification failed", e);
            classification.put("comprehend", java.util.Map.of("error", e.getMessage()));
        }
        
        // Method 3: AWS Bedrock (Claude)
        try {
            java.util.Map<String, Object> bedrockResult = classifyWithBedrock(text);
            classification.put("bedrock", bedrockResult);
        } catch (Exception e) {
            log.error("Bedrock classification failed", e);
            classification.put("bedrock", java.util.Map.of("error", e.getMessage()));
        }
        
        return classification;
    }
    
    public java.util.Map<String, Object> getConsensus(java.util.Map<String, Object> classification) {
        java.util.Map<String, Object> regex = (java.util.Map<String, Object>) classification.get("regex");
        java.util.Map<String, Object> comprehend = (java.util.Map<String, Object>) classification.getOrDefault("comprehend", java.util.Map.of());
        java.util.Map<String, Object> bedrock = (java.util.Map<String, Object>) classification.getOrDefault("bedrock", java.util.Map.of());
        
        String regexType = (String) regex.getOrDefault("type", "UNKNOWN");
        String comprehendType = (String) comprehend.getOrDefault("type", "UNKNOWN");
        String bedrockType = (String) bedrock.getOrDefault("type", "UNKNOWN");
        
        double regexConf = regex.containsKey("confidence") ? ((Number) regex.get("confidence")).doubleValue() : 0.0;
        double comprehendConf = comprehend.containsKey("confidence") ? ((Number) comprehend.get("confidence")).doubleValue() : 0.0;
        double bedrockConf = bedrock.containsKey("confidence") ? ((Number) bedrock.get("confidence")).doubleValue() : 0.0;
        
        // Voting: if 2+ agree, use that
        if (regexType.equals(comprehendType) || regexType.equals(bedrockType)) {
            return java.util.Map.of(
                "type", regexType,
                "confidence", Math.max(regexConf, Math.max(comprehendConf, bedrockConf)),
                "method", "consensus"
            );
        }
        
        if (comprehendType.equals(bedrockType) && !comprehendType.equals("UNKNOWN")) {
            return java.util.Map.of(
                "type", comprehendType,
                "confidence", (comprehendConf + bedrockConf) / 2,
                "method", "consensus"
            );
        }
        
        // No consensus - use highest confidence
        if (regexConf >= comprehendConf && regexConf >= bedrockConf) {
            return java.util.Map.of("type", regexType, "confidence", regexConf, "method", "regex");
        } else if (bedrockConf >= comprehendConf) {
            return java.util.Map.of("type", bedrockType, "confidence", bedrockConf, "method", "bedrock");
        } else {
            return java.util.Map.of("type", comprehendType, "confidence", comprehendConf, "method", "comprehend");
        }
    }
    
    private String extractPurposeCode(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Purpose:\\s*([A-Z_]+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "UNKNOWN";
    }
    
    private java.util.Map<String, Object> classifyWithComprehend(String text) {
        software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest request = 
            software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest.builder()
                .text(text.substring(0, Math.min(5000, text.length())))
                .languageCode("en")
                .build();
        
        software.amazon.awssdk.services.comprehend.model.DetectEntitiesResponse response = 
            comprehendClient.detectEntities(request);
        
        // Simple heuristic based on entities
        long quantityCount = response.entities().stream()
            .filter(e -> e.type() == software.amazon.awssdk.services.comprehend.model.EntityType.QUANTITY)
            .count();
        
        String docType = quantityCount > 2 ? "SCHEDULE_PRODUCTION" : "UNKNOWN";
        return java.util.Map.of("type", docType, "confidence", 0.5);
    }
    
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private java.util.Map<String, Object> classifyWithBedrock(String text) {
        String prompt = String.format(
            "Classify this document as exactly one of: SCHEDULE_PRODUCTION, INVOICE_PRODUCTION, PURCHASE_ORDER, CUSTOMS_DECLARATION\n\n" +
            "Guidelines:\n" +
            "- INVOICE_PRODUCTION: Contains invoice number, customer, items, amounts, total\n" +
            "- PURCHASE_ORDER: Contains PO number, vendor, items to purchase, delivery info\n" +
            "- SCHEDULE_PRODUCTION: Contains production schedule, quantities, dates, line assignments\n" +
            "- CUSTOMS_DECLARATION: Contains customs info, origin, destination, declared items\n\n" +
            "Document text:\n%s\n\nRespond with ONLY the classification type.",
            text.substring(0, Math.min(1000, text.length()))
        );
        
        String requestBody = gson.toJson(java.util.Map.of(
            "anthropic_version", "bedrock-2023-05-31",
            "max_tokens", 50,
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
        String classification = (String) content.get(0).get("text");
        
        classification = classification.trim().toUpperCase();
        return java.util.Map.of("type", classification, "confidence", 0.95);
    }
}
