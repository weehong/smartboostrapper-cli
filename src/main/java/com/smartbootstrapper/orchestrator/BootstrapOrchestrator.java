package com.smartbootstrapper.orchestrator;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.exception.SmartBootstrapperException;
import com.smartbootstrapper.git.GitFileHarvester;
import com.smartbootstrapper.git.GitRepositoryService;
import com.smartbootstrapper.initializr.SkeletonExtractor;
import com.smartbootstrapper.initializr.SpringInitializrClient;
import com.smartbootstrapper.manifest.CommitMessageParser;
import com.smartbootstrapper.model.*;
import com.smartbootstrapper.progress.ProgressTracker;
import com.smartbootstrapper.progress.StatisticsCollector;
import com.smartbootstrapper.refactor.RefactorOrchestrator;
import com.smartbootstrapper.writer.ProjectFileWriter;
import com.smartbootstrapper.writer.RollbackManager;
import com.smartbootstrapper.zip.ZipFileHarvester;
import com.smartbootstrapper.zip.ZipRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the complete bootstrap workflow.
 */
public class BootstrapOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapOrchestrator.class);

    private static final int TOTAL_PHASES = 5;

    private final SpringInitializrClient initializrClient;
    private final SkeletonExtractor skeletonExtractor;
    private final RefactorOrchestrator refactorOrchestrator;
    private final ProgressTracker progressTracker;
    private final StatisticsCollector statistics;
    private final RollbackManager rollbackManager;

    private Path tempZipPath;

    public BootstrapOrchestrator(PrintWriter writer) {
        this(writer, true);
    }

    public BootstrapOrchestrator(PrintWriter writer, boolean useColors) {
        this.initializrClient = new SpringInitializrClient();
        this.skeletonExtractor = new SkeletonExtractor();
        this.refactorOrchestrator = new RefactorOrchestrator();
        this.progressTracker = new ProgressTracker(writer, useColors);
        this.statistics = new StatisticsCollector();
        this.rollbackManager = new RollbackManager();

        // Connect rollback manager to progress tracker
        rollbackManager.setProgressCallback(progressTracker::reportRollbackProgress);
    }

    /**
     * Executes the complete bootstrap workflow.
     *
     * @param manifest The manifest containing file entries
     * @param config   The project configuration
     * @return BootstrapResult with outcome and statistics
     */
    public BootstrapResult execute(Manifest manifest, ProjectConfiguration config) {
        logger.info("Starting bootstrap execution");
        statistics.start();
        progressTracker.initialize(TOTAL_PHASES);

        Path targetDirectory = Path.of(config.getTargetDirectory());

        try {
            // Phase 1: Download Spring Boot skeleton
            downloadSkeleton(config, targetDirectory);

            // Phase 2: Validate manifest and repository
            // (Already done by CLI, but we validate again just in case)

            // Phase 3: Harvest files from Git history
            HarvestResult harvestResult = harvestFiles(manifest);
            statistics.setHarvestResult(harvestResult);

            // Phase 4: Refactor and write files
            RefactorResult refactorResult = refactorAndWriteFiles(
                    harvestResult.getHarvestedFiles(),
                    config.getOldPackage(),
                    config.getNewPackage(),
                    targetDirectory,
                    manifest
            );
            statistics.setRefactorResult(refactorResult);

            // Phase 5: Create commit with message from COMMIT.md
            createManifestCommit(manifest, targetDirectory);

            // Success!
            statistics.stop();
            progressTracker.reportCompletion(true, "Bootstrap completed successfully!");

            // Clear rollback manager on success
            rollbackManager.clear();

            return statistics.toBootstrapResult(true, config.getTargetDirectory(), null);

        } catch (Exception e) {
            logger.error("Bootstrap failed", e);
            statistics.stop();

            // Perform rollback
            performRollback();

            return statistics.toBootstrapResult(false, config.getTargetDirectory(), e.getMessage());

        } finally {
            // Cleanup temp files
            cleanup();
        }
    }

    /**
     * Executes the bootstrap workflow for multiple manifests.
     * Downloads skeleton once, then processes each manifest in sequence.
     *
     * @param manifests The list of manifests to process
     * @param config    The project configuration
     * @return BootstrapResult with outcome and statistics
     */
    public BootstrapResult executeMultipleManifests(List<Manifest> manifests, ProjectConfiguration config) {
        logger.info("Starting multi-manifest bootstrap execution with {} manifests", manifests.size());
        statistics.start();

        // Calculate total phases: 1 (skeleton) + 3 per manifest (harvest, refactor, commit)
        int totalPhases = 1 + (manifests.size() * 3);
        progressTracker.initialize(totalPhases);

        Path targetDirectory = Path.of(config.getTargetDirectory());
        int currentPhase = 0;
        int commitsCreated = 0;

        try {
            // Phase 1: Download Spring Boot skeleton (once)
            currentPhase = 1;
            downloadSkeletonForMultiple(config, targetDirectory, currentPhase, totalPhases);

            // Process each manifest
            for (int i = 0; i < manifests.size(); i++) {
                Manifest manifest = manifests.get(i);
                int manifestNum = i + 1;

                // Phase 2 + (i * 3): Harvest files
                currentPhase = 2 + (i * 3);
                HarvestResult harvestResult = harvestFilesForManifest(manifest, currentPhase, totalPhases, manifestNum, manifests.size());
                statistics.addHarvestResult(harvestResult);

                // Phase 3 + (i * 3): Refactor and write files
                currentPhase = 3 + (i * 3);
                RefactorResult refactorResult = refactorAndWriteFilesForManifest(
                        harvestResult.getHarvestedFiles(),
                        config.getOldPackage(),
                        config.getNewPackage(),
                        targetDirectory,
                        manifest,
                        currentPhase,
                        totalPhases,
                        manifestNum,
                        manifests.size()
                );
                statistics.addRefactorResult(refactorResult);

                // Phase 4 + (i * 3): Create commit from COMMIT.md
                currentPhase = 4 + (i * 3);
                boolean committed = createManifestCommitForManifest(manifest, targetDirectory, currentPhase, totalPhases, manifestNum, manifests.size());
                if (committed) {
                    commitsCreated++;
                }
            }

            // Success!
            statistics.stop();
            String message = String.format("Bootstrap completed successfully! Created %d commits.", commitsCreated);
            progressTracker.reportCompletion(true, message);

            // Clear rollback manager on success
            rollbackManager.clear();

            return statistics.toBootstrapResult(true, config.getTargetDirectory(), null);

        } catch (Exception e) {
            logger.error("Multi-manifest bootstrap failed", e);
            statistics.stop();

            // Perform rollback
            performRollback();

            return statistics.toBootstrapResult(false, config.getTargetDirectory(), e.getMessage());

        } finally {
            // Cleanup temp files
            cleanup();
        }
    }

    private void downloadSkeletonForMultiple(ProjectConfiguration config, Path targetDirectory, int phase, int totalPhases) {
        progressTracker.startPhase(phase, totalPhases, Constants.PHASE_DOWNLOAD_SKELETON);

        try {
            // Check if target directory exists and is empty
            if (Files.exists(targetDirectory)) {
                if (!Files.isDirectory(targetDirectory)) {
                    throw new SmartBootstrapperException("Target path exists but is not a directory");
                }
                try (var entries = Files.list(targetDirectory)) {
                    if (entries.findFirst().isPresent()) {
                        progressTracker.reportProgress("Warning: Target directory is not empty");
                    }
                }
            }

            // Download skeleton from Spring Initializr
            progressTracker.reportSubTask("Connecting to Spring Initializr...");
            tempZipPath = initializrClient.downloadSkeleton(config);
            progressTracker.reportSubTask("Downloaded skeleton project");

            // Extract skeleton to target directory
            progressTracker.reportSubTask("Extracting skeleton to target directory...");
            var extractedFiles = skeletonExtractor.extractSkeleton(tempZipPath, targetDirectory);
            progressTracker.reportSubTask("Extracted " + extractedFiles.size() + " files");
            statistics.recordFilesDownloaded(extractedFiles.size());

            // Initialize git repository and create initial commit
            progressTracker.reportSubTask("Initializing git repository...");
            try (GitRepositoryService gitService = GitRepositoryService.init(targetDirectory)) {
                gitService.addAll();
                gitService.commit(Constants.INITIAL_COMMIT_MESSAGE);
                progressTracker.reportSubTask("Initialized git repository with initial commit");
            }

            // Track created directories for rollback
            rollbackManager.recordDirectoryCreated(targetDirectory);

            progressTracker.completePhase();

        } catch (Exception e) {
            progressTracker.reportError("Failed to download skeleton: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to download Spring Boot skeleton", e);
        }
    }

    private HarvestResult harvestFilesForManifest(Manifest manifest, int phase, int totalPhases, int manifestNum, int totalManifests) {
        String phaseLabel = String.format("%s (%d/%d)", Constants.PHASE_HARVEST, manifestNum, totalManifests);
        progressTracker.startPhase(phase, totalPhases, phaseLabel);

        if (manifest.isZipSource()) {
            return harvestFilesFromZipForManifest(manifest);
        } else {
            return harvestFilesFromGitForManifest(manifest);
        }
    }

    private HarvestResult harvestFilesFromGitForManifest(Manifest manifest) {
        try (GitRepositoryService repoService = GitRepositoryService.open(manifest.getSourceRepositoryPath())) {
            GitFileHarvester harvester = new GitFileHarvester(repoService);

            harvester.setProgressCallback((path, success) -> {
                if (success) {
                    progressTracker.reportSubTask("Harvested: " + getFileName(path));
                    statistics.recordFileHarvested();
                } else {
                    progressTracker.reportError("Failed to harvest: " + getFileName(path));
                    statistics.recordError();
                }
            });

            HarvestResult result = harvester.harvestFiles(manifest);

            if (result.hasErrors()) {
                throw new SmartBootstrapperException("Failed to harvest " + result.getFailedFiles() + " file(s)");
            }

            progressTracker.completePhase();
            return result;

        } catch (SmartBootstrapperException e) {
            throw e;
        } catch (Exception e) {
            progressTracker.reportError("Harvest failed: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to harvest files from Git", e);
        }
    }

    private HarvestResult harvestFilesFromZipForManifest(Manifest manifest) {
        try (ZipRepositoryService repoService = ZipRepositoryService.open(manifest.getSourceRepositoryPath())) {
            ZipFileHarvester harvester = new ZipFileHarvester(repoService);

            harvester.setProgressCallback((path, success) -> {
                if (success) {
                    progressTracker.reportSubTask("Harvested: " + getFileName(path));
                    statistics.recordFileHarvested();
                } else {
                    progressTracker.reportError("Failed to harvest: " + getFileName(path));
                    statistics.recordError();
                }
            });

            HarvestResult result = harvester.harvestFiles(manifest);

            if (result.hasErrors()) {
                throw new SmartBootstrapperException("Failed to harvest " + result.getFailedFiles() + " file(s)");
            }

            progressTracker.completePhase();
            return result;

        } catch (SmartBootstrapperException e) {
            throw e;
        } catch (Exception e) {
            progressTracker.reportError("Harvest failed: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to harvest files from ZIP archives", e);
        }
    }

    private RefactorResult refactorAndWriteFilesForManifest(Map<String, byte[]> harvestedFiles,
                                                             String oldPackage,
                                                             String newPackage,
                                                             Path defaultTargetDirectory,
                                                             Manifest manifest,
                                                             int phase,
                                                             int totalPhases,
                                                             int manifestNum,
                                                             int totalManifests) {
        String phaseLabel = String.format("%s (%d/%d)", Constants.PHASE_REFACTOR, manifestNum, totalManifests);
        progressTracker.startPhase(phase, totalPhases, phaseLabel);

        try {
            refactorOrchestrator.setProgressCallback((path, success) -> {
                if (success) {
                    progressTracker.reportSubTask("Refactored: " + getFileName(path));
                    statistics.recordFileRefactored();
                } else {
                    progressTracker.reportError("Refactor failed: " + getFileName(path));
                    statistics.recordError();
                }
            });

            String pathOldPackage = manifest.detectDestinationBasePackage().orElse(oldPackage);

            RefactorResult refactorResult = refactorOrchestrator.refactorFilesWithPathTransformation(
                    harvestedFiles, oldPackage, newPackage, pathOldPackage
            );

            Map<String, byte[]> refactoredFiles = refactorResult.getRefactoredFilesWithContent();

            Map<String, Path> originalTargetDirectoryMap = buildTargetDirectoryMap(manifest, defaultTargetDirectory);

            Map<String, Path> targetDirectoryMap = new HashMap<>();
            for (Map.Entry<String, Path> entry : originalTargetDirectoryMap.entrySet()) {
                String originalPath = entry.getKey();
                String transformedPath = refactorOrchestrator.transformDestinationPath(originalPath, pathOldPackage, newPackage);
                targetDirectoryMap.put(transformedPath, entry.getValue());
            }

            Map<Path, Map<String, byte[]>> filesByTargetDir = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : refactoredFiles.entrySet()) {
                String destPath = entry.getKey();
                byte[] content = entry.getValue();
                Path targetDir = targetDirectoryMap.getOrDefault(destPath, defaultTargetDirectory);

                filesByTargetDir.computeIfAbsent(targetDir, k -> new HashMap<>())
                        .put(destPath, content);
            }

            for (Map.Entry<Path, Map<String, byte[]>> dirEntry : filesByTargetDir.entrySet()) {
                Path targetDir = dirEntry.getKey();
                Map<String, byte[]> files = dirEntry.getValue();

                ProjectFileWriter writer = new ProjectFileWriter(targetDir, rollbackManager);
                writer.setProgressCallback((path, success) -> {
                    if (success) {
                        progressTracker.reportSubTask("Wrote: " + getFileName(path) + " -> " + targetDir);
                        statistics.recordFileWritten();
                    } else {
                        progressTracker.reportError("Write failed: " + getFileName(path));
                        statistics.recordError();
                    }
                });

                writer.writeFiles(files);
            }

            progressTracker.completePhase();
            return refactorResult;

        } catch (Exception e) {
            progressTracker.reportError("Refactor/write failed: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to refactor and write files", e);
        }
    }

    private boolean createManifestCommitForManifest(Manifest manifest, Path targetDirectory, int phase, int totalPhases, int manifestNum, int totalManifests) {
        String phaseLabel = String.format("%s (%d/%d)", Constants.PHASE_COMMIT, manifestNum, totalManifests);
        progressTracker.startPhase(phase, totalPhases, phaseLabel);

        try {
            if (!manifest.hasSequenceNumber()) {
                progressTracker.reportSubTask("No sequence number in manifest, skipping commit");
                progressTracker.completePhase();
                return false;
            }

            int sequenceNumber = manifest.getSequenceNumber().orElseThrow();

            Path sourceRepoPath = Path.of(manifest.getSourceRepositoryPath());
            Path commitMdPath = sourceRepoPath.resolve("COMMIT.md");

            if (!Files.exists(commitMdPath)) {
                progressTracker.reportSubTask("COMMIT.md not found, skipping commit");
                logger.warn("COMMIT.md not found at: {}", commitMdPath);
                progressTracker.completePhase();
                return false;
            }

            progressTracker.reportSubTask("Reading commit message from COMMIT.md...");
            CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

            if (!parser.hasMessage(sequenceNumber)) {
                progressTracker.reportSubTask("No commit message for sequence " + sequenceNumber + ", skipping commit");
                logger.warn("No commit message found for sequence {} in COMMIT.md", sequenceNumber);
                progressTracker.completePhase();
                return false;
            }

            String commitMessage = parser.getMessageOrThrow(sequenceNumber);
            progressTracker.reportSubTask("Found commit message for sequence " + sequenceNumber);

            try (GitRepositoryService gitService = GitRepositoryService.open(targetDirectory)) {
                gitService.addAll();
                String commitHash = gitService.commit(commitMessage);
                progressTracker.reportSubTask("Created commit: " + commitHash.substring(0, 7));
                logger.info("Created commit {} with message from COMMIT.md sequence {}", commitHash, sequenceNumber);
            }

            progressTracker.completePhase();
            return true;

        } catch (Exception e) {
            progressTracker.reportError("Failed to create commit: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to create commit from COMMIT.md", e);
        }
    }

    private void downloadSkeleton(ProjectConfiguration config, Path targetDirectory) {
        progressTracker.startPhase(1, Constants.PHASE_DOWNLOAD_SKELETON);

        try {
            // Check if target directory exists and is empty
            if (Files.exists(targetDirectory)) {
                if (!Files.isDirectory(targetDirectory)) {
                    throw new SmartBootstrapperException("Target path exists but is not a directory");
                }
                // Allow non-empty directory but warn
                try (var entries = Files.list(targetDirectory)) {
                    if (entries.findFirst().isPresent()) {
                        progressTracker.reportProgress("Warning: Target directory is not empty");
                    }
                }
            }

            // Download skeleton from Spring Initializr
            progressTracker.reportSubTask("Connecting to Spring Initializr...");
            tempZipPath = initializrClient.downloadSkeleton(config);
            progressTracker.reportSubTask("Downloaded skeleton project");

            // Extract skeleton to target directory
            progressTracker.reportSubTask("Extracting skeleton to target directory...");
            var extractedFiles = skeletonExtractor.extractSkeleton(tempZipPath, targetDirectory);
            progressTracker.reportSubTask("Extracted " + extractedFiles.size() + " files");
            statistics.recordFilesDownloaded(extractedFiles.size());

            // Initialize git repository and create initial commit
            progressTracker.reportSubTask("Initializing git repository...");
            try (GitRepositoryService gitService = GitRepositoryService.init(targetDirectory)) {
                gitService.addAll();
                gitService.commit(Constants.INITIAL_COMMIT_MESSAGE);
                progressTracker.reportSubTask("Initialized git repository with initial commit");
            }

            // Track created directories for rollback
            rollbackManager.recordDirectoryCreated(targetDirectory);

            progressTracker.completePhase();

        } catch (Exception e) {
            progressTracker.reportError("Failed to download skeleton: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to download Spring Boot skeleton", e);
        }
    }

    private HarvestResult harvestFiles(Manifest manifest) {
        progressTracker.startPhase(3, Constants.PHASE_HARVEST);

        if (manifest.isZipSource()) {
            return harvestFilesFromZip(manifest);
        } else {
            return harvestFilesFromGit(manifest);
        }
    }

    private HarvestResult harvestFilesFromGit(Manifest manifest) {
        try (GitRepositoryService repoService = GitRepositoryService.open(manifest.getSourceRepositoryPath())) {
            GitFileHarvester harvester = new GitFileHarvester(repoService);

            // Set up progress callback
            harvester.setProgressCallback((path, success) -> {
                if (success) {
                    progressTracker.reportSubTask("Harvested: " + getFileName(path));
                    statistics.recordFileHarvested();
                } else {
                    progressTracker.reportError("Failed to harvest: " + getFileName(path));
                    statistics.recordError();
                }
            });

            HarvestResult result = harvester.harvestFiles(manifest);

            if (result.hasErrors()) {
                throw new SmartBootstrapperException(
                        "Failed to harvest " + result.getFailedFiles() + " file(s)"
                );
            }

            progressTracker.completePhase();
            return result;

        } catch (SmartBootstrapperException e) {
            throw e;
        } catch (Exception e) {
            progressTracker.reportError("Harvest failed: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to harvest files from Git", e);
        }
    }

    private HarvestResult harvestFilesFromZip(Manifest manifest) {
        try (ZipRepositoryService repoService = ZipRepositoryService.open(manifest.getSourceRepositoryPath())) {
            ZipFileHarvester harvester = new ZipFileHarvester(repoService);

            // Set up progress callback
            harvester.setProgressCallback((path, success) -> {
                if (success) {
                    progressTracker.reportSubTask("Harvested: " + getFileName(path));
                    statistics.recordFileHarvested();
                } else {
                    progressTracker.reportError("Failed to harvest: " + getFileName(path));
                    statistics.recordError();
                }
            });

            HarvestResult result = harvester.harvestFiles(manifest);

            if (result.hasErrors()) {
                throw new SmartBootstrapperException(
                        "Failed to harvest " + result.getFailedFiles() + " file(s)"
                );
            }

            progressTracker.completePhase();
            return result;

        } catch (SmartBootstrapperException e) {
            throw e;
        } catch (Exception e) {
            progressTracker.reportError("Harvest failed: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to harvest files from ZIP archives", e);
        }
    }

    private RefactorResult refactorAndWriteFiles(Map<String, byte[]> harvestedFiles,
                                                   String oldPackage,
                                                   String newPackage,
                                                   Path defaultTargetDirectory,
                                                   Manifest manifest) {
        progressTracker.startPhase(4, Constants.PHASE_REFACTOR);

        try {
            // Refactor files with path transformation
            refactorOrchestrator.setProgressCallback((path, success) -> {
                if (success) {
                    progressTracker.reportSubTask("Refactored: " + getFileName(path));
                    statistics.recordFileRefactored();
                } else {
                    progressTracker.reportError("Refactor failed: " + getFileName(path));
                    statistics.recordError();
                }
            });

            // Detect destination base package for path transformation
            // This may differ from oldPackage (which is from source paths) when
            // manifest destination paths use a different package structure
            String pathOldPackage = manifest.detectDestinationBasePackage()
                    .orElse(oldPackage);

            // Use the method that handles both content refactoring and path transformation
            // - oldPackage: for content refactoring (matches actual package declarations in source files)
            // - pathOldPackage: for path transformation (matches destination path structure in manifest)
            RefactorResult refactorResult = refactorOrchestrator.refactorFilesWithPathTransformation(
                    harvestedFiles, oldPackage, newPackage, pathOldPackage
            );

            // Get the refactored files with transformed paths
            Map<String, byte[]> refactoredFiles = refactorResult.getRefactoredFilesWithContent();

            // Build a map of original destination path to target directory from manifest entries
            // We need to transform these paths too for custom target directories
            Map<String, Path> originalTargetDirectoryMap = buildTargetDirectoryMap(manifest, defaultTargetDirectory);

            // Create a new map with transformed paths (using pathOldPackage for consistency)
            Map<String, Path> targetDirectoryMap = new HashMap<>();
            for (Map.Entry<String, Path> entry : originalTargetDirectoryMap.entrySet()) {
                String originalPath = entry.getKey();
                String transformedPath = refactorOrchestrator.transformDestinationPath(originalPath, pathOldPackage, newPackage);
                targetDirectoryMap.put(transformedPath, entry.getValue());
            }

            // Group files by target directory
            Map<Path, Map<String, byte[]>> filesByTargetDir = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : refactoredFiles.entrySet()) {
                String destPath = entry.getKey();
                byte[] content = entry.getValue();
                Path targetDir = targetDirectoryMap.getOrDefault(destPath, defaultTargetDirectory);

                filesByTargetDir.computeIfAbsent(targetDir, k -> new HashMap<>())
                        .put(destPath, content);
            }

            // Write files to their respective target directories
            for (Map.Entry<Path, Map<String, byte[]>> dirEntry : filesByTargetDir.entrySet()) {
                Path targetDir = dirEntry.getKey();
                Map<String, byte[]> files = dirEntry.getValue();

                ProjectFileWriter writer = new ProjectFileWriter(targetDir, rollbackManager);
                writer.setProgressCallback((path, success) -> {
                    if (success) {
                        progressTracker.reportSubTask("Wrote: " + getFileName(path) + " -> " + targetDir);
                        statistics.recordFileWritten();
                    } else {
                        progressTracker.reportError("Write failed: " + getFileName(path));
                        statistics.recordError();
                    }
                });

                writer.writeFiles(files);
            }

            progressTracker.completePhase();
            return refactorResult;

        } catch (Exception e) {
            progressTracker.reportError("Refactor/write failed: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to refactor and write files", e);
        }
    }

    private Map<String, Path> buildTargetDirectoryMap(Manifest manifest, Path defaultTargetDirectory) {
        Map<String, Path> map = new HashMap<>();
        for (ManifestEntry entry : manifest.getEntries()) {
            if (entry.hasTargetDirectory()) {
                map.put(entry.getDestinationPath(), Path.of(entry.getTargetDirectory()));
            }
        }
        return map;
    }

    /**
     * Creates a commit with the message from COMMIT.md if the manifest has a sequence number.
     *
     * @param manifest        The manifest being processed
     * @param targetDirectory The target directory containing the git repository
     */
    private void createManifestCommit(Manifest manifest, Path targetDirectory) {
        progressTracker.startPhase(5, Constants.PHASE_COMMIT);

        try {
            if (!manifest.hasSequenceNumber()) {
                progressTracker.reportSubTask("No sequence number in manifest, skipping commit");
                progressTracker.completePhase();
                return;
            }

            int sequenceNumber = manifest.getSequenceNumber().orElseThrow();

            // Locate COMMIT.md in the source repository directory
            Path sourceRepoPath = Path.of(manifest.getSourceRepositoryPath());
            Path commitMdPath = sourceRepoPath.resolve("COMMIT.md");

            if (!Files.exists(commitMdPath)) {
                progressTracker.reportSubTask("COMMIT.md not found, skipping commit");
                logger.warn("COMMIT.md not found at: {}", commitMdPath);
                progressTracker.completePhase();
                return;
            }

            // Parse COMMIT.md and get the message for this sequence
            progressTracker.reportSubTask("Reading commit message from COMMIT.md...");
            CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

            if (!parser.hasMessage(sequenceNumber)) {
                progressTracker.reportSubTask("No commit message for sequence " + sequenceNumber + ", skipping commit");
                logger.warn("No commit message found for sequence {} in COMMIT.md", sequenceNumber);
                progressTracker.completePhase();
                return;
            }

            String commitMessage = parser.getMessageOrThrow(sequenceNumber);
            progressTracker.reportSubTask("Found commit message for sequence " + sequenceNumber);

            // Stage all changes and create the commit
            try (GitRepositoryService gitService = GitRepositoryService.open(targetDirectory)) {
                gitService.addAll();
                String commitHash = gitService.commit(commitMessage);
                progressTracker.reportSubTask("Created commit: " + commitHash.substring(0, 7));
                logger.info("Created commit {} with message from COMMIT.md sequence {}", commitHash, sequenceNumber);
            }

            progressTracker.completePhase();

        } catch (Exception e) {
            progressTracker.reportError("Failed to create commit: " + e.getMessage());
            throw new SmartBootstrapperException("Failed to create commit from COMMIT.md", e);
        }
    }

    private void performRollback() {
        if (rollbackManager.hasChanges()) {
            progressTracker.reportRollbackStart();
            boolean success = rollbackManager.rollback();
            progressTracker.reportRollbackComplete(success);
        }
    }

    private void cleanup() {
        // Clean up temporary ZIP file
        if (tempZipPath != null) {
            skeletonExtractor.cleanup(tempZipPath);
            tempZipPath = null;
        }
    }

    private String getFileName(String path) {
        if (path == null) return "unknown";
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * Gets the statistics collector.
     */
    public StatisticsCollector getStatistics() {
        return statistics;
    }

    /**
     * Gets the progress tracker.
     */
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }
}
