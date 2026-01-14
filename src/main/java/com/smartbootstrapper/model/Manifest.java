package com.smartbootstrapper.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Container for manifest entries with validation methods.
 * Represents the complete manifest file parsed from YAML.
 */
public class Manifest {

    /**
     * Source type for the manifest.
     */
    public enum SourceType {
        GIT,    // Traditional Git repository
        ZIP     // Directory containing commit ZIP files
    }

    private final List<ManifestEntry> entries;
    private final String sourceRepositoryPath;
    private final SourceType sourceType;
    private final Integer sequenceNumber;

    public Manifest(List<ManifestEntry> entries, String sourceRepositoryPath) {
        this(entries, sourceRepositoryPath, SourceType.GIT, null);
    }

    public Manifest(List<ManifestEntry> entries, String sourceRepositoryPath, SourceType sourceType) {
        this(entries, sourceRepositoryPath, sourceType, null);
    }

    public Manifest(List<ManifestEntry> entries, String sourceRepositoryPath, SourceType sourceType, Integer sequenceNumber) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.sourceRepositoryPath = Objects.requireNonNull(sourceRepositoryPath, "sourceRepositoryPath must not be null");
        this.sourceType = sourceType != null ? sourceType : SourceType.GIT;
        this.sequenceNumber = sequenceNumber;
    }

    public List<ManifestEntry> getEntries() {
        return entries;
    }

    public String getSourceRepositoryPath() {
        return sourceRepositoryPath;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public boolean isZipSource() {
        return sourceType == SourceType.ZIP;
    }

    public boolean isGitSource() {
        return sourceType == SourceType.GIT;
    }

    /**
     * Returns the sequence number extracted from the manifest filename.
     * For example, "commit-1.yaml" would have sequence number 1.
     *
     * @return Optional containing the sequence number, or empty if not extracted
     */
    public Optional<Integer> getSequenceNumber() {
        return Optional.ofNullable(sequenceNumber);
    }

    /**
     * Checks if this manifest has a sequence number.
     */
    public boolean hasSequenceNumber() {
        return sequenceNumber != null;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns all unique commit hashes referenced in the manifest.
     */
    public Set<String> getUniqueCommitHashes() {
        return entries.stream()
                .map(ManifestEntry::getCommitHash)
                .collect(Collectors.toSet());
    }

    /**
     * Returns all destination paths in the manifest.
     */
    public List<String> getDestinationPaths() {
        return entries.stream()
                .map(ManifestEntry::getDestinationPath)
                .collect(Collectors.toList());
    }

    /**
     * Finds duplicate destination paths in the manifest.
     */
    public List<String> findDuplicateDestinations() {
        Map<String, Long> pathCounts = entries.stream()
                .collect(Collectors.groupingBy(
                        ManifestEntry::getDestinationPath,
                        Collectors.counting()
                ));

        return pathCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Returns only Java file entries.
     */
    public List<ManifestEntry> getJavaEntries() {
        return entries.stream()
                .filter(ManifestEntry::isJavaFile)
                .collect(Collectors.toList());
    }

    /**
     * Returns only properties file entries.
     */
    public List<ManifestEntry> getPropertiesEntries() {
        return entries.stream()
                .filter(ManifestEntry::isPropertiesFile)
                .collect(Collectors.toList());
    }

    /**
     * Returns only XML file entries.
     */
    public List<ManifestEntry> getXmlEntries() {
        return entries.stream()
                .filter(ManifestEntry::isXmlFile)
                .collect(Collectors.toList());
    }

    /**
     * Detects the base package from Java file source paths.
     * Analyzes paths like "src/main/java/com/example/app/service/MyService.java"
     * to find the common base package (e.g., "com.example.app").
     *
     * NOTE: Uses source paths (not destination paths) because the actual Java
     * file content contains package declarations matching the source structure.
     * Use this for content refactoring (package declarations, import statements).
     *
     * @return Optional containing the detected base package, or empty if none found
     */
    public Optional<String> detectBasePackage() {
        return detectPackageFromPaths(entry -> entry.getSourcePath());
    }

    /**
     * Detects the base package from Java file destination paths.
     * Analyzes destination paths to find the common base package structure.
     *
     * Use this for path transformation (determining output directory structure).
     *
     * @return Optional containing the detected base package, or empty if none found
     */
    public Optional<String> detectDestinationBasePackage() {
        return detectPackageFromPaths(entry -> entry.getDestinationPath());
    }

    /**
     * Generic helper to detect base package from either source or destination paths.
     */
    private Optional<String> detectPackageFromPaths(java.util.function.Function<ManifestEntry, String> pathExtractor) {
        List<String> packagePaths = getJavaEntries().stream()
                .map(pathExtractor)
                .map(this::extractPackageFromPath)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());

        if (packagePaths.isEmpty()) {
            return Optional.empty();
        }

        // Find common base package among all packages
        return findCommonBasePackage(packagePaths);
    }

    /**
     * Extracts the package name from a Java file path.
     * E.g., "src/main/java/com/example/app/MyClass.java" -> "com.example.app"
     */
    private Optional<String> extractPackageFromPath(String path) {
        if (path == null || !path.endsWith(".java")) {
            return Optional.empty();
        }

        // Look for src/main/java/ or src/test/java/ patterns
        String[] sourceRoots = {"src/main/java/", "src/test/java/"};

        for (String sourceRoot : sourceRoots) {
            int rootIndex = path.indexOf(sourceRoot);
            if (rootIndex >= 0) {
                String packagePath = path.substring(rootIndex + sourceRoot.length());
                // Remove the filename to get just the package path
                int lastSlash = packagePath.lastIndexOf('/');
                if (lastSlash > 0) {
                    String packageName = packagePath.substring(0, lastSlash).replace('/', '.');
                    return Optional.of(packageName);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds the longest common base package among a list of packages.
     * E.g., ["com.example.app.service", "com.example.app.model"] -> "com.example.app"
     */
    private Optional<String> findCommonBasePackage(List<String> packages) {
        if (packages.isEmpty()) {
            return Optional.empty();
        }

        if (packages.size() == 1) {
            return Optional.of(packages.get(0));
        }

        // Split all packages into parts
        List<String[]> packageParts = packages.stream()
                .map(p -> p.split("\\."))
                .collect(Collectors.toList());

        // Find minimum length
        int minLength = packageParts.stream()
                .mapToInt(parts -> parts.length)
                .min()
                .orElse(0);

        // Find common prefix
        StringBuilder commonPackage = new StringBuilder();
        for (int i = 0; i < minLength; i++) {
            final int index = i;
            String part = packageParts.get(0)[i];

            boolean allMatch = packageParts.stream()
                    .allMatch(parts -> parts[index].equals(part));

            if (allMatch) {
                if (commonPackage.length() > 0) {
                    commonPackage.append(".");
                }
                commonPackage.append(part);
            } else {
                break;
            }
        }

        String result = commonPackage.toString();
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }

    @Override
    public String toString() {
        return String.format("Manifest{repository='%s', entries=%d}", sourceRepositoryPath, entries.size());
    }
}
