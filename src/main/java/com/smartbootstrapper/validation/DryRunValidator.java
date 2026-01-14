package com.smartbootstrapper.validation;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.git.GitFileHarvester;
import com.smartbootstrapper.git.GitRepositoryService;
import com.smartbootstrapper.manifest.ManifestValidator;
import com.smartbootstrapper.model.HarvestResult;
import com.smartbootstrapper.model.Manifest;
import com.smartbootstrapper.model.ManifestEntry;
import com.smartbootstrapper.model.ValidationResult;
import com.smartbootstrapper.refactor.PackageRefactorService;
import com.smartbootstrapper.zip.ZipFileHarvester;
import com.smartbootstrapper.zip.ZipRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates comprehensive validation (dry-run) of the bootstrap operation.
 */
public class DryRunValidator {

    private static final Logger logger = LoggerFactory.getLogger(DryRunValidator.class);

    private final ManifestValidator manifestValidator;
    private final PackageRefactorService packageRefactorService;

    public DryRunValidator() {
        this.manifestValidator = new ManifestValidator();
        this.packageRefactorService = new PackageRefactorService();
    }

    /**
     * Performs complete dry-run validation.
     *
     * @param manifest   The manifest to validate
     * @param oldPackage The old package name (for refactoring validation)
     * @param newPackage The new package name (for refactoring validation)
     * @return Combined ValidationResult
     */
    public ValidationResult validateAll(Manifest manifest, String oldPackage, String newPackage) {
        logger.info("Starting dry-run validation for manifest with {} entries (source type: {})",
                manifest.size(), manifest.getSourceType());

        ValidationResult.Builder resultBuilder = ValidationResult.builder();

        // 1. Validate manifest structure
        ValidationResult manifestStructureResult = validateManifestStructure(manifest);
        mergeResults(manifestStructureResult, resultBuilder);
        if (!manifestStructureResult.isSuccess()) {
            logger.warn("Manifest structure validation failed");
            return resultBuilder.build();
        }

        // 2. Validate source repository based on type
        if (manifest.isZipSource()) {
            return validateZipSource(manifest, oldPackage, newPackage, resultBuilder);
        } else {
            return validateGitSource(manifest, oldPackage, newPackage, resultBuilder);
        }
    }

    /**
     * Validates ZIP-based source repository.
     */
    private ValidationResult validateZipSource(Manifest manifest, String oldPackage, String newPackage,
                                                ValidationResult.Builder resultBuilder) {
        // Validate ZIP repository
        ValidationResult zipRepoResult = validateZipRepository(manifest.getSourceRepositoryPath());
        mergeResults(zipRepoResult, resultBuilder);
        if (!zipRepoResult.isSuccess()) {
            logger.warn("ZIP repository validation failed");
            return resultBuilder.build();
        }

        // Validate commits and files exist using ZIP service
        try (ZipRepositoryService repoService = ZipRepositoryService.open(manifest.getSourceRepositoryPath())) {
            ValidationResult commitsResult = validateCommitsExistInZip(manifest, repoService);
            mergeResults(commitsResult, resultBuilder);
            if (!commitsResult.isSuccess()) {
                logger.warn("Commit validation failed");
                return resultBuilder.build();
            }

            ValidationResult filesResult = validateFilesExistInZip(manifest, repoService);
            mergeResults(filesResult, resultBuilder);
            if (!filesResult.isSuccess()) {
                logger.warn("File existence validation failed");
                return resultBuilder.build();
            }

            // Validate Java files can be parsed
            ValidationResult javaResult = validateJavaFilesFromZip(manifest, repoService, oldPackage, newPackage);
            mergeResults(javaResult, resultBuilder);
        }

        ValidationResult finalResult = resultBuilder.build();
        logger.info("Dry-run validation completed: {}", finalResult.isSuccess() ? "PASSED" : "FAILED");
        return finalResult;
    }

