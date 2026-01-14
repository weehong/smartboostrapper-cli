package com.smartbootstrapper.validation;

import com.smartbootstrapper.Constants;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates user input values.
 */
public class InputValidator {

    /**
     * Result of input validation.
     */
    public record ValidationResult(boolean valid, String message, String suggestion) {
        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }

        public static ValidationResult failure(String message, String suggestion) {
            return new ValidationResult(false, message, suggestion);
        }
    }

    /**
     * Validates a group ID.
     * Must be lowercase with dots, starting with a letter.
     */
    public ValidationResult validateGroupId(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.failure("Group ID cannot be empty",
                    "Example: com.example, org.mycompany");
        }

        if (!Constants.GROUP_ID_PATTERN.matcher(input).matches()) {
            return ValidationResult.failure(
                    "Invalid group ID format",
                    "Use lowercase letters and dots only. Example: com.example");
        }

        return ValidationResult.success();
    }

    /**
     * Validates an artifact ID.
     * Must be lowercase with hyphens, starting with a letter.
     */
    public ValidationResult validateArtifactId(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.failure("Artifact ID cannot be empty",
                    "Example: my-app, user-service");
        }

        if (!Constants.ARTIFACT_ID_PATTERN.matcher(input).matches()) {
            return ValidationResult.failure(
                    "Invalid artifact ID format",
                    "Use lowercase letters and hyphens only. Example: my-app");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a Java package name.
     */
    public ValidationResult validatePackageName(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.failure("Package name cannot be empty",
                    "Example: com.example.myapp");
        }

        if (!Constants.PACKAGE_NAME_PATTERN.matcher(input).matches()) {
            return ValidationResult.failure(
                    "Invalid package name format",
                    "Use lowercase letters and dots only. Example: com.example.myapp");
        }

        // Check for Java reserved words
        String[] parts = input.split("\\.");
        for (String part : parts) {
            if (isJavaReservedWord(part)) {
                return ValidationResult.failure(
                        "Package name contains reserved word: " + part,
                        "Avoid Java keywords in package names");
            }
        }

        return ValidationResult.success();
    }

    /**
     * Validates a project version.
     */
    public ValidationResult validateVersion(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.failure("Version cannot be empty",
                    "Example: 1.0.0, 0.0.1-SNAPSHOT");
        }

        if (!Constants.VERSION_PATTERN.matcher(input).matches()) {
            return ValidationResult.failure(
                    "Invalid version format",
                    "Use semantic versioning. Example: 1.0.0 or 0.0.1-SNAPSHOT");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a Spring Boot version.
     */
    public ValidationResult validateSpringBootVersion(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.failure("Spring Boot version cannot be empty",
                    "Example: 3.2.0, 3.1.5");
        }

        if (!Constants.SPRING_BOOT_VERSION_PATTERN.matcher(input).matches()) {
            return ValidationResult.failure(
                    "Invalid Spring Boot version format",
                    "Use format: major.minor.patch. Example: 3.2.0");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a Java version.
     */
    public ValidationResult validateJavaVersion(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.failure("Java version cannot be empty",
                    "Supported versions: " + String.join(", ", Constants.SUPPORTED_JAVA_VERSIONS));
        }

        if (!Constants.JAVA_VERSION_PATTERN.matcher(input).matches()) {
            return ValidationResult.failure(
                    "Invalid or unsupported Java version",
                    "Supported versions: " + String.join(", ", Constants.SUPPORTED_JAVA_VERSIONS));
        }

        return ValidationResult.success();
    }

    /**
     * Validates a project name.
     */
    public ValidationResult validateProjectName(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.failure("Project name cannot be empty",
                    "Example: My Application, User Service");
        }

        if (input.length() > 100) {
            return ValidationResult.failure(
                    "Project name is too long (max 100 characters)",
                    "Use a shorter name");
        }

        return ValidationResult.success();
    }

    /**
     * Validates a target directory path.
     */
    public ValidationResult validateTargetDirectory(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.failure("Target directory cannot be empty");
        }

        // Check for invalid characters
        if (input.contains("\0")) {
            return ValidationResult.failure("Target directory contains invalid characters");
        }

        return ValidationResult.success();
    }

    /**
     * Suggests a valid value based on invalid input.
     */
    public Optional<String> suggestGroupId(String input) {
        if (input == null) {
            return Optional.empty();
        }

        // Convert to lowercase and replace invalid characters
        String suggestion = input.toLowerCase()
                .replaceAll("[^a-z0-9.]", "")
                .replaceAll("^\\.", "")
                .replaceAll("\\.$", "")
                .replaceAll("\\.{2,}", ".");

        if (suggestion.isEmpty()) {
            return Optional.of("com.example");
        }

        if (!suggestion.contains(".")) {
            suggestion = "com." + suggestion;
        }

        return Optional.of(suggestion);
    }

    /**
     * Suggests a valid artifact ID based on invalid input.
     */
    public Optional<String> suggestArtifactId(String input) {
        if (input == null) {
            return Optional.empty();
        }

        // Convert to lowercase and replace invalid characters with hyphens
        String suggestion = input.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("^-", "")
                .replaceAll("-$", "")
                .replaceAll("-{2,}", "-");

        if (suggestion.isEmpty()) {
            return Optional.of("my-app");
        }

        return Optional.of(suggestion);
    }

    private boolean isJavaReservedWord(String word) {
        return switch (word) {
            case "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                 "class", "const", "continue", "default", "do", "double", "else", "enum",
                 "extends", "final", "finally", "float", "for", "goto", "if", "implements",
                 "import", "instanceof", "int", "interface", "long", "native", "new", "package",
                 "private", "protected", "public", "return", "short", "static", "strictfp",
                 "super", "switch", "synchronized", "this", "throw", "throws", "transient",
                 "try", "void", "volatile", "while", "true", "false", "null" -> true;
            default -> false;
        };
    }
}
