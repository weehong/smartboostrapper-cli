package com.smartbootstrapper.manifest;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.exception.ManifestException;
import com.smartbootstrapper.model.Manifest;
import com.smartbootstrapper.model.ManifestEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Parses YAML manifest files into Manifest objects.
 */
public class ManifestParser {

    private static final Logger logger = LoggerFactory.getLogger(ManifestParser.class);

    /**
     * Pattern to extract sequence number from manifest filename.
     * Matches: commit-1.yaml, commit-2.yml, commit-10.yaml, etc.
     */
    private static final Pattern MANIFEST_SEQUENCE_PATTERN = Pattern.compile("commit-(\\d+)\\.ya?ml$", Pattern.CASE_INSENSITIVE);

    private final Yaml yaml;

    public ManifestParser() {
        this.yaml = new Yaml();
    }

    /**
     * Parses a manifest file from the given path.
     *
     * @param manifestPath Path to the manifest YAML file
     * @return Parsed Manifest object
     * @throws ManifestException if parsing fails
     */
    public Manifest parse(Path manifestPath) {
        logger.debug("Parsing manifest file: {}", manifestPath);

        if (!Files.exists(manifestPath)) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_NOT_FOUND, manifestPath),
                    manifestPath.toString()
            );
        }

        if (!Files.isReadable(manifestPath)) {
            throw new ManifestException(
                    "Manifest file is not readable: " + manifestPath,
                    manifestPath.toString()
            );
        }

        // Extract sequence number from filename (e.g., commit-1.yaml -> 1)
        Integer sequenceNumber = extractSequenceNumber(manifestPath);

        try (InputStream inputStream = Files.newInputStream(manifestPath)) {
            return parseFromInputStream(inputStream, manifestPath.toString(), sequenceNumber);
        } catch (IOException e) {
            throw new ManifestException(
                    "Failed to read manifest file: " + e.getMessage(),
                    manifestPath.toString(),
                    e
            );
        }
    }

    /**
     * Extracts the sequence number from a manifest filename.
     * For example, "commit-1.yaml" returns 1, "commit-10.yml" returns 10.
     *
     * @param manifestPath Path to the manifest file
     * @return The sequence number, or null if not found
     */
    Integer extractSequenceNumber(Path manifestPath) {
        String filename = manifestPath.getFileName().toString();
        Matcher matcher = MANIFEST_SEQUENCE_PATTERN.matcher(filename);

        if (matcher.find()) {
            int sequence = Integer.parseInt(matcher.group(1));
            logger.debug("Extracted sequence number {} from manifest filename: {}", sequence, filename);
            return sequence;
        }

        logger.debug("No sequence number found in manifest filename: {}", filename);
        return null;
    }

    /**
     * Parses a manifest from an input stream.
     *
     * @param inputStream Input stream containing YAML content
     * @param sourceName  Name of the source for error messages
     * @return Parsed Manifest object
     * @throws ManifestException if parsing fails
     */
    public Manifest parseFromInputStream(InputStream inputStream, String sourceName) {
        return parseFromInputStream(inputStream, sourceName, null);
    }

    /**
     * Parses a manifest from an input stream with an optional sequence number.
     *
     * @param inputStream            Input stream containing YAML content
     * @param sourceName             Name of the source for error messages
     * @param filenameSequenceNumber Optional sequence number extracted from filename
     * @return Parsed Manifest object
     * @throws ManifestException if parsing fails
     */
    @SuppressWarnings("unchecked")
    public Manifest parseFromInputStream(InputStream inputStream, String sourceName, Integer filenameSequenceNumber) {
        Map<String, Object> root;
        try {
            root = yaml.load(inputStream);
        } catch (MarkedYAMLException e) {
            int line = e.getProblemMark() != null ? e.getProblemMark().getLine() + 1 : -1;
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_PARSE, e.getProblem()),
                    sourceName,
                    line,
                    e
            );
        } catch (Exception e) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_PARSE, e.getMessage()),
                    sourceName,
                    e
            );
        }

        if (root == null) {
            throw new ManifestException(Constants.ERROR_MANIFEST_EMPTY, sourceName);
        }

        // Extract source repository path
        String sourceRepository = extractSourceRepository(root, sourceName);

        // Extract source type (default to ZIP for backward compatibility with new format)
        Manifest.SourceType sourceType = extractSourceType(root, sourceName);

        // Extract sequence number: YAML field takes priority over filename pattern
        Integer sequenceNumber = extractSequenceNumberFromYaml(root, sourceName);
        if (sequenceNumber == null) {
            sequenceNumber = filenameSequenceNumber;
        }

        // Extract file entries
        List<ManifestEntry> entries = extractFileEntries(root, sourceName);

        if (entries.isEmpty()) {
            throw new ManifestException(Constants.ERROR_MANIFEST_EMPTY, sourceName);
        }

        logger.info("Successfully parsed manifest with {} entries from {} repository: {}{}",
                entries.size(), sourceType, sourceRepository,
                sequenceNumber != null ? " (sequence: " + sequenceNumber + ")" : "");

        return new Manifest(entries, sourceRepository, sourceType, sequenceNumber);
    }

    /**
     * Extracts the sequence number from the YAML content.
     * Returns null if the field is not present or invalid.
     *
     * @param root       The parsed YAML root map
     * @param sourceName Name of the source for error messages
     * @return The sequence number, or null if not present
     */
    private Integer extractSequenceNumberFromYaml(Map<String, Object> root, String sourceName) {
        Object sequenceObj = root.get(Constants.MANIFEST_FIELD_SEQUENCE_NUMBER);

        if (sequenceObj == null) {
            return null;
        }

        if (sequenceObj instanceof Integer) {
            Integer seq = (Integer) sequenceObj;
            logger.debug("Extracted sequence number {} from YAML field in: {}", seq, sourceName);
            return seq;
        }

        if (sequenceObj instanceof Number) {
            int seq = ((Number) sequenceObj).intValue();
            logger.debug("Extracted sequence number {} from YAML field in: {}", seq, sourceName);
            return seq;
        }

        if (sequenceObj instanceof String) {
            try {
                int seq = Integer.parseInt(((String) sequenceObj).trim());
                logger.debug("Extracted sequence number {} from YAML string field in: {}", seq, sourceName);
                return seq;
            } catch (NumberFormatException e) {
                throw new ManifestException(
                        Constants.MANIFEST_FIELD_SEQUENCE_NUMBER + " must be a valid integer: " + sequenceObj,
                        sourceName
                );
            }
        }

        throw new ManifestException(
                Constants.MANIFEST_FIELD_SEQUENCE_NUMBER + " must be an integer",
                sourceName
        );
    }

    private Manifest.SourceType extractSourceType(Map<String, Object> root, String sourceName) {
        Object sourceTypeObj = root.get(Constants.MANIFEST_FIELD_SOURCE_TYPE);

        if (sourceTypeObj == null) {
            // Default to ZIP if not specified (for the new ZIP-based workflow)
            return Manifest.SourceType.ZIP;
        }

        if (!(sourceTypeObj instanceof String)) {
            throw new ManifestException(
                    Constants.MANIFEST_FIELD_SOURCE_TYPE + " must be a string (git or zip)",
                    sourceName
            );
        }

        String sourceType = ((String) sourceTypeObj).toLowerCase().trim();

        return switch (sourceType) {
            case Constants.SOURCE_TYPE_GIT -> Manifest.SourceType.GIT;
            case Constants.SOURCE_TYPE_ZIP -> Manifest.SourceType.ZIP;
            default -> throw new ManifestException(
                    "Invalid sourceType: " + sourceType + " (expected 'git' or 'zip')",
                    sourceName
            );
        };
    }

    private String extractSourceRepository(Map<String, Object> root, String sourceName) {
        Object sourceRepoObj = root.get(Constants.MANIFEST_FIELD_SOURCE_REPOSITORY);

        if (sourceRepoObj == null) {
            throw new ManifestException(
                    "Missing required field: " + Constants.MANIFEST_FIELD_SOURCE_REPOSITORY,
                    sourceName
            );
        }

        if (!(sourceRepoObj instanceof String)) {
            throw new ManifestException(
                    Constants.MANIFEST_FIELD_SOURCE_REPOSITORY + " must be a string",
                    sourceName
            );
        }

        String sourceRepository = (String) sourceRepoObj;
        if (sourceRepository.isBlank()) {
            throw new ManifestException(
                    Constants.MANIFEST_FIELD_SOURCE_REPOSITORY + " cannot be empty",
                    sourceName
            );
        }

        return sourceRepository;
    }

    @SuppressWarnings("unchecked")
    private List<ManifestEntry> extractFileEntries(Map<String, Object> root, String sourceName) {
        Object filesObj = root.get(Constants.MANIFEST_FIELD_FILES);

        if (filesObj == null) {
            throw new ManifestException(
                    "Missing required field: " + Constants.MANIFEST_FIELD_FILES,
                    sourceName
            );
        }

        if (!(filesObj instanceof List)) {
            throw new ManifestException(
                    Constants.MANIFEST_FIELD_FILES + " must be a list",
                    sourceName
            );
        }

        List<Object> filesList = (List<Object>) filesObj;
        List<ManifestEntry> entries = new ArrayList<>();

        for (int i = 0; i < filesList.size(); i++) {
            Object fileObj = filesList.get(i);
            if (!(fileObj instanceof Map)) {
                throw new ManifestException(
                        String.format(Constants.ERROR_MANIFEST_INVALID_FORMAT, i + 1,
                                "Each file entry must be a map with commit, sourcePath, and destinationPath"),
                        sourceName
                );
            }

            Map<String, Object> fileMap = (Map<String, Object>) fileObj;
            ManifestEntry entry = parseFileEntry(fileMap, i + 1, sourceName);
            entries.add(entry);
        }

        return entries;
    }

    private ManifestEntry parseFileEntry(Map<String, Object> fileMap, int entryNumber, String sourceName) {
        // Extract commit hash
        String commit = extractStringField(fileMap, Constants.MANIFEST_FIELD_COMMIT, entryNumber, sourceName);
        validateCommitHash(commit, entryNumber, sourceName);

        // Extract source path
        String sourcePath = extractStringField(fileMap, Constants.MANIFEST_FIELD_SOURCE_PATH, entryNumber, sourceName);
        validatePath(sourcePath, Constants.MANIFEST_FIELD_SOURCE_PATH, entryNumber, sourceName);

        // Extract destination path
        String destinationPath = extractStringField(fileMap, Constants.MANIFEST_FIELD_DESTINATION_PATH, entryNumber, sourceName);
        validatePath(destinationPath, Constants.MANIFEST_FIELD_DESTINATION_PATH, entryNumber, sourceName);

        // Extract optional target directory
        String targetDirectory = extractOptionalStringField(fileMap, Constants.MANIFEST_FIELD_TARGET_DIRECTORY, entryNumber, sourceName);

        return new ManifestEntry(commit, sourcePath, destinationPath, targetDirectory);
    }

    private String extractStringField(Map<String, Object> map, String fieldName, int entryNumber, String sourceName) {
        Object value = map.get(fieldName);

        if (value == null) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_INVALID_FORMAT, entryNumber,
                            "Missing required field: " + fieldName),
                    sourceName
            );
        }

        if (!(value instanceof String)) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_INVALID_FORMAT, entryNumber,
                            fieldName + " must be a string"),
                    sourceName
            );
        }

        String stringValue = (String) value;
        if (stringValue.isBlank()) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_INVALID_FORMAT, entryNumber,
                            fieldName + " cannot be empty"),
                    sourceName
            );
        }

        return stringValue.trim();
    }

    private String extractOptionalStringField(Map<String, Object> map, String fieldName, int entryNumber, String sourceName) {
        Object value = map.get(fieldName);

        if (value == null) {
            return null;
        }

        if (!(value instanceof String)) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_INVALID_FORMAT, entryNumber,
                            fieldName + " must be a string"),
                    sourceName
            );
        }

        String stringValue = ((String) value).trim();
        return stringValue.isBlank() ? null : stringValue;
    }

    private void validateCommitHash(String commit, int entryNumber, String sourceName) {
        if (!Constants.GIT_COMMIT_HASH_PATTERN.matcher(commit).matches()) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_INVALID_FORMAT, entryNumber,
                            "Invalid commit hash format: " + commit + " (expected 7-40 hex characters)"),
                    sourceName
            );
        }
    }

    private void validatePath(String path, String fieldName, int entryNumber, String sourceName) {
        if (path.startsWith("/") && fieldName.equals(Constants.MANIFEST_FIELD_SOURCE_PATH)) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_INVALID_FORMAT, entryNumber,
                            fieldName + " should be relative, not absolute: " + path),
                    sourceName
            );
        }

        if (path.contains("..")) {
            throw new ManifestException(
                    String.format(Constants.ERROR_MANIFEST_INVALID_FORMAT, entryNumber,
                            fieldName + " cannot contain '..': " + path),
                    sourceName
            );
        }
    }

    /**
     * Discovers all commit-*.yaml manifest files in the same directory as the given manifest file.
     * Returns them sorted by sequence number (commit-1.yaml, commit-2.yaml, etc.).
     *
     * @param manifestPath Path to any manifest file in the directory
     * @return List of manifest file paths sorted by sequence number
     */
    public List<Path> discoverAllManifests(Path manifestPath) {
        Path parentDir = manifestPath.toAbsolutePath().getParent();
        if (parentDir == null) {
            logger.warn("Cannot determine parent directory for manifest: {}", manifestPath);
            return List.of(manifestPath);
        }

        try (Stream<Path> files = Files.list(parentDir)) {
            List<Path> manifests = files
                    .filter(Files::isRegularFile)
                    .filter(p -> MANIFEST_SEQUENCE_PATTERN.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(p -> {
                        Integer seq = extractSequenceNumber(p);
                        return seq != null ? seq : Integer.MAX_VALUE;
                    }))
                    .toList();

            if (manifests.isEmpty()) {
                // No sequenced manifests found, return the original manifest
                logger.debug("No commit-*.yaml files found, using original manifest: {}", manifestPath);
                return List.of(manifestPath);
            }

            logger.info("Discovered {} manifest files in directory: {}", manifests.size(), parentDir);
            return manifests;

        } catch (IOException e) {
            logger.warn("Failed to list directory {}, using original manifest: {}", parentDir, e.getMessage());
            return List.of(manifestPath);
        }
    }

    /**
     * Parses all discovered manifest files in the directory.
     * Returns them sorted by sequence number.
     *
     * @param manifestPath Path to any manifest file in the directory
     * @return List of parsed Manifest objects sorted by sequence number
     */
    public List<Manifest> parseAllManifests(Path manifestPath) {
        List<Path> manifestPaths = discoverAllManifests(manifestPath);
        List<Manifest> manifests = new ArrayList<>();

        for (Path path : manifestPaths) {
            Manifest manifest = parse(path);
            manifests.add(manifest);
        }

        logger.info("Parsed {} manifests from directory", manifests.size());
        return manifests;
    }
}
