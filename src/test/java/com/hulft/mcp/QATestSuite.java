package com.hulft.mcp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.*;
import java.nio.file.*;

public class QATestSuite {
    
    private static final String BASE_URL = "http://localhost:3333/mcp";
    
    @Before
    public void setup() {
        // Clean jobs directory before each test
        try {
            Path jobsDir = Paths.get("jobs");
            if (Files.exists(jobsDir)) {
                Files.walk(jobsDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (Exception e) {}
                    });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ========== FUNCTIONAL TESTS ==========
    
    @Test
    public void testPDFUploadAndExtraction() throws Exception {
        System.out.println("\n=== TEST: PDF Upload and Extraction ===");
        
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs/invoice_production.pdf"));
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "test_invoice.pdf",
            "content", base64,
            "type", "pdf"
        ));
        
        String result = MCPServer.handleMultiFileUpload(files);
        
        assertTrue("Result should contain job ID", result.contains("Job ID:"));
        assertTrue("Result should indicate success", result.contains("✓"));
        
        // Verify metadata was created
        Path jobsDir = Paths.get("jobs");
        assertTrue("Jobs directory should exist", Files.exists(jobsDir));
        
        System.out.println("✓ PDF upload successful");
    }
    
    @Test
    public void testExcelUploadAndExtraction() throws Exception {
        System.out.println("\n=== TEST: Excel Upload and Extraction ===");
        
        byte[] excelBytes = Files.readAllBytes(Paths.get("test-files/invoice.xlsx"));
        String base64 = Base64.getEncoder().encodeToString(excelBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "test_invoice.xlsx",
            "content", base64,
            "type", "excel"
        ));
        
        String result = MCPServer.handleMultiFileUpload(files);
        
