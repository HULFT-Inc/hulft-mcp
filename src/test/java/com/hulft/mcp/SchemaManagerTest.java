package com.hulft.mcp;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class SchemaManagerTest {
    
    private final SchemaManager manager = new SchemaManager();
    
    @Test
    public void testAddAndGetSchema() {
        manager.addSchema("invoice", "{\"fields\": [\"amount\", \"date\"]}");
        final String schema = manager.getSchema("invoice");
        assertNotNull("Schema should not be null", schema);
        assertTrue("Schema should contain fields", schema.contains("fields"));
    }
    
    @Test
    public void testGetBuiltInSchema() {
        final String schema = manager.getSchema("INVOICE_PRODUCTION");
        assertNotNull("Built-in invoice schema should exist", schema);
        assertTrue("Schema should contain invoice_number", schema.contains("invoice_number"));
    }
    
    @Test
    public void testHasCustomSchema() {
        assertFalse("Should not have custom schema initially", manager.hasCustomSchema("custom"));
        manager.addSchema("custom", "{}");
        assertTrue("Should have custom schema after adding", manager.hasCustomSchema("custom"));
    }
    
    @Test
    public void testGetAllCustomSchemas() {
        manager.addSchema("schema1", "{}");
        manager.addSchema("schema2", "{}");
        final java.util.Map<String, String> schemas = manager.getAllCustomSchemas();
        assertNotNull("Schemas map should not be null", schemas);
        assertTrue("Should contain schema1", schemas.containsKey("schema1"));
        assertTrue("Should contain schema2", schemas.containsKey("schema2"));
    }
}
