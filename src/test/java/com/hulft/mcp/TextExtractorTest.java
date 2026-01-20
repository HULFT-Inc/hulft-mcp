package com.hulft.mcp;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextExtractorTest {
    
    private TextExtractor extractor;
    
    @Before
    public void setUp() {
        // Create with null client for basic tests
        extractor = new TextExtractor(null);
    }
    
    @Test
    public void testExtractFromExcel() {
        // Create minimal Excel file bytes
        final byte[] excelBytes = createMinimalExcelBytes();
        final String result = extractor.extractFromExcel(excelBytes);
        
        assertNotNull("Result should not be null", result);
        // Result can be empty string for empty workbook or contain content
        assertTrue("Result should be non-null", result != null);
    }
    
    @Test
    public void testExtractFromExcelInvalid() {
        final byte[] invalidBytes = "not an excel file".getBytes();
        final String result = extractor.extractFromExcel(invalidBytes);
        
        assertNotNull("Result should not be null", result);
        assertTrue("Result should indicate failure", 
            result.contains("failed") || result.contains("error") || result.contains("Excel"));
    }
    
    @Test
    public void testGetLastOcrConfidence() {
        final Float confidence = extractor.getLastOcrConfidence();
        // Should be null initially
        assertTrue("Confidence should be null or valid", confidence == null || confidence >= 0);
    }
    
    @Test
    public void testExtractFromPdf() {
        final byte[] pdfBytes = "fake pdf".getBytes();
        final String result = extractor.extractFromPdf(pdfBytes, "test.pdf");
        assertNotNull("Result should not be null", result);
    }
    
    @Test
    public void testExtractFromImage() {
        final byte[] imageBytes = "fake image".getBytes();
        final String result = extractor.extractFromImage(imageBytes, "test.jpg");
        assertNotNull("Result should not be null", result);
    }
    
    private byte[] createMinimalExcelBytes() {
        // Return minimal valid Excel file structure
        try {
            final org.apache.poi.ss.usermodel.Workbook workbook = 
                new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            workbook.createSheet("Sheet1");
            final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();
            return baos.toByteArray();
        } catch (final Exception e) {
            return new byte[0];
        }
    }
}