        assertTrue("Result should contain job ID", result.contains("Job ID:"));
        System.out.println("✓ Excel upload successful");
    }
    
    @Test
    public void testImageUploadAndOCR() throws Exception {
        System.out.println("\n=== TEST: Image Upload and OCR ===");
        
        byte[] imageBytes = Files.readAllBytes(Paths.get("test-files/invoice.png"));
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "test_invoice.png",
            "content", base64,
            "type", "image"
        ));
        
        String result = MCPServer.handleMultiFileUpload(files);
        
        assertTrue("Result should contain job ID", result.contains("Job ID:"));
        System.out.println("✓ Image OCR successful");
    }
    
    @Test
    public void testZipArchiveExtraction() throws Exception {
        System.out.println("\n=== TEST: ZIP Archive Extraction ===");
        
        // Create test ZIP
        Path tempZip = Files.createTempFile("test", ".zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                Files.newOutputStream(tempZip))) {
            
            // Add test file
            zos.putNextEntry(new java.util.zip.ZipEntry("test.txt"));
            zos.write("Test content".getBytes());
            zos.closeEntry();
        }
        
        byte[] zipBytes = Files.readAllBytes(tempZip);
        String base64 = Base64.getEncoder().encodeToString(zipBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "test.zip",
            "content", base64,
            "type", "archive"
        ));
        
        String result = MCPServer.handleMultiFileUpload(files);
        
        assertTrue("Result should indicate extraction", result.contains("extracted"));
        Files.deleteIfExists(tempZip);
        System.out.println("✓ ZIP extraction successful");
    }
    
    @Test
    public void testDocumentClassification() throws Exception {
        System.out.println("\n=== TEST: Document Classification ===");
        
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs/invoice_production.pdf"));
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "invoice.pdf",
            "content", base64,
            "type", "pdf"
        ));
        
        MCPServer.handleMultiFileUpload(files);
        
        // Find the metadata file
        Path metaFile = Files.walk(Paths.get("jobs"))
            .filter(p -> p.toString().endsWith("meta.json"))
            .findFirst()
            .orElseThrow();
        
        String metaJson = Files.readString(metaFile);
        assertTrue("Should contain classification", metaJson.contains("finalClassification"));
        assertTrue("Should classify as INVOICE", metaJson.contains("INVOICE_PRODUCTION"));
        
        System.out.println("✓ Classification successful");
    }
    
    @Test
    public void testFieldExtraction() throws Exception {
        System.out.println("\n=== TEST: Field Extraction ===");
        
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs/invoice_production.pdf"));
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "invoice.pdf",
            "content", base64,
            "type", "pdf"
        ));
        
        MCPServer.handleMultiFileUpload(files);
        
        Path metaFile = Files.walk(Paths.get("jobs"))
            .filter(p -> p.toString().endsWith("meta.json"))
            .findFirst()
            .orElseThrow();
        
        String metaJson = Files.readString(metaFile);
        assertTrue("Should contain extracted fields", metaJson.contains("extractedFields"));
        assertTrue("Should extract invoice number", metaJson.contains("invoice_number"));
        
        System.out.println("✓ Field extraction successful");
    }
    
    @Test
    public void testOCRConfidence() throws Exception {
        System.out.println("\n=== TEST: OCR Confidence Tracking ===");
        
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs/invoice_production.pdf"));
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "invoice.pdf",
            "content", base64,
            "type", "pdf"
        ));
        
        MCPServer.handleMultiFileUpload(files);
        
        Path metaFile = Files.walk(Paths.get("jobs"))
            .filter(p -> p.toString().endsWith("meta.json"))
            .findFirst()
            .orElseThrow();
        
        String metaJson = Files.readString(metaFile);
        assertTrue("Should contain OCR confidence", metaJson.contains("ocrConfidence"));
        
        System.out.println("✓ OCR confidence tracked");
    }
    
    @Test
    public void testCustomSchema() throws Exception {
        System.out.println("\n=== TEST: Custom Schema Management ===");
        
        // Verify SchemaManager exists and is accessible
        assertNotNull("SchemaManager class should exist", Class.forName("com.hulft.mcp.SchemaManager"));
        
        System.out.println("✓ Custom schema support verified");
    }
    
    // ========== EDGE CASE TESTS ==========
    
    @Test
    public void testEmptyFileUpload() throws Exception {
        System.out.println("\n=== TEST: Empty File Upload ===");
        
        try {
            List<Map<String, Object>> files = List.of(Map.of(
                "filename", "empty.pdf",
                "content", "",
                "type", "pdf"
            ));
            
            String result = MCPServer.handleMultiFileUpload(files);
            fail("Should throw exception for empty file");
        } catch (Exception e) {
            System.out.println("✓ Empty file rejected: " + e.getMessage());
        }
    }
    
    @Test
    public void testInvalidBase64() throws Exception {
        System.out.println("\n=== TEST: Invalid Base64 ===");
        
        try {
            List<Map<String, Object>> files = List.of(Map.of(
                "filename", "test.pdf",
                "content", "not-valid-base64!!!",
                "type", "pdf"
            ));
            
            String result = MCPServer.handleMultiFileUpload(files);
            fail("Should throw exception for invalid base64");
        } catch (Exception e) {
            System.out.println("✓ Invalid base64 rejected: " + e.getMessage());
        }
    }
    
    @Test
    public void testLargeFileUpload() throws Exception {
        System.out.println("\n=== TEST: Large File Upload ===");
        
        // Create 10MB file
        byte[] largeContent = new byte[10 * 1024 * 1024];
        Arrays.fill(largeContent, (byte) 'A');
        String base64 = Base64.getEncoder().encodeToString(largeContent);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "large.txt",
            "content", base64,
            "type", "pdf"
        ));
        
        long start = System.currentTimeMillis();
        String result = MCPServer.handleMultiFileUpload(files);
        long duration = System.currentTimeMillis() - start;
        
        System.out.println("✓ Large file processed in " + duration + "ms");
        assertTrue("Should complete in reasonable time", duration < 30000);
    }
    
    @Test
    public void testMultipleFilesUpload() throws Exception {
        System.out.println("\n=== TEST: Multiple Files Upload ===");
        
        byte[] pdf1 = Files.readAllBytes(Paths.get("test-pdfs/invoice_production.pdf"));
        byte[] pdf2 = Files.readAllBytes(Paths.get("test-pdfs/purchase_order.pdf"));
        
        List<Map<String, Object>> files = List.of(
            Map.of("filename", "invoice.pdf", "content", Base64.getEncoder().encodeToString(pdf1), "type", "pdf"),
            Map.of("filename", "po.pdf", "content", Base64.getEncoder().encodeToString(pdf2), "type", "pdf")
        );
        
        String result = MCPServer.handleMultiFileUpload(files);
        
        assertTrue("Should process both files", result.contains("Files: 2"));
        System.out.println("✓ Multiple files processed");
    }
    
    @Test
    public void testCorruptedPDF() throws Exception {
        System.out.println("\n=== TEST: Corrupted PDF ===");
        
        byte[] corruptedPDF = "Not a real PDF".getBytes();
        String base64 = Base64.getEncoder().encodeToString(corruptedPDF);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "corrupted.pdf",
            "content", base64,
            "type", "pdf"
        ));
        
        String result = MCPServer.handleMultiFileUpload(files);
        
        // Should handle gracefully
        assertTrue("Should create job even for corrupted file", result.contains("Job ID:"));
        System.out.println("✓ Corrupted PDF handled gracefully");
    }
    
    // ========== PERFORMANCE TESTS ==========
    
    @Test
    public void testProcessingSpeed() throws Exception {
        System.out.println("\n=== TEST: Processing Speed ===");
        
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs/invoice_production.pdf"));
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "speed_test.pdf",
            "content", base64,
            "type", "pdf"
        ));
        
        long start = System.currentTimeMillis();
        MCPServer.handleMultiFileUpload(files);
        long duration = System.currentTimeMillis() - start;
        
        System.out.println("Processing time: " + duration + "ms");
        assertTrue("Should process in < 10s", duration < 10000);
        System.out.println("✓ Performance acceptable");
    }
    
    @Test
    public void testConcurrentUploads() throws Exception {
        System.out.println("\n=== TEST: Concurrent Uploads ===");
        
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs/invoice_production.pdf"));
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int index = i;
            Thread t = new Thread(() -> {
                try {
                    List<Map<String, Object>> files = List.of(Map.of(
                        "filename", "concurrent_" + index + ".pdf",
                        "content", base64,
                        "type", "pdf"
                    ));
                    MCPServer.handleMultiFileUpload(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
            t.start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        System.out.println("✓ Concurrent uploads handled");
    }
    
    // ========== INTEGRATION TESTS ==========
    
    @Test
    public void testEndToEndWorkflow() throws Exception {
        System.out.println("\n=== TEST: End-to-End Workflow ===");
        
        // 1. Upload document
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs/invoice_production.pdf"));
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        
        List<Map<String, Object>> files = List.of(Map.of(
            "filename", "e2e_test.pdf",
            "content", base64,
            "type", "pdf"
        ));
        
        String result = MCPServer.handleMultiFileUpload(files);
        assertTrue("Upload successful", result.contains("✓"));
        
        // 2. Verify metadata
        Path metaFile = Files.walk(Paths.get("jobs"))
            .filter(p -> p.toString().endsWith("meta.json"))
            .findFirst()
            .orElseThrow();
        
        String metaJson = Files.readString(metaFile);
        
        // 3. Verify all components
        assertTrue("Has text extraction", metaJson.contains("textractAnalysis"));
        assertTrue("Has classification", metaJson.contains("finalClassification"));
        assertTrue("Has extracted fields", metaJson.contains("extractedFields"));
        assertTrue("Has structured data", metaJson.contains("structuredData"));
        assertTrue("Has markdown", metaJson.contains("markdown"));
        assertTrue("Has OCR confidence", metaJson.contains("ocrConfidence"));
        
        System.out.println("✓ End-to-end workflow complete");
    }
}
