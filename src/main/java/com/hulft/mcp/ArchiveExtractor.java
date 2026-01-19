package com.hulft.mcp;

import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Handles extraction of archive files (ZIP, TAR, TAR.GZ).
 */
@Slf4j
public class ArchiveExtractor {
    
    public int extract(final String archivePath, final String destPath) {
        try {
            final Path archive = Paths.get(archivePath);
            final Path dest = Paths.get(destPath);
            
            if (archivePath.endsWith(".zip")) {
                return extractZip(archive, dest);
            } else if (archivePath.endsWith(".tar") || archivePath.endsWith(".tar.gz")) {
                return extractTar(archive, dest);
            }
            
            log.warn("Unsupported archive format: {}", archivePath);
            return 0;
        } catch (final Exception e) {
            log.error("Error extracting archive", e);
            return 0;
        }
    }
    
    private int extractZip(final Path zipFile, final Path destDir) throws Exception {
        int count = 0;
        try (final ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    final Path filePath = destDir.resolve(entry.getName());
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                    count++;
                    log.info("Extracted: {}", entry.getName());
                }
                zis.closeEntry();
            }
        }
        return count;
    }
    
    private int extractTar(final Path tarFile, final Path destDir) throws Exception {
        int count = 0;
        java.io.InputStream fileStream = Files.newInputStream(tarFile);
        
        if (tarFile.toString().endsWith(".gz")) {
            fileStream = new GZIPInputStream(fileStream);
        }
        
        try (final TarArchiveInputStream tis = new TarArchiveInputStream(fileStream)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                if (!entry.isDirectory()) {
                    final Path filePath = destDir.resolve(entry.getName());
                    Files.createDirectories(filePath.getParent());
                    Files.copy(tis, filePath, StandardCopyOption.REPLACE_EXISTING);
                    count++;
                    log.info("Extracted: {}", entry.getName());
                }
            }
        }
        return count;
    }
}
