package com.hulft.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages custom extraction schemas for different document types.
 * Provides storage and retrieval of JSON schemas used for field extraction,
 * with built-in schemas for common document types.
 */
public class SchemaManager {
    private final Map<String, String> customSchemas = new ConcurrentHashMap<>();
    
    /**
     * Adds or updates a custom schema for a document type.
     *
     * @param docType the document type identifier
     * @param schema the JSON schema definition
     */
    public void addSchema(final String docType, final String schema) {
        customSchemas.put(docType, schema);
    }
    
    /**
     * Retrieves the schema for a document type.
     * Returns custom schema if available, otherwise returns built-in schema.
     *
     * @param docType the document type identifier
     * @return JSON schema string, or "{}" if no schema exists
     */
    public String getSchema(final String docType) {
        if (customSchemas.containsKey(docType)) {
            return customSchemas.get(docType);
        }
        return getBuiltInSchema(docType);
    }
    
    /**
     * Checks if a custom schema exists for the document type.
     *
     * @param docType the document type identifier
     * @return true if custom schema exists, false otherwise
     */
    public boolean hasCustomSchema(final String docType) {
        return customSchemas.containsKey(docType);
    }
    
    /**
     * Returns a copy of all custom schemas.
     *
     * @return map of document types to their custom schemas
     */
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
