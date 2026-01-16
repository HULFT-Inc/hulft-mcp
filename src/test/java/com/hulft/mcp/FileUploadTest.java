package com.hulft.mcp;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FileUploadTest {

    @Test
    public void testUploadAllPDFs() throws Exception {
        List<Map<String, Object>> files = new ArrayList<>();
        
        // Load all test PDFs
        String[] pdfFiles = {
            "schedule_production.pdf",
            "invoice_production.pdf",
            "purchase_order.pdf",
            "customs_declaration.pdf"
        };
        
        for (String filename : pdfFiles) {
            byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs", filename));
            String base64 = Base64.getEncoder().encodeToString(pdfBytes);
            
            Map<String, Object> file = new HashMap<>();
            file.put("filename", filename);
            file.put("content", base64);
            file.put("type", "pdf");
            files.add(file);
            
            System.out.println("Loaded: " + filename + " (" + pdfBytes.length + " bytes)");
        }
        
        // Simulate upload
        String result = MCPServer.handleMultiFileUpload(files);
        System.out.println("\n=== Upload Result ===");
        System.out.println(result);
    }
    
    @Test
    public void testUploadSinglePDF() throws Exception {
        byte[] pdfBytes = Files.readAllBytes(Paths.get("test-pdfs", "invoice_production.pdf"));
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        
        Map<String, Object> file = new HashMap<>();
        file.put("filename", "invoice_production.pdf");
        file.put("content", base64);
        file.put("type", "pdf");
        
        List<Map<String, Object>> files = List.of(file);
        String result = MCPServer.handleMultiFileUpload(files);
        
        System.out.println("=== Single Upload Result ===");
        System.out.println(result);
    }
}
