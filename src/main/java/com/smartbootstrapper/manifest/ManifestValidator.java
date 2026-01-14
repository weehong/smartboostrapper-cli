package com.smartbootstrapper.manifest;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.model.Manifest;
import com.smartbootstrapper.model.ManifestEntry;
import com.smartbootstrapper.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Validates manifest files for structural correctness.
 */
public class ManifestValidator {

    private static final Logger logger = LoggerFactory.getLogger(ManifestValidator.class);

    /**
     * Validates that a manifest file exists and is readable.
     *
     * @param manifestPath Path to the manifest file
     * @return ValidationResult indicating success or failure with details
     */
    public ValidationResult validateManifestFile(Path manifestPath) {
        logger.debug("Validating manifest file: {}", manifestPath);

        ValidationResult.Builder builder = ValidationResult.builder();

        // Check file exists
        boolean exists = Files.exists(manifestPath);
        builder.addCheck("Manifest file exists", exists);
        if (!exists) {
            builder.addError(
                    String.format(Constants.ERROR_MANIFEST_NOT_FOUND, manifestPath),
                    "Ensure the manifest file path is correct"
            );
            return builder.build();
        }

        // Check file is readable
        boolean readable = Files.isReadable(manifestPath);
        builder.addCheck("Manifest file is readable", readable);
        if (!readable) {
            builder.addError(
                    "Manifest file is not readable: " + manifestPath,
                    "Check file permissions"
            );
            return builder.build();
        }

        // Check file is not a directory
        boolean isFile = Files.isRegularFile(manifestPath);
        builder.addCheck("Manifest path is a file", isFile);
        if (!isFile) {
            builder.addError(
                    "Manifest path is a directory, not a file: " + manifestPath,
                    "Provide path to a YAML file"
            );
            return builder.build();
        }

        // Check file extension
        String fileName = manifestPath.getFileName().toString();
        boolean hasValidExtension = fileName.endsWith(".yaml") || fileName.endsWith(".yml");
        builder.addCheck("Manifest has YAML extension", hasValidExtension,
                hasValidExtension ? null : "Expected .yaml or .yml extension");

        return builder.build();
    }

    /**
     * Validates the structure of a parsed manifest.
     *
     * @param manifest The manifest to validate
     * @return ValidationResult indicating success or failure with details
     */
    public ValidationResult validateManifestStructure(Manifest manifest) {
        logger.debug("Validating manifest structure with {} entries", manifest.size());

        ValidationResult.Builder builder = ValidationResult.builder();

        // Check manifest has entries
        boolean hasEntries = !manifest.isEmpty();
        builder.addCheck("Manifest contains entries", hasEntries,
                hasEntries ? manifest.size() + " entries found" : null);
        if (!hasEntries) {
            builder.addError(Constants.ERROR_MANIFEST_EMPTY);
            return builder.build();
        }

        // Check for duplicate destinations
        List<String> duplicates = manifest.findDuplicateDestinations();
        boolean noDuplicates = duplicates.isEmpty();
        builder.addCheck("No duplicate destination paths", noDuplicates);
        if (!noDuplicates) {
            for (String dup : duplicates) {
                builder.addError(
                        "Duplicate destination path: " + dup,
                        "Each destination path must be unique"
                );
            }
        }

        // Validate source repository path
        String repoPath = manifest.getSourceRepositoryPath();
        boolean repoPathValid = repoPath != null && !repoPath.isBlank();
        builder.addCheck("Source repository path is specified", repoPathValid);
        if (!repoPathValid) {
            builder.addError(
                    "Source repository path is missing or empty",
                    "Specify the sourceRepository field in the manifest"
            );
        }

        // Validate each entry
        int entryNumber = 0;
        for (ManifestEntry entry : manifest.getEntries()) {
            entryNumber++;
            validateEntry(entry, entryNumber, builder);
        }

        return builder.build();
    }

    private void validateEntry(ManifestEntry entry, int entryNumber, ValidationResult.Builder builder) {
        // Validate commit hash format
        String commit = entry.getCommitHash();
        boolean commitValid = Constants.GIT_COMMIT_HASH_PATTERN.matcher(commit).matches();
        if (!commitValid) {
            builder.addError(
                    new ValidationResult.ValidationError(
                            "Invalid commit hash format: " + commit,
                            "Expected 7-40 hex characters",
                            entry.getSourcePath(),
                            null,
                            commit
                    )
            );
        }

        // Validate source path is not empty and relative
        String sourcePath = entry.getSourcePath();
        if (sourcePath.isBlank()) {
            builder.addError(
                    new ValidationResult.ValidationError(
                            "Source path is empty",
                            "Entry " + entryNumber,
                            null,
                            null,
                            commit
                    )
            );
        } else if (sourcePath.startsWith("/")) {
            builder.addError(
                    new ValidationResult.ValidationError(
                            "Source path should be relative: " + sourcePath,
                            "Remove leading slash",
                            sourcePath,
                            null,
                            commit
                    )
            );
        }

        // Validate destination path is not empty
        String destPath = entry.getDestinationPath();
        if (destPath.isBlank()) {
            builder.addError(
                    new ValidationResult.ValidationError(
                            "Destination path is empty",
                            "Entry " + entryNumber,
                            null,
                            null,
                            commit
                    )
            );
        }

        // Check for path traversal attempts
        if (sourcePath.contains("..") || destPath.contains("..")) {
            builder.addError(
                    new ValidationResult.ValidationError(
                            "Path contains '..' which is not allowed",
                            "Entry " + entryNumber,
                            sourcePath.contains("..") ? sourcePath : destPath,
                            null,
                            commit
                    )
            );
        }
    }
}
