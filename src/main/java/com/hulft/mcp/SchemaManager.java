package com.hulft.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages custom extraction schemas for different document types.
 */
public class SchemaManager {
    private final Map<String, String> customSchemas = new ConcurrentHashMap<>();
    
    public void addSchema(final String docType, final String schema) {
        customSchemas.put(docType, schema);
    }
    
    public String getSchema(final String docType) {
        if (customSchemas.containsKey(docType)) {
            return customSchemas.get(docType);
        }
        return getBuiltInSchema(docType);
    }
    
    public boolean hasCustomSchema(final String docType) {
        return customSchemas.containsKey(docType);
    }
    
    public Map<String, String> getAllCustomSchemas() {
        return new ConcurrentHashMap<>(customSchemas);
    }
    
    private String getBuiltInSchema(final String docType) {
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
