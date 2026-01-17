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
            "customs_declaration.pdf",
            "complex_invoice.pdf",
            "invoice_no_purpose.pdf",
            "ambiguous_doc.pdf",
            "multipage_po.pdf"
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
    
    @Test
    public void testUploadExcelAndImages() throws Exception {
        List<Map<String, Object>> files = new ArrayList<>();
        
        // Excel files
        addFile(files, "test-files/invoice.xlsx", "excel");
        addFile(files, "test-files/purchase_order.xlsx", "excel");
        
        // Image files
        addFile(files, "test-files/invoice.png", "image");
        addFile(files, "test-files/schedule.png", "image");
        
        String result = MCPServer.handleMultiFileUpload(files);
        System.out.println("=== Excel & Image Upload Result ===");
        System.out.println(result);
    }
    
    private void addFile(List<Map<String, Object>> files, String path, String type) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String base64 = Base64.getEncoder().encodeToString(bytes);
        
        Map<String, Object> file = new HashMap<>();
        file.put("filename", Paths.get(path).getFileName().toString());
        file.put("content", base64);
        file.put("type", type);
        files.add(file);
        
        System.out.println("Loaded: " + path + " (" + bytes.length + " bytes)");
    }
}
