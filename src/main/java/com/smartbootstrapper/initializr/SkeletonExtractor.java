package com.smartbootstrapper.initializr;

import com.smartbootstrapper.exception.InitializrException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts Spring Boot skeleton projects from ZIP files.
 */
public class SkeletonExtractor {

    private static final Logger logger = LoggerFactory.getLogger(SkeletonExtractor.class);

    /**
     * Extracts a skeleton ZIP file to the target directory.
     *
     * @param zipPath         Path to the ZIP file
     * @param targetDirectory Target directory for extraction
     * @return List of extracted file paths
     * @throws InitializrException if extraction fails
     */
    public List<Path> extractSkeleton(Path zipPath, Path targetDirectory) {
        logger.info("Extracting skeleton from {} to {}", zipPath, targetDirectory);

        validateZipFile(zipPath);
        ensureTargetDirectory(targetDirectory);

        List<Path> extractedFiles = new ArrayList<>();
        String rootFolder = null;

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            // First pass: determine root folder (Spring Initializr creates a folder with the artifact name)
            rootFolder = findRootFolder(zipFile);
            logger.debug("Detected root folder in ZIP: {}", rootFolder);

            // Second pass: extract files
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path extractedPath = extractEntry(zipFile, entry, targetDirectory, rootFolder);
                if (extractedPath != null) {
                    extractedFiles.add(extractedPath);
                }
            }

        } catch (IOException e) {
            throw new InitializrException(
                    "Failed to extract skeleton ZIP: " + e.getMessage(),
                    zipPath.toString(),
                    e
            );
        }

        logger.info("Extracted {} files to {}", extractedFiles.size(), targetDirectory);
        return extractedFiles;
    }

    private void validateZipFile(Path zipPath) {
        if (!Files.exists(zipPath)) {
            throw new InitializrException(
                    "ZIP file does not exist: " + zipPath,
                    zipPath.toString()
            );
        }

        if (!Files.isReadable(zipPath)) {
            throw new InitializrException(
                    "ZIP file is not readable: " + zipPath,
                    zipPath.toString()
            );
        }

        try {
            // Validate ZIP structure
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                if (zipFile.size() == 0) {
                    throw new InitializrException(
                            "ZIP file is empty",
                            zipPath.toString()
                    );
                }
            }
        } catch (IOException e) {
            throw new InitializrException(
                    "Invalid ZIP file: " + e.getMessage(),
                    zipPath.toString(),
                    e
            );
        }
    }

    private void ensureTargetDirectory(Path targetDirectory) {
        try {
            if (Files.exists(targetDirectory)) {
                if (!Files.isDirectory(targetDirectory)) {
                    throw new InitializrException(
                            "Target path exists but is not a directory: " + targetDirectory,
                            targetDirectory.toString()
                    );
                }
            } else {
                Files.createDirectories(targetDirectory);
                logger.debug("Created target directory: {}", targetDirectory);
            }
        } catch (IOException e) {
            throw new InitializrException(
                    "Failed to create target directory: " + e.getMessage(),
                    targetDirectory.toString(),
                    e
            );
        }
    }

    private String findRootFolder(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            int slashIndex = name.indexOf('/');
            if (slashIndex > 0) {
                return name.substring(0, slashIndex);
            }
        }
        return null;
    }

    private Path extractEntry(ZipFile zipFile, ZipEntry entry, Path targetDirectory, String rootFolder)
            throws IOException {

        String entryName = entry.getName();

        // Skip the root folder itself
        if (entry.isDirectory() && rootFolder != null && entryName.equals(rootFolder + "/")) {
            return null;
        }

        // Remove root folder prefix if present
        String relativePath = entryName;
        if (rootFolder != null && entryName.startsWith(rootFolder + "/")) {
            relativePath = entryName.substring(rootFolder.length() + 1);
        }

        // Skip empty paths
        if (relativePath.isEmpty()) {
            return null;
        }

        // Validate path (prevent zip slip attack)
        Path targetPath = targetDirectory.resolve(relativePath).normalize();
        if (!targetPath.startsWith(targetDirectory)) {
            logger.warn("Skipping potentially malicious ZIP entry: {}", entryName);
            return null;
        }

        if (entry.isDirectory()) {
            Files.createDirectories(targetPath);
            logger.debug("Created directory: {}", targetPath);
            return null;
        }

        // Create parent directories if needed
        Files.createDirectories(targetPath.getParent());

        // Extract file
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.debug("Extracted file: {}", targetPath);
        return targetPath;
    }

    /**
     * Cleans up a temporary ZIP file.
     *
     * @param zipPath Path to the ZIP file to delete
     */
    public void cleanup(Path zipPath) {
        try {
            if (Files.exists(zipPath)) {
                Files.delete(zipPath);
                logger.debug("Cleaned up temporary ZIP: {}", zipPath);
            }
        } catch (IOException e) {
            logger.warn("Failed to clean up temporary ZIP file: {}", e.getMessage());
        }
    }

    /**
     * Cleans up an extracted directory.
     *
     * @param directory Path to the directory to delete
     */
    public void cleanupDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                FileUtils.deleteDirectory(directory.toFile());
                logger.debug("Cleaned up directory: {}", directory);
            }
        } catch (IOException e) {
            logger.warn("Failed to clean up directory: {}", e.getMessage());
        }
    }
}