    /**
     * Validates Git-based source repository.
     */
    private ValidationResult validateGitSource(Manifest manifest, String oldPackage, String newPackage,
                                                ValidationResult.Builder resultBuilder) {
        // Validate Git repository
        ValidationResult gitRepoResult = validateGitRepository(manifest.getSourceRepositoryPath());
        mergeResults(gitRepoResult, resultBuilder);
        if (!gitRepoResult.isSuccess()) {
            logger.warn("Git repository validation failed");
            return resultBuilder.build();
        }

        // Validate commits and files exist
        try (GitRepositoryService repoService = GitRepositoryService.open(manifest.getSourceRepositoryPath())) {
            ValidationResult commitsResult = validateCommitsExist(manifest, repoService);
            mergeResults(commitsResult, resultBuilder);
            if (!commitsResult.isSuccess()) {
                logger.warn("Commit validation failed");
                return resultBuilder.build();
            }

            ValidationResult filesResult = validateFilesExist(manifest, repoService);
            mergeResults(filesResult, resultBuilder);
            if (!filesResult.isSuccess()) {
                logger.warn("File existence validation failed");
                return resultBuilder.build();
            }

            // Validate Java files can be parsed
            ValidationResult javaResult = validateJavaFiles(manifest, repoService, oldPackage, newPackage);
            mergeResults(javaResult, resultBuilder);
        }

        ValidationResult finalResult = resultBuilder.build();
        logger.info("Dry-run validation completed: {}", finalResult.isSuccess() ? "PASSED" : "FAILED");
        return finalResult;
    }

    /**
     * Validates manifest structure.
     */
    public ValidationResult validateManifestStructure(Manifest manifest) {
        logger.debug("Validating manifest structure");
        return manifestValidator.validateManifestStructure(manifest);
    }

    /**
     * Validates that the Git repository exists and is accessible.
     */
    public ValidationResult validateGitRepository(String repositoryPath) {
        logger.debug("Validating Git repository: {}", repositoryPath);

        ValidationResult.Builder builder = ValidationResult.builder();

        Path repoPath = Path.of(repositoryPath);

        // Check directory exists
        boolean exists = Files.exists(repoPath);
        builder.addCheck("Repository path exists", exists);
        if (!exists) {
            builder.addError(
                    String.format(Constants.ERROR_GIT_REPO_NOT_FOUND, repositoryPath),
                    "Verify the sourceRepository path in the manifest"
            );
            return builder.build();
        }

        // Check it's a directory
        boolean isDirectory = Files.isDirectory(repoPath);
        builder.addCheck("Repository path is a directory", isDirectory);
        if (!isDirectory) {
            builder.addError(
                    "Path is not a directory: " + repositoryPath,
                    "The sourceRepository should point to a Git repository directory"
            );
            return builder.build();
        }

        // Check it's a Git repository
        Path gitDir = repoPath.resolve(".git");
        boolean isGitRepo = Files.exists(gitDir) && Files.isDirectory(gitDir);
        builder.addCheck("Path is a Git repository", isGitRepo);
        if (!isGitRepo) {
            builder.addError(
                    "Not a Git repository: " + repositoryPath,
                    "Initialize with 'git init' or clone a repository"
            );
            return builder.build();
        }

        // Try to open the repository
        try (GitRepositoryService service = GitRepositoryService.open(repositoryPath)) {
            builder.addCheck("Repository can be opened", true);
        } catch (Exception e) {
            builder.addCheck("Repository can be opened", false);
            builder.addError(
                    "Failed to open repository: " + e.getMessage(),
                    "Check repository integrity"
            );
        }

        return builder.build();
    }

    /**
     * Validates that all commits referenced in the manifest exist.
     */
    public ValidationResult validateCommitsExist(Manifest manifest, GitRepositoryService repoService) {
        logger.debug("Validating commits exist");

        ValidationResult.Builder builder = ValidationResult.builder();
        Set<String> uniqueCommits = manifest.getUniqueCommitHashes();

        int validCommits = 0;
        for (String commit : uniqueCommits) {
            boolean exists = repoService.commitExists(commit);
            if (exists) {
                validCommits++;
            } else {
                builder.addError(
                        new ValidationResult.ValidationError(
                                String.format(Constants.ERROR_GIT_COMMIT_NOT_FOUND, commit),
                                "Verify the commit hash exists in the repository",
                                null, null, commit
                        )
                );
            }
        }

        boolean allValid = validCommits == uniqueCommits.size();
        builder.addCheck("All commits exist", allValid,
                String.format("%d/%d commits found", validCommits, uniqueCommits.size()));

        return builder.build();
    }

