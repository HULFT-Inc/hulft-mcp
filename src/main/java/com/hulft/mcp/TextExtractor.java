package com.hulft.mcp;

import lombok.extern.slf4j.Slf4j;

/**
 * Handles text extraction from various document formats.
 */
@Slf4j
@SuppressWarnings("PMD.AvoidCatchingGenericException") // Generic exception handling for robustness
public class TextExtractor {
    private final software.amazon.awssdk.services.textract.TextractClient textractClient;
    private final ThreadLocal<Float> ocrConfidence = new ThreadLocal<>();
    
    public TextExtractor(final software.amazon.awssdk.services.textract.TextractClient textractClient) {
        this.textractClient = textractClient;
    }
    
    public String extractFromPdf(final byte[] fileBytes, final String filename) {
        return analyzeWithTextract(fileBytes, filename);
    }
    
    public String extractFromImage(final byte[] fileBytes, final String filename) {
        return analyzeWithTextract(fileBytes, filename);
    }
    
    public String extractFromExcel(final byte[] fileBytes) {
        try {
            final org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(
                new java.io.ByteArrayInputStream(fileBytes));
            final StringBuilder text = new StringBuilder();
            for (final org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                for (final org.apache.poi.ss.usermodel.Row row : sheet) {
                    for (final org.apache.poi.ss.usermodel.Cell cell : row) {
                        text.append(cell.toString()).append(' ');
                    }
                    text.append('\n');
                }
            }
            workbook.close();
            return text.toString();
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error extracting Excel text", e);
            }
            return "Excel extraction failed: " + e.getMessage();
        }
    }
    
    public Float getLastOcrConfidence() {
        final Float conf = ocrConfidence.get();
        ocrConfidence.remove();
        return conf;
    }
    
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private String analyzeWithTextract(final byte[] fileBytes, final String filename) {
        try {
            final software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest request = 
                software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest.builder()
                    .document(software.amazon.awssdk.services.textract.model.Document.builder()
                        .bytes(software.amazon.awssdk.core.SdkBytes.fromByteArray(fileBytes))
                        .build())
                    .build();
            
            final software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse response = 
                textractClient.detectDocumentText(request);
            
            final StringBuilder text = new StringBuilder();
            final java.util.List<Float> confidences = new java.util.ArrayList<>();
            
            for (final software.amazon.awssdk.services.textract.model.Block block : response.blocks()) {
                if (block.blockType() == software.amazon.awssdk.services.textract.model.BlockType.LINE) {
                    text.append(block.text()).append('\n');
                    if (block.confidence() != null) {
                        confidences.add(block.confidence());
                    }
                }
            }
            
            // Calculate average confidence
            final float avgConfidence = confidences.isEmpty()
                ? 0
                : (float) confidences.stream().mapToDouble(Float::doubleValue).average().orElse(0);
            
            final String extractedText = text.toString();
            if (log.isInfoEnabled()) {
                log.info("Textract extracted {} characters from {} (avg confidence: {:.2f}%)", 
                    extractedText.length(), filename, avgConfidence);
            }
            
            ocrConfidence.set(avgConfidence);
            return extractedText;
            
        } catch (final Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error with Textract analysis", e);
            }
            return "Textract analysis failed: " + e.getMessage();
        }
    }
}
