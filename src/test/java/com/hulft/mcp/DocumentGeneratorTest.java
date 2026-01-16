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
    
    @Test
    public void generateComplexInvoice() throws Exception {
        byte[] pdf = createPDF("INVOICE_PRODUCTION",
            "TAX INVOICE",
            "Invoice Number: 2026-INV-00542",
            "Issue Date: January 16, 2026",
            "",
            "Bill To: Global Manufacturing Ltd",
            "123 Industrial Park, Detroit, MI 48201",
            "",
            "Items:",
            "1. Steel Rods (Grade A) - Qty: 500 - $45.00 ea - $22,500.00",
            "2. Aluminum Sheets - Qty: 200 - $85.50 ea - $17,100.00",
            "3. Copper Wire (10mm) - Qty: 1000m - $12.75/m - $12,750.00",
            "",
            "Subtotal: $52,350.00",
            "Sales Tax (8.5%): $4,449.75",
            "Shipping: $250.00",
            "TOTAL DUE: $57,049.75",
            "",
            "Payment Terms: Net 30",
            "Due Date: February 15, 2026"
        );
        savePDF("test-pdfs/complex_invoice.pdf", pdf);
        System.out.println("Generated: complex_invoice.pdf");
    }
    
    @Test
    public void generateInvoiceWithoutPurpose() throws Exception {
        // Test document without explicit "Purpose:" field
        byte[] pdf = createPDF("",
            "INVOICE",
            "Invoice #: INV-2026-999",
            "Date: 2026-01-16",
            "Customer: Test Corp",
            "Amount: $1,234.56"
        );
        savePDF("test-pdfs/invoice_no_purpose.pdf", pdf);
        System.out.println("Generated: invoice_no_purpose.pdf");
    }
    
    @Test
    public void generateAmbiguousDocument() throws Exception {
        // Document that could be confused between types
        byte[] pdf = createPDF("SCHEDULE_PRODUCTION",
            "Production Order & Schedule",
            "Order #: PO-2026-123",
            "Schedule Date: 2026-01-20",
            "Product: Widget Assembly",
            "Quantity: 5000 units",
            "Invoice will follow upon completion"
        );
        savePDF("test-pdfs/ambiguous_doc.pdf", pdf);
        System.out.println("Generated: ambiguous_doc.pdf");
    }
    
    @Test
    public void generateMultiPagePurchaseOrder() throws Exception {
        // Longer document
        byte[] pdf = createMultiPagePDF("PURCHASE_ORDER",
            new String[]{
                "PURCHASE ORDER",
                "PO Number: PO-2026-BULK-001",
                "Date: January 16, 2026",
                "Vendor: International Supplies Inc",
                "Delivery Address: Warehouse 5, 789 Logistics Blvd"
            },
            new String[]{
                "Line Items (Page 2):",
                "Item 1: Bearings (SKU: BRG-001) - 10,000 units @ $2.50",
                "Item 2: Gaskets (SKU: GSK-045) - 5,000 units @ $1.25",
                "Item 3: Bolts M8 (SKU: BLT-M8) - 50,000 units @ $0.15",
                "Item 4: Washers (SKU: WSH-STD) - 50,000 units @ $0.05"
            }
        );
        savePDF("test-pdfs/multipage_po.pdf", pdf);
        System.out.println("Generated: multipage_po.pdf");
    }

    private void savePDF(String path, byte[] pdf) throws Exception {
        java.nio.file.Path filePath = java.nio.file.Paths.get(path);
        java.nio.file.Files.createDirectories(filePath.getParent());
        java.nio.file.Files.write(filePath, pdf);
    }
    
    private byte[] createMultiPagePDF(String purposeCode, String[] page1Lines, String[] page2Lines) throws Exception {
        PDDocument document = new PDDocument();
        
        // Page 1
        PDPage page1 = new PDPage();
        document.addPage(page1);
        PDPageContentStream cs1 = new PDPageContentStream(document, page1);
        
        if (!purposeCode.isEmpty()) {
            cs1.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs1.beginText();
            cs1.newLineAtOffset(50, 750);
            cs1.showText("Purpose: " + purposeCode);
            cs1.endText();
        }
        
        cs1.setFont(PDType1Font.HELVETICA, 12);
        float yPos = 720;
        for (String line : page1Lines) {
            cs1.beginText();
            cs1.newLineAtOffset(50, yPos);
            cs1.showText(line);
            cs1.endText();
            yPos -= 20;
        }
        cs1.close();
        
        // Page 2
        PDPage page2 = new PDPage();
        document.addPage(page2);
        PDPageContentStream cs2 = new PDPageContentStream(document, page2);
        cs2.setFont(PDType1Font.HELVETICA, 12);
        yPos = 750;
        for (String line : page2Lines) {
            cs2.beginText();
            cs2.newLineAtOffset(50, yPos);
            cs2.showText(line);
            cs2.endText();
            yPos -= 20;
        }
        cs2.close();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();
        return baos.toByteArray();
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