    /**
     * Validates that all files exist at their specified commits.
     */
    public ValidationResult validateFilesExist(Manifest manifest, GitRepositoryService repoService) {
        logger.debug("Validating files exist at commits");

        ValidationResult.Builder builder = ValidationResult.builder();

        int validFiles = 0;
        int totalFiles = manifest.size();

        for (ManifestEntry entry : manifest.getEntries()) {
            boolean exists = repoService.fileExistsAtCommit(entry.getCommitHash(), entry.getSourcePath());
            if (exists) {
                validFiles++;
            } else {
                builder.addError(
                        new ValidationResult.ValidationError(
                                String.format(Constants.ERROR_GIT_FILE_NOT_FOUND,
                                        entry.getCommitHash().substring(0, Math.min(7, entry.getCommitHash().length())),
                                        entry.getSourcePath()),
                                "Verify the file path and commit hash",
                                entry.getSourcePath(),
                                null,
                                entry.getCommitHash()
                        )
                );
            }
        }

        boolean allValid = validFiles == totalFiles;
        builder.addCheck("All files exist at specified commits", allValid,
                String.format("%d/%d files found", validFiles, totalFiles));

        return builder.build();
    }

    /**
     * Validates that all Java files can be parsed and refactored.
     */
    public ValidationResult validateJavaFiles(Manifest manifest, GitRepositoryService repoService,
                                               String oldPackage, String newPackage) {
        logger.debug("Validating Java files can be parsed");

        ValidationResult.Builder builder = ValidationResult.builder();

        GitFileHarvester harvester = new GitFileHarvester(repoService);
        HarvestResult harvestResult = harvester.harvestFiles(manifest);

        if (!harvestResult.isSuccess()) {
            builder.addCheck("Files can be harvested", false);
            harvestResult.getErrors().forEach((path, error) ->
                    builder.addError(error, "File: " + path));
            return builder.build();
        }

        builder.addCheck("Files can be harvested", true,
                harvestResult.getSuccessfulFiles() + " files");

        // Validate Java file parsing
        Map<String, byte[]> javaFiles = new HashMap<>();
        harvestResult.getHarvestedFiles().forEach((path, content) -> {
            if (path.endsWith(".java")) {
                javaFiles.put(path, content);
            }
        });

        if (javaFiles.isEmpty()) {
            builder.addCheck("Java files can be parsed", true, "No Java files to parse");
            return builder.build();
        }

        int parseable = 0;
        for (Map.Entry<String, byte[]> entry : javaFiles.entrySet()) {
            String path = entry.getKey();
            String content = new String(entry.getValue(), StandardCharsets.UTF_8);

            String parseErrors = packageRefactorService.getParseErrors(content, path);
            if (parseErrors.isEmpty()) {
                parseable++;
            } else {
                builder.addError(
                        new ValidationResult.ValidationError(
                                "Failed to parse Java file",
                                parseErrors,
                                path, null, null
                        )
                );
            }
        }

        boolean allParseable = parseable == javaFiles.size();
        builder.addCheck("Java files can be parsed", allParseable,
                String.format("%d/%d files parseable", parseable, javaFiles.size()));

        return builder.build();
    }

    // ========== ZIP Source Validation Methods ==========

    /**
     * Validates that the ZIP repository directory exists and contains ZIP files.
     */
    public ValidationResult validateZipRepository(String repositoryPath) {
        logger.debug("Validating ZIP repository: {}", repositoryPath);

        ValidationResult.Builder builder = ValidationResult.builder();

        Path repoPath = Path.of(repositoryPath);

        // Check directory exists
        boolean exists = Files.exists(repoPath);
        builder.addCheck("Repository path exists", exists);
        if (!exists) {
            builder.addError(
                    "Commits directory not found: " + repositoryPath,
                    "Verify the sourceRepository path in the manifest points to a directory containing commit ZIP files"
            );
            return builder.build();
        }

        // Check it's a directory
        boolean isDirectory = Files.isDirectory(repoPath);
        builder.addCheck("Repository path is a directory", isDirectory);
        if (!isDirectory) {
            builder.addError(
                    "Path is not a directory: " + repositoryPath,
                    "The sourceRepository should point to a directory containing commit ZIP files"
            );
            return builder.build();
        }

        // Try to open the ZIP repository
        try (ZipRepositoryService service = ZipRepositoryService.open(repositoryPath)) {
            int commitCount = service.getAvailableCommits().size();
            builder.addCheck("ZIP repository can be opened", true,
                    commitCount + " commit ZIP files found");
        } catch (Exception e) {
            builder.addCheck("ZIP repository can be opened", false);
            builder.addError(
                    "Failed to open ZIP repository: " + e.getMessage(),
                    "Ensure directory contains ZIP files with format: {project}-{commitHash}.zip"
            );
        }

        return builder.build();
    }

