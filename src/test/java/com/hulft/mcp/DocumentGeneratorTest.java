package com.hulft.mcp;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class DocumentGeneratorTest {

    @Test
    public void generateScheduleProductionPDF() throws Exception {
        byte[] pdf = createPDF("SCHEDULE_PRODUCTION", 
            "Production Schedule",
            "Date: 2026-01-16",
            "Product: Widget A",
            "Quantity: 1000 units",
            "Start: 08:00",
            "End: 17:00",
            "Line: Assembly Line 3"
        );
        savePDF("test-pdfs/schedule_production.pdf", pdf);
        System.out.println("Generated: schedule_production.pdf");
    }

    @Test
    public void generateInvoiceProductionPDF() throws Exception {
        byte[] pdf = createPDF("INVOICE_PRODUCTION",
            "INVOICE",
            "Invoice #: INV-2026-001",
            "Date: 2026-01-16",
            "Customer: Acme Corp",
            "Item: Widget A - $50.00 x 100",
            "Subtotal: $5,000.00",
            "Tax: $500.00",
            "Total: $5,500.00"
        );
        savePDF("test-pdfs/invoice_production.pdf", pdf);
        System.out.println("Generated: invoice_production.pdf");
    }

    @Test
    public void generatePurchaseOrderPDF() throws Exception {
        byte[] pdf = createPDF("PURCHASE_ORDER",
            "PURCHASE ORDER",
            "PO #: PO-2026-001",
            "Date: 2026-01-16",
            "Vendor: Parts Supplier Inc",
            "Item: Steel Plates - $25.00 x 200",
            "Delivery Date: 2026-01-20",
            "Total: $5,000.00"
        );
        savePDF("test-pdfs/purchase_order.pdf", pdf);
        System.out.println("Generated: purchase_order.pdf");
    }

    @Test
    public void generateCustomsDeclarationPDF() throws Exception {
        byte[] pdf = createPDF("CUSTOMS_DECLARATION",
            "CUSTOMS DECLARATION",
            "Declaration #: CD-2026-001",
            "Date: 2026-01-16",
            "Origin: USA",
            "Destination: Japan",
            "Contents: Electronic Components",
            "Value: $10,000.00",
            "HS Code: 8542.31"
        );
        savePDF("test-pdfs/customs_declaration.pdf", pdf);
        System.out.println("Generated: customs_declaration.pdf");
    }

    private void savePDF(String path, byte[] pdf) throws Exception {
        java.nio.file.Path filePath = java.nio.file.Paths.get(path);
        java.nio.file.Files.createDirectories(filePath.getParent());
        java.nio.file.Files.write(filePath, pdf);
    }

    private byte[] createPDF(String purposeCode, String... lines) throws Exception {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 750);
        contentStream.showText("Purpose: " + purposeCode);
        contentStream.endText();

        contentStream.setFont(PDType1Font.HELVETICA, 12);
        float yPosition = 720;
        for (String line : lines) {
            contentStream.beginText();
            contentStream.newLineAtOffset(50, yPosition);
            contentStream.showText(line);
            contentStream.endText();
            yPosition -= 20;
        }

        contentStream.close();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();

        return baos.toByteArray();
    }
}
