package com.hulft.mcp;

import org.apache.poi.ss.usermodel.*;
import java.io.ByteArrayInputStream;

/**
 * Converts documents to markdown format.
 */
public class MarkdownConverter {
    
    public String convertExcelToMarkdown(final byte[] fileBytes) {
        try {
            final Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes));
            final StringBuilder md = new StringBuilder();
            
            for (final Sheet sheet : workbook) {
                md.append("# ").append(sheet.getSheetName()).append("\n\n");
                
                for (final Row row : sheet) {
                    md.append("| ");
                    for (final Cell cell : row) {
                        md.append(cell.toString()).append(" | ");
                    }
                    md.append("\n");
                    
                    if (row.getRowNum() == 0) {
                        md.append("| ");
                        for (int i = 0; i < row.getLastCellNum(); i++) {
                            md.append("--- | ");
                        }
                        md.append("\n");
                    }
                }
                md.append("\n");
            }
            workbook.close();
            return md.toString();
        } catch (final Exception e) {
            return "Error converting Excel: " + e.getMessage();
        }
    }
}
