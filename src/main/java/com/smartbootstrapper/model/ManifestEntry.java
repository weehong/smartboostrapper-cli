package com.smartbootstrapper.model;

import java.util.Objects;

/**
 * Represents a single file entry in the manifest.
 * Each entry specifies a file to harvest from a specific Git commit.
 */
public class ManifestEntry {

    private final String commitHash;
    private final String sourcePath;
    private final String destinationPath;
    private final String targetDirectory;

    public ManifestEntry(String commitHash, String sourcePath, String destinationPath) {
        this(commitHash, sourcePath, destinationPath, null);
    }

    public ManifestEntry(String commitHash, String sourcePath, String destinationPath, String targetDirectory) {
        this.commitHash = Objects.requireNonNull(commitHash, "commitHash must not be null");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath must not be null");
        this.destinationPath = Objects.requireNonNull(destinationPath, "destinationPath must not be null");
        this.targetDirectory = targetDirectory; // Optional, can be null
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    /**
     * Returns the target directory for this entry, if specified.
     * When set, this overrides the default target directory.
     */
    public String getTargetDirectory() {
        return targetDirectory;
    }

    /**
     * Returns true if this entry has a custom target directory.
     */
    public boolean hasTargetDirectory() {
        return targetDirectory != null && !targetDirectory.isBlank();
    }

    public boolean isJavaFile() {
        return sourcePath.endsWith(".java");
    }

    public boolean isPropertiesFile() {
        return sourcePath.endsWith(".properties") || sourcePath.endsWith(".yml") || sourcePath.endsWith(".yaml");
    }

    public boolean isXmlFile() {
        return sourcePath.endsWith(".xml");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManifestEntry that = (ManifestEntry) o;
        return Objects.equals(commitHash, that.commitHash) &&
               Objects.equals(sourcePath, that.sourcePath) &&
               Objects.equals(destinationPath, that.destinationPath) &&
               Objects.equals(targetDirectory, that.targetDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commitHash, sourcePath, destinationPath, targetDirectory);
    }

    @Override
    public String toString() {
        if (targetDirectory != null) {
            return String.format("ManifestEntry{commit='%s', source='%s', dest='%s', target='%s'}",
                    commitHash, sourcePath, destinationPath, targetDirectory);
        }
        return String.format("ManifestEntry{commit='%s', source='%s', dest='%s'}",
                commitHash, sourcePath, destinationPath);
    }
}
