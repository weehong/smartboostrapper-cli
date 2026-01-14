package com.smartbootstrapper.git;

import com.smartbootstrapper.exception.GitException;
import com.smartbootstrapper.model.HarvestResult;
import com.smartbootstrapper.model.Manifest;
import com.smartbootstrapper.model.ManifestEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 * Harvests files from Git history based on manifest entries.
 */
public class GitFileHarvester {

    private static final Logger logger = LoggerFactory.getLogger(GitFileHarvester.class);

    private final GitRepositoryService repositoryService;
    private BiConsumer<String, Boolean> progressCallback;

    public GitFileHarvester(GitRepositoryService repositoryService) {
        this.repositoryService = repositoryService;
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
     * Harvests all files specified in the manifest from Git history.
     *
     * @param manifest The manifest containing file entries to harvest
     * @return HarvestResult containing harvested files and any errors
     */
    public HarvestResult harvestFiles(Manifest manifest) {
        logger.info("Starting file harvest from {} with {} entries",
                manifest.getSourceRepositoryPath(), manifest.size());

        HarvestResult.Builder resultBuilder = HarvestResult.builder()
                .totalFiles(manifest.size());

        for (ManifestEntry entry : manifest.getEntries()) {
            harvestEntry(entry, resultBuilder);
        }

        HarvestResult result = resultBuilder.build();
        logger.info("Harvest completed: {} successful, {} failed",
                result.getSuccessfulFiles(), result.getFailedFiles());

        return result;
    }

    private void harvestEntry(ManifestEntry entry, HarvestResult.Builder resultBuilder) {
        String commitHash = entry.getCommitHash();
        String sourcePath = entry.getSourcePath();
        String destinationPath = entry.getDestinationPath();

        logger.debug("Harvesting {} from commit {} -> {}", sourcePath, commitHash, destinationPath);

        try {
            byte[] content = repositoryService.getFileBytes(commitHash, sourcePath);
            resultBuilder.addHarvestedFile(destinationPath, content);

            if (progressCallback != null) {
                progressCallback.accept(destinationPath, true);
            }

            logger.debug("Successfully harvested: {}", destinationPath);

        } catch (GitException e) {
            String errorMessage = String.format("Failed to harvest %s from commit %s: %s",
                    sourcePath, commitHash.substring(0, Math.min(7, commitHash.length())), e.getMessage());
            resultBuilder.addError(destinationPath, errorMessage);

            if (progressCallback != null) {
                progressCallback.accept(destinationPath, false);
            }

            logger.warn("Failed to harvest file: {}", errorMessage);
        }
    }

    /**
     * Validates that all files in the manifest can be harvested.
     * This is a dry-run check without actually storing the content.
     *
     * @param manifest The manifest to validate
     * @return HarvestResult with validation results (files will be empty, only errors populated)
     */
    public HarvestResult validateHarvest(Manifest manifest) {
        logger.info("Validating harvest for {} entries", manifest.size());

        HarvestResult.Builder resultBuilder = HarvestResult.builder()
                .totalFiles(manifest.size());

        for (ManifestEntry entry : manifest.getEntries()) {
            validateEntry(entry, resultBuilder);
        }

        return resultBuilder.build();
    }

    private void validateEntry(ManifestEntry entry, HarvestResult.Builder resultBuilder) {
        String commitHash = entry.getCommitHash();
        String sourcePath = entry.getSourcePath();
        String destinationPath = entry.getDestinationPath();

        // Check if commit exists
        if (!repositoryService.commitExists(commitHash)) {
            resultBuilder.addError(destinationPath,
                    String.format("Commit not found: %s", commitHash));
            return;
        }

        // Check if file exists at commit
        if (!repositoryService.fileExistsAtCommit(commitHash, sourcePath)) {
            resultBuilder.addError(destinationPath,
                    String.format("File not found at commit %s: %s",
                            commitHash.substring(0, Math.min(7, commitHash.length())), sourcePath));
            return;
        }

        // File is valid - add a placeholder to indicate success
        resultBuilder.addHarvestedFile(destinationPath, new byte[0]);
    }
}
