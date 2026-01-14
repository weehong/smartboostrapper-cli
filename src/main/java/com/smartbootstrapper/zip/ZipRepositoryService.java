package com.smartbootstrapper.zip;

import com.smartbootstrapper.exception.GitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Service for extracting files from ZIP-based commit snapshots.
 * Handles ZIP files named with pattern: {project}-{commitHash}.zip
 */
public class ZipRepositoryService implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ZipRepositoryService.class);

    private final Path commitsDirectory;
    private final Map<String, Path> commitToZipMap;
    private final Map<String, ZipFile> openZipFiles;

    private ZipRepositoryService(Path commitsDirectory, Map<String, Path> commitToZipMap) {
        this.commitsDirectory = commitsDirectory;
        this.commitToZipMap = commitToZipMap;
        this.openZipFiles = new HashMap<>();
    }

    /**
     * Opens a ZIP-based repository from a directory containing commit ZIP files.
     *
     * @param commitsDirectory Path to directory containing ZIP files
     * @return ZipRepositoryService instance
     * @throws GitException if the directory is invalid or contains no ZIP files
     */
    public static ZipRepositoryService open(String commitsDirectory) {
        return open(Path.of(commitsDirectory));
    }

    /**
     * Opens a ZIP-based repository from a directory containing commit ZIP files.
     *
     * @param commitsDirectory Path to directory containing ZIP files
     * @return ZipRepositoryService instance
     * @throws GitException if the directory is invalid or contains no ZIP files
     */
    public static ZipRepositoryService open(Path commitsDirectory) {
        logger.debug("Opening ZIP repository at: {}", commitsDirectory);

        if (!Files.exists(commitsDirectory)) {
            throw new GitException(
                    "Commits directory not found: " + commitsDirectory,
                    commitsDirectory.toString()
            );
        }

        if (!Files.isDirectory(commitsDirectory)) {
            throw new GitException(
                    "Path is not a directory: " + commitsDirectory,
                    commitsDirectory.toString()
            );
        }

        // Scan for ZIP files and build commit hash map
        Map<String, Path> commitToZipMap = new HashMap<>();
        try (Stream<Path> files = Files.list(commitsDirectory)) {
            files.filter(p -> p.toString().endsWith(".zip"))
                    .forEach(zipPath -> {
                        String commitHash = extractCommitHash(zipPath);
                        if (commitHash != null) {
                            commitToZipMap.put(commitHash, zipPath);
                            // Also map abbreviated hashes (7+ chars)
                            if (commitHash.length() >= 7) {
                                commitToZipMap.put(commitHash.substring(0, 7), zipPath);
                            }
                            logger.debug("Found commit ZIP: {} -> {}", commitHash.substring(0, 7), zipPath.getFileName());
                        }
                    });
        } catch (IOException e) {
            throw new GitException(
                    "Failed to scan commits directory: " + e.getMessage(),
                    commitsDirectory.toString(),
                    e
            );
        }

        if (commitToZipMap.isEmpty()) {
            throw new GitException(
                    "No commit ZIP files found in: " + commitsDirectory,
                    commitsDirectory.toString()
            );
        }

        logger.info("Opened ZIP repository with {} commits at: {}",
                commitToZipMap.size() / 2, commitsDirectory);

        return new ZipRepositoryService(commitsDirectory, commitToZipMap);
    }

    /**
     * Extracts commit hash from ZIP filename.
     * Expected format: {project}-{commitHash}.zip
     */
    private static String extractCommitHash(Path zipPath) {
        String fileName = zipPath.getFileName().toString();
        // Remove .zip extension
        String baseName = fileName.substring(0, fileName.length() - 4);

        // Find last hyphen and extract commit hash
        int lastHyphen = baseName.lastIndexOf('-');
        if (lastHyphen > 0 && lastHyphen < baseName.length() - 1) {
            String potentialHash = baseName.substring(lastHyphen + 1);
            // Validate it looks like a commit hash (hex characters)
            if (potentialHash.matches("[a-fA-F0-9]{7,40}")) {
                return potentialHash.toLowerCase();
            }
        }

        logger.warn("Could not extract commit hash from: {}", fileName);
        return null;
    }

    /**
     * Checks if a commit exists (has a corresponding ZIP file).
     *
     * @param commitHash The commit hash to check (full or abbreviated)
     * @return true if the commit ZIP exists
     */
    public boolean commitExists(String commitHash) {
        String normalizedHash = commitHash.toLowerCase();

        // Check exact match first
        if (commitToZipMap.containsKey(normalizedHash)) {
            return true;
        }

        // Check abbreviated match
        if (normalizedHash.length() >= 7) {
            String abbreviated = normalizedHash.substring(0, 7);
            return commitToZipMap.containsKey(abbreviated);
        }

        return false;
    }

    /**
     * Checks if a file exists in a specific commit's ZIP.
     *
     * @param commitHash The commit hash
     * @param filePath   The file path to check
     * @return true if the file exists in the ZIP
     */
    public boolean fileExistsAtCommit(String commitHash, String filePath) {
        try {
            ZipFile zipFile = getZipFile(commitHash);
            if (zipFile == null) {
                return false;
            }

            String entryPath = findEntryPath(zipFile, filePath);
            return entryPath != null;
        } catch (Exception e) {
            logger.debug("Error checking file existence: {} at {}", filePath, commitHash, e);
            return false;
        }
    }

    /**
     * Retrieves file content as bytes from a specific commit's ZIP.
     *
     * @param commitHash The commit hash
     * @param filePath   The file path relative to project root
     * @return The file content as bytes
     * @throws GitException if the commit or file is not found
     */
    public byte[] getFileBytes(String commitHash, String filePath) {
        logger.debug("Getting file {} from commit {}", filePath, commitHash);

        ZipFile zipFile = getZipFile(commitHash);
        if (zipFile == null) {
            throw new GitException(
                    "Commit not found: " + commitHash,
                    commitsDirectory.toString(),
                    commitHash,
                    filePath
            );
        }

        String entryPath = findEntryPath(zipFile, filePath);
        if (entryPath == null) {
            throw new GitException(
                    String.format("File not found at commit %s: %s",
                            commitHash.substring(0, Math.min(7, commitHash.length())), filePath),
                    commitsDirectory.toString(),
                    commitHash,
                    filePath
            );
        }

        try {
            ZipEntry entry = zipFile.getEntry(entryPath);
            try (InputStream is = zipFile.getInputStream(entry);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                is.transferTo(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new GitException(
                    "Failed to read file from ZIP: " + e.getMessage(),
                    commitsDirectory.toString(),
                    commitHash,
                    filePath,
                    e
            );
        }
    }

    /**
     * Retrieves file content as a string from a specific commit's ZIP.
     */
    public String getFileContent(String commitHash, String filePath) {
        byte[] bytes = getFileBytes(commitHash, filePath);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Gets the ZipFile for a commit hash, opening it if necessary.
     */
    private ZipFile getZipFile(String commitHash) {
        String normalizedHash = commitHash.toLowerCase();

        // Find the ZIP path
        Path zipPath = commitToZipMap.get(normalizedHash);
        if (zipPath == null && normalizedHash.length() >= 7) {
            zipPath = commitToZipMap.get(normalizedHash.substring(0, 7));
        }

        if (zipPath == null) {
            return null;
        }

        // Check if already open
        String zipKey = zipPath.toString();
        if (openZipFiles.containsKey(zipKey)) {
            return openZipFiles.get(zipKey);
        }

        // Open the ZIP file
        try {
            ZipFile zipFile = new ZipFile(zipPath.toFile());
            openZipFiles.put(zipKey, zipFile);
            return zipFile;
        } catch (IOException e) {
            throw new GitException(
                    "Failed to open ZIP file: " + e.getMessage(),
                    commitsDirectory.toString(),
                    e
            );
        }
    }

    /**
     * Finds the full entry path for a file in the ZIP.
     * Handles the case where ZIP contents are in a subdirectory.
     */
    private String findEntryPath(ZipFile zipFile, String filePath) {
        // First, try direct match
        if (zipFile.getEntry(filePath) != null) {
            return filePath;
        }

        // ZIP files from GitHub/GitLab often have a root directory
        // Try to find the entry with any prefix
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // Check if entry ends with our file path
            if (entryName.endsWith("/" + filePath) || entryName.equals(filePath)) {
                return entryName;
            }

            // Also check without leading slash
            String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            if (entryName.endsWith("/" + normalizedPath)) {
                return entryName;
            }
        }

        return null;
    }

    /**
     * Lists all available commit hashes.
     */
    public Set<String> getAvailableCommits() {
        Set<String> commits = new HashSet<>();
        for (String hash : commitToZipMap.keySet()) {
            if (hash.length() > 7) {
                commits.add(hash);
            }
        }
        return commits;
    }

    /**
     * Returns the commits directory path.
     */
    public String getRepositoryPath() {
        return commitsDirectory.toString();
    }

    @Override
    public void close() {
        for (ZipFile zipFile : openZipFiles.values()) {
            try {
                zipFile.close();
            } catch (IOException e) {
                logger.warn("Failed to close ZIP file: {}", e.getMessage());
            }
        }
        openZipFiles.clear();
        logger.debug("Closed ZIP repository: {}", commitsDirectory);
    }
}