    /**
     * Validates that all commits referenced in the manifest exist as ZIP files.
     */
    public ValidationResult validateCommitsExistInZip(Manifest manifest, ZipRepositoryService repoService) {
        logger.debug("Validating commits exist in ZIP repository");

        ValidationResult.Builder builder = ValidationResult.builder();
        Set<String> uniqueCommits = manifest.getUniqueCommitHashes();

        int validCommits = 0;
        for (String commit : uniqueCommits) {
            boolean exists = repoService.commitExists(commit);
            if (exists) {
                validCommits++;
            } else {
                builder.addError(
                        new ValidationResult.ValidationError(
                                String.format("Commit ZIP not found: %s", commit),
                                "Ensure a ZIP file exists with commit hash in filename",
                                null, null, commit
                        )
                );
            }
        }

        boolean allValid = validCommits == uniqueCommits.size();
        builder.addCheck("All commits exist", allValid,
                String.format("%d/%d commits found", validCommits, uniqueCommits.size()));

        return builder.build();
    }

    /**
     * Validates that all files exist at their specified commits in ZIP files.
     */
    public ValidationResult validateFilesExistInZip(Manifest manifest, ZipRepositoryService repoService) {
        logger.debug("Validating files exist in ZIP archives");

        ValidationResult.Builder builder = ValidationResult.builder();

        int validFiles = 0;
        int totalFiles = manifest.size();

        for (ManifestEntry entry : manifest.getEntries()) {
            boolean exists = repoService.fileExistsAtCommit(entry.getCommitHash(), entry.getSourcePath());
            if (exists) {
                validFiles++;
            } else {
                builder.addError(
                        new ValidationResult.ValidationError(
                                String.format(Constants.ERROR_GIT_FILE_NOT_FOUND,
                                        entry.getCommitHash().substring(0, Math.min(7, entry.getCommitHash().length())),
                                        entry.getSourcePath()),
                                "Verify the file path exists in the commit ZIP archive",
                                entry.getSourcePath(),
                                null,
                                entry.getCommitHash()
                        )
                );
            }
        }

        boolean allValid = validFiles == totalFiles;
        builder.addCheck("All files exist at specified commits", allValid,
                String.format("%d/%d files found", validFiles, totalFiles));

        return builder.build();
    }

    /**
     * Validates that all Java files from ZIP archives can be parsed and refactored.
     */
    public ValidationResult validateJavaFilesFromZip(Manifest manifest, ZipRepositoryService repoService,
                                                      String oldPackage, String newPackage) {
        logger.debug("Validating Java files from ZIP archives can be parsed");

        ValidationResult.Builder builder = ValidationResult.builder();

        ZipFileHarvester harvester = new ZipFileHarvester(repoService);
        HarvestResult harvestResult = harvester.harvestFiles(manifest);

        if (!harvestResult.isSuccess()) {
            builder.addCheck("Files can be harvested", false);
            harvestResult.getErrors().forEach((path, error) ->
                    builder.addError(error, "File: " + path));
            return builder.build();
        }

        builder.addCheck("Files can be harvested", true,
                harvestResult.getSuccessfulFiles() + " files");

        // Validate Java file parsing
        Map<String, byte[]> javaFiles = new HashMap<>();
        harvestResult.getHarvestedFiles().forEach((path, content) -> {
            if (path.endsWith(".java")) {
                javaFiles.put(path, content);
            }
        });

        if (javaFiles.isEmpty()) {
            builder.addCheck("Java files can be parsed", true, "No Java files to parse");
            return builder.build();
        }

        int parseable = 0;
        for (Map.Entry<String, byte[]> entry : javaFiles.entrySet()) {
            String path = entry.getKey();
            String content = new String(entry.getValue(), StandardCharsets.UTF_8);

            String parseErrors = packageRefactorService.getParseErrors(content, path);
            if (parseErrors.isEmpty()) {
                parseable++;
            } else {
                builder.addError(
                        new ValidationResult.ValidationError(
                                "Failed to parse Java file",
                                parseErrors,
                                path, null, null
                        )
                );
            }
        }

        boolean allParseable = parseable == javaFiles.size();
        builder.addCheck("Java files can be parsed", allParseable,
                String.format("%d/%d files parseable", parseable, javaFiles.size()));

        return builder.build();
    }

    private void mergeResults(ValidationResult source, ValidationResult.Builder target) {
        source.getChecks().forEach(check ->
                target.addCheck(check.getName(), check.isPassed(), check.getDetails()));
        source.getErrors().forEach(target::addError);
    }
}
