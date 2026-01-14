package com.smartbootstrapper.writer;

import com.smartbootstrapper.exception.SmartBootstrapperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Writes harvested and refactored files to the target directory.
 */
public class ProjectFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(ProjectFileWriter.class);

    private final Path targetDirectory;
    private final RollbackManager rollbackManager;
    private BiConsumer<String, Boolean> progressCallback;

    public ProjectFileWriter(Path targetDirectory, RollbackManager rollbackManager) {
        this.targetDirectory = targetDirectory;
        this.rollbackManager = rollbackManager;
    }

    /**
     * Sets a callback for progress updates.
     *
     * @param callback BiConsumer that receives (file path, success status)
     */
    public void setProgressCallback(BiConsumer<String, Boolean> callback) {
        this.progressCallback = callback;
    }

    /**
     * Writes all files to the target directory.
     *
     * @param files Map of relative path to file content
     * @return Number of files successfully written
     */
    public int writeFiles(Map<String, byte[]> files) {
        logger.info("Writing {} files to {}", files.size(), targetDirectory);

        int written = 0;
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String relativePath = entry.getKey();
            byte[] content = entry.getValue();

            try {
                writeFile(relativePath, content);
                written++;

                if (progressCallback != null) {
                    progressCallback.accept(relativePath, true);
                }
            } catch (Exception e) {
                logger.error("Failed to write file: {}", relativePath, e);
                if (progressCallback != null) {
                    progressCallback.accept(relativePath, false);
                }
                throw new SmartBootstrapperException(
                        "Failed to write file: " + relativePath + " - " + e.getMessage(),
                        e
                );
            }
        }

        logger.info("Successfully wrote {} files", written);
        return written;
    }

    /**
     * Writes a single file to the target directory.
     *
     * @param relativePath The relative path within the target directory
     * @param content      The file content
     */
    public void writeFile(String relativePath, byte[] content) {
        // Normalize the relative path - strip leading slashes to ensure it's treated as relative
        String sanitizedPath = relativePath;
        while (sanitizedPath.startsWith("/") || sanitizedPath.startsWith("\\")) {
            sanitizedPath = sanitizedPath.substring(1);
        }

        Path normalizedTargetDir = targetDirectory.toAbsolutePath().normalize();
        Path fullPath = normalizedTargetDir.resolve(sanitizedPath).toAbsolutePath().normalize();

        // Security check: ensure path is within target directory
        if (!fullPath.startsWith(normalizedTargetDir)) {
            logger.error("Path traversal check failed - targetDir: '{}', fullPath: '{}', relativePath: '{}', sanitizedPath: '{}'",
                    normalizedTargetDir, fullPath, relativePath, sanitizedPath);
            throw new SmartBootstrapperException(
                    "Attempted path traversal: " + relativePath,
                    "Security violation detected"
            );
        }

        logger.debug("Writing file: {}", fullPath);

        try {
            // Create parent directories if needed
            Path parentDir = fullPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                rollbackManager.recordDirectoryCreated(parentDir);
                logger.debug("Created directory: {}", parentDir);
            }

            // Write the file
            boolean fileExisted = Files.exists(fullPath);
            byte[] originalContent = null;

            if (fileExisted) {
                originalContent = Files.readAllBytes(fullPath);
            }

            Files.write(fullPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            if (fileExisted) {
                rollbackManager.recordFileModified(fullPath, originalContent);
            } else {
                rollbackManager.recordFileCreated(fullPath);
            }

            logger.debug("Wrote {} bytes to {}", content.length, fullPath);

        } catch (IOException e) {
            throw new SmartBootstrapperException(
                    "Failed to write file: " + e.getMessage(),
                    fullPath.toString(),
                    e
            );
        }
    }

    /**
     * Writes a single file with string content.
     *
     * @param relativePath The relative path within the target directory
     * @param content      The file content as a string
     */
    public void writeFile(String relativePath, String content) {
        writeFile(relativePath, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Copies a file from source to destination within the target directory.
     *
     * @param sourcePath      The source file path
     * @param destinationPath The relative destination path
     */
    public void copyFile(Path sourcePath, String destinationPath) {
        try {
            byte[] content = Files.readAllBytes(sourcePath);
            writeFile(destinationPath, content);
        } catch (IOException e) {
            throw new SmartBootstrapperException(
                    "Failed to copy file: " + e.getMessage(),
                    sourcePath.toString(),
                    e
            );
        }
    }

    /**
     * Returns the target directory.
     */
    public Path getTargetDirectory() {
        return targetDirectory;
    }

    /**
     * Returns the rollback manager.
     */
    public RollbackManager getRollbackManager() {
        return rollbackManager;
    }
}
