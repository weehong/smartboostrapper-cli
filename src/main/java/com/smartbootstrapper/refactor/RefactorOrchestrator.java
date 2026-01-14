package com.smartbootstrapper.refactor;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.exception.RefactorException;
import com.smartbootstrapper.model.RefactorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Orchestrates the refactoring of harvested files.
 * Routes files to the appropriate refactoring service based on file type.
 */
public class RefactorOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(RefactorOrchestrator.class);

    private final PackageRefactorService packageRefactorService;
    private final ResourceRefactorService resourceRefactorService;
    private BiConsumer<String, Boolean> progressCallback;

    public RefactorOrchestrator() {
        this.packageRefactorService = new PackageRefactorService();
        this.resourceRefactorService = new ResourceRefactorService();
    }

    public RefactorOrchestrator(PackageRefactorService packageRefactorService,
                                 ResourceRefactorService resourceRefactorService) {
        this.packageRefactorService = packageRefactorService;
        this.resourceRefactorService = resourceRefactorService;
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
     * Refactors all files in the provided map.
     *
     * @param files      Map of destination path to file content bytes
     * @param oldPackage The old package name to replace
     * @param newPackage The new package name
     * @return RefactorResult containing refactored files and statistics
     */
    public RefactorResult refactorFiles(Map<String, byte[]> files, String oldPackage, String newPackage) {
        logger.info("Starting refactoring of {} files ({} -> {})", files.size(), oldPackage, newPackage);

        RefactorResult.Builder resultBuilder = RefactorResult.builder()
                .totalFiles(files.size());

        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String destinationPath = entry.getKey();
            byte[] content = entry.getValue();

            refactorFile(destinationPath, content, oldPackage, newPackage, resultBuilder);
        }

        RefactorResult result = resultBuilder.build();
        logger.info("Refactoring completed: {} Java, {} properties, {} XML, {} skipped, {} errors",
                result.getJavaFilesRefactored(),
                result.getPropertiesFilesRefactored(),
                result.getXmlFilesRefactored(),
                result.getFilesSkipped(),
                result.getErrors().size());

        return result;
    }

    /**
     * Refactors a single file and updates the result builder.
     *
     * @param destinationPath The destination path of the file
     * @param content         The file content as bytes
     * @param oldPackage      The old package name
     * @param newPackage      The new package name
     * @param resultBuilder   The result builder to update
     * @return The refactored content as bytes
     */
    public byte[] refactorFile(String destinationPath, byte[] content, String oldPackage, String newPackage,
                                RefactorResult.Builder resultBuilder) {
        FileType fileType = determineFileType(destinationPath);

        try {
            byte[] refactoredContent = switch (fileType) {
                case JAVA -> refactorJavaFile(destinationPath, content, oldPackage, newPackage, resultBuilder);
                case PROPERTIES -> refactorPropertiesFile(destinationPath, content, oldPackage, newPackage, resultBuilder);
                case XML -> refactorXmlFile(destinationPath, content, oldPackage, newPackage, resultBuilder);
                case OTHER -> skipFile(destinationPath, content, resultBuilder);
            };

            if (progressCallback != null) {
                progressCallback.accept(destinationPath, true);
            }

            return refactoredContent;

        } catch (RefactorException e) {
            resultBuilder.addError(destinationPath, e.getMessage());
            if (progressCallback != null) {
                progressCallback.accept(destinationPath, false);
            }
            throw e;
        } catch (Exception e) {
            String errorMessage = "Unexpected error refactoring file: " + e.getMessage();
            resultBuilder.addError(destinationPath, errorMessage);
            if (progressCallback != null) {
                progressCallback.accept(destinationPath, false);
            }
            throw new RefactorException(errorMessage, destinationPath, e);
        }
    }

    private byte[] refactorJavaFile(String path, byte[] content, String oldPackage, String newPackage,
                                     RefactorResult.Builder resultBuilder) {
        String sourceCode = new String(content, StandardCharsets.UTF_8);
        String refactored = packageRefactorService.refactorJavaFile(sourceCode, oldPackage, newPackage, path);

        resultBuilder.incrementJavaFiles();
        resultBuilder.addRefactoredFile(path);

        logger.debug("Refactored Java file: {}", path);
        return refactored.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] refactorPropertiesFile(String path, byte[] content, String oldPackage, String newPackage,
                                           RefactorResult.Builder resultBuilder) {
        String sourceContent = new String(content, StandardCharsets.UTF_8);
        String refactored = resourceRefactorService.refactorPropertiesFile(sourceContent, oldPackage, newPackage);

        resultBuilder.incrementPropertiesFiles();
        resultBuilder.addRefactoredFile(path);

        logger.debug("Refactored properties file: {}", path);
        return refactored.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] refactorXmlFile(String path, byte[] content, String oldPackage, String newPackage,
                                    RefactorResult.Builder resultBuilder) {
        String sourceContent = new String(content, StandardCharsets.UTF_8);
        String refactored = resourceRefactorService.refactorXmlFile(sourceContent, oldPackage, newPackage);

        resultBuilder.incrementXmlFiles();
        resultBuilder.addRefactoredFile(path);

        logger.debug("Refactored XML file: {}", path);
        return refactored.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] skipFile(String path, byte[] content, RefactorResult.Builder resultBuilder) {
        resultBuilder.incrementSkipped();
        logger.debug("Skipped file (no refactoring needed): {}", path);
        return content;
    }

    /**
     * Determines the file type based on the file path extension.
     */
    private FileType determineFileType(String path) {
        if (path == null) {
            return FileType.OTHER;
        }

        String lowerPath = path.toLowerCase();

        if (lowerPath.endsWith(Constants.EXT_JAVA)) {
            return FileType.JAVA;
        }

        if (lowerPath.endsWith(Constants.EXT_PROPERTIES) ||
            lowerPath.endsWith(Constants.EXT_YML) ||
            lowerPath.endsWith(Constants.EXT_YAML)) {
            return FileType.PROPERTIES;
        }

        if (lowerPath.endsWith(Constants.EXT_XML)) {
            return FileType.XML;
        }

        return FileType.OTHER;
    }

    /**
     * Validates that all Java files can be parsed.
     *
     * @param files Map of destination path to file content bytes
     * @return RefactorResult containing validation errors
     */
    public RefactorResult validateJavaFiles(Map<String, byte[]> files) {
        logger.info("Validating {} files for parsing", files.size());

        RefactorResult.Builder resultBuilder = RefactorResult.builder()
                .totalFiles(files.size());

        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String path = entry.getKey();
            byte[] content = entry.getValue();

            if (determineFileType(path) == FileType.JAVA) {
                String sourceCode = new String(content, StandardCharsets.UTF_8);
                String parseErrors = packageRefactorService.getParseErrors(sourceCode, path);

                if (!parseErrors.isEmpty()) {
                    resultBuilder.addError(path, parseErrors);
                } else {
                    resultBuilder.incrementJavaFiles();
                }
            } else {
                resultBuilder.incrementSkipped();
            }
        }

        return resultBuilder.build();
    }

    private enum FileType {
        JAVA,
        PROPERTIES,
        XML,
        OTHER
    }

    /**
     * Transforms a destination path by replacing the old package structure with the new package structure.
     * For example, if oldPackage is "com.example.myapp" and newPackage is "com.mycompany.newapp",
     * then "src/main/java/com/example/myapp/service/MyService.java" becomes
     * "src/main/java/com/mycompany/newapp/service/MyService.java".
     *
     * @param destinationPath The original destination path
     * @param oldPackage      The old package name
     * @param newPackage      The new package name
     * @return The transformed destination path
     */
    public String transformDestinationPath(String destinationPath, String oldPackage, String newPackage) {
        if (destinationPath == null || oldPackage == null || newPackage == null) {
            return destinationPath;
        }

        if (oldPackage.equals(newPackage)) {
            return destinationPath;
        }

        // Convert package names to path format (e.g., com.example.myapp -> com/example/myapp)
        String oldPackagePath = oldPackage.replace('.', '/');
        String newPackagePath = newPackage.replace('.', '/');

        // Check for src/main/java or src/test/java patterns
        String[] sourceRoots = {"src/main/java/", "src/test/java/"};

        for (String sourceRoot : sourceRoots) {
            String oldPathPattern = sourceRoot + oldPackagePath;
            if (destinationPath.startsWith(oldPathPattern)) {
                String transformed = sourceRoot + newPackagePath + destinationPath.substring(oldPathPattern.length());
                logger.debug("Transformed path: {} -> {}", destinationPath, transformed);
                return transformed;
            }
        }

        // Also handle paths without leading src/ (e.g., just the package path)
        if (destinationPath.startsWith(oldPackagePath + "/") || destinationPath.equals(oldPackagePath)) {
            String transformed = newPackagePath + destinationPath.substring(oldPackagePath.length());
            logger.debug("Transformed path: {} -> {}", destinationPath, transformed);
            return transformed;
        }

        return destinationPath;
    }

    /**
     * Refactors all files and transforms their destination paths.
     * This is the main entry point that handles both content refactoring and path transformation.
     * Uses the same package for both content refactoring and path transformation.
     *
     * @param files      Map of destination path to file content bytes
     * @param oldPackage The old package name to replace
     * @param newPackage The new package name
     * @return RefactorResult containing refactored files with transformed paths and statistics
     */
    public RefactorResult refactorFilesWithPathTransformation(Map<String, byte[]> files, String oldPackage, String newPackage) {
        return refactorFilesWithPathTransformation(files, oldPackage, newPackage, oldPackage);
    }

    /**
     * Refactors all files and transforms their destination paths.
     * This overload allows specifying different packages for content refactoring and path transformation.
     *
     * @param files             Map of destination path to file content bytes
     * @param contentOldPackage The old package in file content (for refactoring package declarations/imports)
     * @param newPackage        The new package name
     * @param pathOldPackage    The old package in destination paths (for path transformation)
     * @return RefactorResult containing refactored files with transformed paths and statistics
     */
    public RefactorResult refactorFilesWithPathTransformation(Map<String, byte[]> files,
                                                               String contentOldPackage,
                                                               String newPackage,
                                                               String pathOldPackage) {
        logger.info("Starting refactoring with path transformation of {} files (content: {} -> {}, paths: {} -> {})",
                files.size(), contentOldPackage, newPackage, pathOldPackage, newPackage);

        RefactorResult.Builder resultBuilder = RefactorResult.builder()
                .totalFiles(files.size());

        Map<String, byte[]> transformedFiles = new HashMap<>();

        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String originalPath = entry.getKey();
            byte[] content = entry.getValue();

            // Transform the destination path using pathOldPackage
            String transformedPath = transformDestinationPath(originalPath, pathOldPackage, newPackage);

            // Refactor the file content using contentOldPackage
            byte[] refactoredContent = refactorFile(transformedPath, content, contentOldPackage, newPackage, resultBuilder);

            transformedFiles.put(transformedPath, refactoredContent);

            if (!originalPath.equals(transformedPath)) {
                logger.info("Path transformed: {} -> {}", originalPath, transformedPath);
            }
        }

        RefactorResult result = resultBuilder
                .refactoredFiles(transformedFiles)
                .build();

        logger.info("Refactoring with path transformation completed: {} Java, {} properties, {} XML, {} skipped, {} errors",
                result.getJavaFilesRefactored(),
                result.getPropertiesFilesRefactored(),
                result.getXmlFilesRefactored(),
                result.getFilesSkipped(),
                result.getErrors().size());

        return result;
    }
}
