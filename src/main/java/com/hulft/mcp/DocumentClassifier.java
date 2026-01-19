package com.hulft.mcp;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles document classification using multiple methods and consensus voting.
 */
@Slf4j
@SuppressWarnings({
    "PMD.FieldNamingConventions", // gson is an object instance, not primitive constant
    "PMD.AvoidDuplicateLiterals" // JSON keys used throughout classification
})
public class DocumentClassifier {
    private static final Gson gson = new Gson();
    private static final String UNKNOWN = "UNKNOWN";
    private static final String TYPE_KEY = "type";
    private static final String CONFIDENCE_KEY = "confidence";
    
    private final software.amazon.awssdk.services.comprehend.ComprehendClient comprehendClient;
    private final software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient;
    
    public DocumentClassifier(
            final software.amazon.awssdk.services.comprehend.ComprehendClient comprehendClient,
            final software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient bedrockClient) {
        this.comprehendClient = comprehendClient;
        this.bedrockClient = bedrockClient;
    }
    
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public java.util.Map<String, Object> classify(final String text) {
        final java.util.Map<String, Object> classification = new java.util.HashMap<>();
        
        // Method 1: Regex extraction
        final String regexType = extractPurposeCode(text);
        classification.put("regex", java.util.Map.of(TYPE_KEY, regexType, CONFIDENCE_KEY, UNKNOWN.equals(regexType) ? 0.0 : 1.0));
        
        // Method 2: AWS Comprehend
        try {
            final java.util.Map<String, Object> comprehendResult = classifyWithComprehend(text);
            classification.put("comprehend", comprehendResult);
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Comprehend classification failed", e);
            }
            classification.put("comprehend", java.util.Map.of("error", e.getMessage()));
        }
        
        // Method 3: AWS Bedrock (Claude)
        try {
            final java.util.Map<String, Object> bedrockResult = classifyWithBedrock(text);
            classification.put("bedrock", bedrockResult);
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Bedrock classification failed", e);
            }
            classification.put("bedrock", java.util.Map.of("error", e.getMessage()));
        }
        
        return classification;
    }
    
    @SuppressWarnings("PMD.NPathComplexity") // Consensus voting algorithm requires multiple conditions
    public java.util.Map<String, Object> getConsensus(final java.util.Map<String, Object> classification) {
        final java.util.Map<String, Object> regex = (java.util.Map<String, Object>) classification.get("regex");
        final java.util.Map<String, Object> comprehend = (java.util.Map<String, Object>) classification.getOrDefault("comprehend", java.util.Map.of());
        final java.util.Map<String, Object> bedrock = (java.util.Map<String, Object>) classification.getOrDefault("bedrock", java.util.Map.of());
        
        final String regexType = (String) regex.getOrDefault(TYPE_KEY, UNKNOWN);
        final String comprehendType = (String) comprehend.getOrDefault(TYPE_KEY, UNKNOWN);
        final String bedrockType = (String) bedrock.getOrDefault(TYPE_KEY, UNKNOWN);
        
        final double regexConf = regex.containsKey(CONFIDENCE_KEY) ? ((Number) regex.get(CONFIDENCE_KEY)).doubleValue() : 0.0;
        final double comprehendConf = comprehend.containsKey(CONFIDENCE_KEY) ? ((Number) comprehend.get(CONFIDENCE_KEY)).doubleValue() : 0.0;
        final double bedrockConf = bedrock.containsKey(CONFIDENCE_KEY) ? ((Number) bedrock.get(CONFIDENCE_KEY)).doubleValue() : 0.0;
        
        // Voting: if 2+ agree, use that
        if (regexType.equals(comprehendType) || regexType.equals(bedrockType)) {
            return java.util.Map.of(
                TYPE_KEY, regexType,
                CONFIDENCE_KEY, Math.max(regexConf, Math.max(comprehendConf, bedrockConf)),
                "method", "consensus"
            );
        }
        
        if (comprehendType.equals(bedrockType) && !UNKNOWN.equals(comprehendType)) {
            return java.util.Map.of(
                TYPE_KEY, comprehendType,
                CONFIDENCE_KEY, (comprehendConf + bedrockConf) / 2,
                "method", "consensus"
            );
        }
        
        // No consensus - use highest confidence
        if (regexConf >= comprehendConf && regexConf >= bedrockConf) {
            return java.util.Map.of(TYPE_KEY, regexType, CONFIDENCE_KEY, regexConf, "method", "regex");
        } else if (bedrockConf >= comprehendConf) {
            return java.util.Map.of(TYPE_KEY, bedrockType, CONFIDENCE_KEY, bedrockConf, "method", "bedrock");
        } else {
            return java.util.Map.of(TYPE_KEY, comprehendType, CONFIDENCE_KEY, comprehendConf, "method", "comprehend");
        }
    }
    
    private String extractPurposeCode(final String text) {
        final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Purpose:\\s*([A-Z_]+)");
        final java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return UNKNOWN;
    }
    
    private java.util.Map<String, Object> classifyWithComprehend(final String text) {
        final software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest request = 
            software.amazon.awssdk.services.comprehend.model.DetectEntitiesRequest.builder()
                .text(text.substring(0, Math.min(5000, text.length())))
                .languageCode("en")
                .build();
        
        final software.amazon.awssdk.services.comprehend.model.DetectEntitiesResponse response = 
            comprehendClient.detectEntities(request);
        
        // Simple heuristic based on entities
        final long quantityCount = response.entities().stream()
            .filter(e -> e.type() == software.amazon.awssdk.services.comprehend.model.EntityType.QUANTITY)
            .count();
        
        final String docType = quantityCount > 2 ? "SCHEDULE_PRODUCTION" : UNKNOWN;
        return java.util.Map.of(TYPE_KEY, docType, CONFIDENCE_KEY, 0.5);
    }
    
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private java.util.Map<String, Object> classifyWithBedrock(final String text) {
        final String prompt = String.format(
            "Classify this document as exactly one of: SCHEDULE_PRODUCTION, INVOICE_PRODUCTION, PURCHASE_ORDER, CUSTOMS_DECLARATION\n\n" +
            "Guidelines:\n" +
            "- INVOICE_PRODUCTION: Contains invoice number, customer, items, amounts, total\n" +
            "- PURCHASE_ORDER: Contains PO number, vendor, items to purchase, delivery info\n" +
            "- SCHEDULE_PRODUCTION: Contains production schedule, quantities, dates, line assignments\n" +
            "- CUSTOMS_DECLARATION: Contains customs info, origin, destination, declared items\n\n" +
            "Document text:\n%s\n\nRespond with ONLY the classification type.",
            text.substring(0, Math.min(1000, text.length()))
        );
        
        final String requestBody = gson.toJson(java.util.Map.of(
            "anthropic_version", "bedrock-2023-05-31",
            "max_tokens", 50,
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
        String classification = (String) content.get(0).get("text");
        
        classification = classification.trim().toUpperCase(java.util.Locale.ROOT);
        return java.util.Map.of(TYPE_KEY, classification, CONFIDENCE_KEY, 0.95);
    }
}
