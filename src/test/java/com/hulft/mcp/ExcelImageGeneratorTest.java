package com.hulft.mcp;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;

public class ExcelImageGeneratorTest {

    @Test
    public void generateInvoiceExcel() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Invoice");
        
        int rowNum = 0;
        createRow(sheet, rowNum++, "Purpose:", "INVOICE_PRODUCTION");
        createRow(sheet, rowNum++, "INVOICE", "");
        createRow(sheet, rowNum++, "Invoice #:", "INV-2026-001");
        createRow(sheet, rowNum++, "Date:", "2026-01-16");
        createRow(sheet, rowNum++, "Customer:", "Acme Corp");
        createRow(sheet, rowNum++, "", "");
        createRow(sheet, rowNum++, "Item", "Amount");
        createRow(sheet, rowNum++, "Widget A", "$5,000.00");
        createRow(sheet, rowNum++, "Tax", "$500.00");
        createRow(sheet, rowNum++, "Total", "$5,500.00");
        
        saveExcel(workbook, "test-files/invoice.xlsx");
        System.out.println("Generated: invoice.xlsx");
    }

    @Test
    public void generatePurchaseOrderExcel() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("PO");
        
        int rowNum = 0;
        createRow(sheet, rowNum++, "Purpose:", "PURCHASE_ORDER");
        createRow(sheet, rowNum++, "PURCHASE ORDER", "");
        createRow(sheet, rowNum++, "PO #:", "PO-2026-001");
        createRow(sheet, rowNum++, "Date:", "2026-01-16");
        createRow(sheet, rowNum++, "Vendor:", "Parts Supplier Inc");
        createRow(sheet, rowNum++, "", "");
        createRow(sheet, rowNum++, "Item", "Quantity", "Price");
        createRow(sheet, rowNum++, "Steel Plates", "200", "$5,000.00");
        
        saveExcel(workbook, "test-files/purchase_order.xlsx");
        System.out.println("Generated: purchase_order.xlsx");
    }

    @Test
    public void generateInvoiceImage() throws Exception {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 600);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        
        int y = 50;
        g.drawString("Purpose: INVOICE_PRODUCTION", 50, y);
        y += 40;
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("INVOICE", 50, y);
        y += 40;
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Invoice #: INV-2026-IMG-001", 50, y);
        y += 30;
        g.drawString("Date: 2026-01-16", 50, y);
        y += 30;
        g.drawString("Customer: Image Test Corp", 50, y);
        y += 50;
        g.drawString("Amount: $1,234.56", 50, y);
        
        g.dispose();
        saveImage(image, "test-files/invoice.png");
        System.out.println("Generated: invoice.png");
    }

    @Test
    public void generateScheduleImage() throws Exception {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 600);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        
        int y = 50;
        g.drawString("Purpose: SCHEDULE_PRODUCTION", 50, y);
        y += 40;
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Production Schedule", 50, y);
        y += 40;
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Date: 2026-01-16", 50, y);
        y += 30;
        g.drawString("Product: Widget Assembly", 50, y);
        y += 30;
        g.drawString("Quantity: 1000 units", 50, y);
        y += 30;
        g.drawString("Line: Assembly Line 3", 50, y);
        
        g.dispose();
        saveImage(image, "test-files/schedule.png");
        System.out.println("Generated: schedule.png");
    }

    private void createRow(Sheet sheet, int rowNum, String... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private void saveExcel(Workbook workbook, String path) throws Exception {
        java.nio.file.Path filePath = java.nio.file.Paths.get(path);
        java.nio.file.Files.createDirectories(filePath.getParent());
        try (FileOutputStream fos = new FileOutputStream(path)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    private void saveImage(BufferedImage image, String path) throws Exception {
        java.nio.file.Path filePath = java.nio.file.Paths.get(path);
        java.nio.file.Files.createDirectories(filePath.getParent());
        ImageIO.write(image, "PNG", filePath.toFile());
    }
}
