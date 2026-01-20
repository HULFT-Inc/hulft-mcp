package com.hulft.mcp;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class ArchiveExtractorTest {
    
    private final ArchiveExtractor extractor = new ArchiveExtractor();
    
    @Test
    public void testExtractZip() throws Exception {
        // Create temp directory
        final Path tempDir = Files.createTempDirectory("test-extract");
        
        // Create a simple zip file
        final Path zipPath = tempDir.resolve("test.zip");
        final java.io.FileOutputStream fos = new java.io.FileOutputStream(zipPath.toFile());
        final java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);
        
        zos.putNextEntry(new java.util.zip.ZipEntry("test.txt"));
        zos.write("test content".getBytes());
        zos.closeEntry();
        zos.close();
        
        // Extract
        final Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        final int count = extractor.extract(zipPath.toString(), outputDir.toString());
        
        assertEquals("Should extract 1 file", 1, count);
        assertTrue("Extracted file should exist", Files.exists(outputDir.resolve("test.txt")));
        
        // Cleanup
        Files.deleteIfExists(outputDir.resolve("test.txt"));
        Files.deleteIfExists(outputDir);
        Files.deleteIfExists(zipPath);
        Files.deleteIfExists(tempDir);
    }
    
    @Test
    public void testExtractInvalidArchive() {
        final int count = extractor.extract("/nonexistent/file.zip", "/tmp/output");
        assertEquals("Should return 0 for invalid archive", 0, count);
    }
}
